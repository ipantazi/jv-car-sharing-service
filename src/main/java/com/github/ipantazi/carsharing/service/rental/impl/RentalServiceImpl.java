package com.github.ipantazi.carsharing.service.rental.impl;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.mapper.CarMapper;
import com.github.ipantazi.carsharing.mapper.RentalMapper;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.notification.NotificationMapper;
import com.github.ipantazi.carsharing.notification.NotificationService;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalSpecificationBuilder;
import com.github.ipantazi.carsharing.service.car.CarService;
import com.github.ipantazi.carsharing.service.car.InventoryService;
import com.github.ipantazi.carsharing.service.payment.PaymentValidator;
import com.github.ipantazi.carsharing.service.rental.Calculator;
import com.github.ipantazi.carsharing.service.rental.RentalService;
import com.github.ipantazi.carsharing.service.rental.RentalValidator;
import com.github.ipantazi.carsharing.service.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final RentalSpecificationBuilder specificationBuilder;
    private final RentalMapper rentalMapper;
    private final CarMapper carMapper;
    private final CarService carService;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final Calculator calculator;
    private final PaymentValidator paymentValidator;
    private final RentalValidator rentalValidator;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public RentalResponseDto createRental(Long userId, RentalRequestDto rentalRequestDto) {
        Long carId = rentalRequestDto.carId();
        LocalDate rentalDate = LocalDate.now(clock);

        rentalValidator.checkDatesBeforeRenting(rentalRequestDto, rentalDate);
        paymentValidator.checkForPendingPayments(userId);

        rentalRepository.lockActiveRentalsForUpdateByCarId(carId);
        inventoryService.adjustInventory(carId, 1, OperationType.DECREASE);

        User user = userService.getUserById(userId);
        Car car = carService.findCarById(carId);
        Rental rental = rentalMapper.toRentalEntity(
                user,
                car,
                rentalRequestDto.returnDate(),
                rentalDate
        );
        rentalRepository.save(rental);

        NewRentalPayload newRentalPayload = notificationMapper.toRentalPayload(rental);
        notificationService.sendMessage(NotificationType.NEW_RENTAL_CREATED, newRentalPayload);

        return buildRentalResponseDto(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalResponseDto> getRentalsByFilter(Long userId,
                                                      Boolean isActive,
                                                      Pageable pageable) {
        Specification<Rental> specification = specificationBuilder.build(userId, isActive);
        Page<Rental> rentalPage = rentalRepository.findAll(specification, pageable);

        if (rentalPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
        return mapToRentalResponsePage(rentalPage, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalDetailedDto getRental(Long userId, Long rentalId) {
        Rental rental = rentalRepository.findRentalById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found with id: "
                        + rentalId));

        if (!userService.canAccessRental(userId, rental.getUser().getId())) {
            throw new AccessDeniedException("You do not have permission to access this rental");
        }
        return buildRentalDetailedDto(rental);
    }

    @Override
    @Transactional
    public RentalDetailedDto returnRental(Long userId, Long rentalId) {
        LocalDate actualReturnDate = LocalDate.now(clock);

        Rental rental = rentalRepository.lockRentalForUpdate(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found with id: "
                        + rentalId));

        if (!userService.canAccessRental(userId, rental.getUser().getId())) {
            throw new AccessDeniedException("You do not have permission to access this rental");
        }

        if (rental.getActualReturnDate() != null) {
            throw new IllegalArgumentException("Rental with id %d is already returned on %s"
                    .formatted(rental.getId(), rental.getActualReturnDate()));
        }
        rental.setActualReturnDate(actualReturnDate);

        rentalRepository.save(rental);
        inventoryService.adjustInventory(rental.getCar().getId(), 1, OperationType.INCREASE);
        return buildRentalDetailedDto(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public Rental getRentalEntityByIdAndUserId(Long userId, Long rentalId) {
        return rentalRepository.findRentalByIdAndUserId(rentalId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Rental not found with id: %d and user id: %d."
                        .formatted(rentalId, userId)
                ));
    }

    private RentalStatus getRentalStatus(Rental rental) {
        return rental.getActualReturnDate() == null ? RentalStatus.ACTIVE : RentalStatus.RETURNED;
    }

    private RentalResponseDto buildRentalResponseDto(Rental rental) {
        CarDto carDto = carMapper.toCarDto(rental.getCar());
        BigDecimal baseRentalCost = calculator.calculateBaseRentalCost(carDto.getDailyFee(),
                rental);

        return rentalMapper.toRentalDto(
                rental,
                carDto,
                getRentalStatus(rental),
                baseRentalCost
        );
    }

    private RentalDetailedDto buildRentalDetailedDto(Rental rental) {
        CarDto carDto = carMapper.toCarDto(rental.getCar());

        BigDecimal baseCost = calculator.calculateBaseRentalCost(carDto.getDailyFee(), rental);

        boolean isOverdue = rental.getActualReturnDate() != null
                && rental.getActualReturnDate().isAfter(rental.getReturnDate());
        BigDecimal penaltyAmount = isOverdue
                ? calculator.calculatePenaltyAmount(carDto.getDailyFee(), rental)
                : BigDecimal.ZERO;

        BigDecimal totalCost = baseCost.add(penaltyAmount);
        BigDecimal amountPaid = calculator.calculateTotalAmountPaid(rental.getId());
        BigDecimal amountDue = totalCost.subtract(amountPaid);

        return rentalMapper.toRentalDetailedDto(
                rental,
                carDto,
                baseCost,
                penaltyAmount,
                totalCost,
                amountPaid,
                amountDue,
                getRentalStatus(rental)
                );
    }

    private Page<RentalResponseDto> mapToRentalResponsePage(Page<Rental> rentalPage,
                                                            Pageable pageable) {
        List<RentalResponseDto> rentalResponseDtos = rentalPage.stream()
                .map(this::buildRentalResponseDto)
                .toList();

        return new PageImpl<>(rentalResponseDtos, pageable, rentalPage.getTotalElements());
    }
}
