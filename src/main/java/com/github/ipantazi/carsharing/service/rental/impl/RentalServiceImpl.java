package com.github.ipantazi.carsharing.service.rental.impl;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.mapper.RentalMapper;
import com.github.ipantazi.carsharing.model.Rental;
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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final CarService carService;
    private final InventoryService inventoryService;
    private final RentalSpecificationBuilder specificationBuilder;
    private final Clock clock;
    private final Calculator calculator;
    private final PaymentValidator paymentValidator;
    private final RentalValidator rentalValidator;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final UserService userService;

    @Override
    @Transactional
    public RentalResponseDto createRental(Long userId, RentalRequestDto rentalRequestDto) {
        Long carId = rentalRequestDto.carId();
        LocalDate rentalDate = LocalDate.now(clock);

        rentalValidator.checkDatesBeforeRenting(rentalRequestDto, rentalDate);
        carService.validateCarAvailableForRental(carId);
        paymentValidator.checkForPendingPayments(userId);

        Rental rental = rentalMapper.toRentalEntity(rentalRequestDto);
        rental.setRentalDate(rentalDate);
        rental.setUserId(userId);
        rentalRepository.save(rental);

        inventoryService.adjustInventory(carId, 1, OperationType.DECREASE);

        CarDto carDto = carService.getById(carId);
        NewRentalPayload newRentalPayload = notificationMapper.toRentalPayload(
                rental,
                userService.getUserDetails(userId),
                carDto);
        notificationService.sendMessage(NotificationType.NEW_RENTAL_CREATED, newRentalPayload);

        return buildRentalResponseDto(rental, carDto);
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
        Rental rental = rentalValidator.getRentalWithAccessCheck(userId, rentalId);
        return buildRentalDetailedDto(rental);
    }

    @Override
    @Transactional
    public RentalDetailedDto returnRental(Long userId, Long rentalId) {
        LocalDate actualReturnDate = LocalDate.now(clock);
        Rental rental = rentalValidator.getRentalWithAccessCheck(userId, rentalId);

        if (rental.getActualReturnDate() != null) {
            throw new IllegalArgumentException("Rental with id %d is already returned on %s"
                    .formatted(rental.getId(), rental.getActualReturnDate()));
        }

        rental.setActualReturnDate(actualReturnDate);
        rentalRepository.save(rental);

        inventoryService.adjustInventory(rental.getCarId(), 1, OperationType.INCREASE);

        return buildRentalDetailedDto(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public Rental getRentalByIdAndUserId(Long userId, Long rentalId) {
        return rentalRepository.findRentalByIdAndUserId(rentalId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Rental not found with id: %d and user id: %d."
                        .formatted(rentalId, userId)
                ));
    }

    private RentalStatus getRentalStatus(Rental rental) {
        return rental.getActualReturnDate() == null ? RentalStatus.ACTIVE : RentalStatus.RETURNED;
    }

    private RentalResponseDto buildRentalResponseDto(Rental rental, CarDto carDto) {
        RentalResponseDto dto = rentalMapper.toRentalDto(rental);
        dto.setCarDto(carDto);
        dto.setStatus(getRentalStatus(rental));
        dto.setBaseRentalCost(calculator.calculateBaseRentalCost(carDto.getDailyFee(), rental));
        return dto;
    }

    private RentalDetailedDto buildRentalDetailedDto(Rental rental) {
        RentalDetailedDto rentalDetailedDto = rentalMapper.toRentalDetailedDto(rental);

        CarDto carDto = carService.getById(rental.getCarId());
        BigDecimal baseCost = calculator.calculateBaseRentalCost(carDto.getDailyFee(), rental);

        boolean isOverdue = rental.getActualReturnDate() != null
                && rental.getActualReturnDate().isAfter(rental.getReturnDate());
        BigDecimal penaltyAmount = isOverdue
                ? calculator.calculatePenaltyAmount(carDto.getDailyFee(), rental)
                : BigDecimal.ZERO;

        BigDecimal totalCost = baseCost.add(penaltyAmount);
        BigDecimal amountPaid = calculator.calculateTotalAmountPaid(rental.getId());
        BigDecimal amountDue = totalCost.subtract(amountPaid);

        rentalDetailedDto.setCarDto(carDto);
        rentalDetailedDto.setBaseRentalCost(baseCost);
        rentalDetailedDto.setPenaltyAmount(penaltyAmount);
        rentalDetailedDto.setTotalCost(totalCost);
        rentalDetailedDto.setAmountPaid(amountPaid);
        rentalDetailedDto.setAmountDue(amountDue);
        rentalDetailedDto.setStatus(getRentalStatus(rental));

        return rentalDetailedDto;
    }

    private Page<RentalResponseDto> mapToRentalResponsePage(Page<Rental> rentalPage,
                                                            Pageable pageable) {
        Set<Long> carIds = rentalPage.stream()
                .map(Rental::getCarId)
                .collect(Collectors.toSet());

        Map<Long, CarDto> carMap = carService.getByIds(carIds).stream()
                .collect(Collectors.toMap(CarDto::getId, Function.identity()));

        List<RentalResponseDto> rentalResponseDtos = rentalPage.stream()
                .map(rental -> {
                    CarDto carDto = carMap.get(rental.getCarId());
                    return buildRentalResponseDto(rental, carDto);
                })
                .toList();

        return new PageImpl<>(rentalResponseDtos, pageable, rentalPage.getTotalElements());
    }
}
