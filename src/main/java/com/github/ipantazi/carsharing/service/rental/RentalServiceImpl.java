package com.github.ipantazi.carsharing.service.rental;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.mapper.RentalMapper;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalSpecificationBuilder;
import com.github.ipantazi.carsharing.service.car.CarService;
import com.github.ipantazi.carsharing.service.car.InventoryService;
import com.github.ipantazi.carsharing.service.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private static final int MIN_RENTAL_DAYS = 2;
    private static final int MAX_RENTAL_DAYS = 30;
    private static final BigDecimal LATE_FEE_MULTIPLIER = BigDecimal.valueOf(1.5);
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final UserService userService;
    private final CarService carService;
    private final InventoryService inventoryService;
    private final RentalSpecificationBuilder specificationBuilder;
    private final Clock clock;

    @Override
    @Transactional
    public RentalResponseDto createRental(Long userId, RentalRequestDto rentalRequestDto) {
        LocalDate rentalDate = LocalDate.now(clock);
        // check is user has unreturned rental???

        /* Do not allow users to borrow new car if at least one pending payment for the user
        Before creating borrowing - simply check the number of pending payments
        If at least one exists - forbid borrowing */

        checkBeforeRent(rentalRequestDto, rentalDate);

        Rental rental = rentalMapper.toRentalEntity(rentalRequestDto);
        rental.setRentalDate(rentalDate);
        rental.setUserId(userId);
        rentalRepository.save(rental);

        inventoryService.adjustInventory(rentalRequestDto.carId(), 1, OperationType.DECREASE);
        CarDto carDto = carService.getById(rental.getCarId());
        return buildRentalResponseDto(rental, carDto);
    }

    @Override
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
    public RentalDetailedDto getRentalById(Long userId, Long rentalId) {
        Rental rental = getRentalWithAccessCheck(userId, rentalId);
        return buildRentalDetailedDto(rental);
    }

    @Override
    @Transactional
    public RentalDetailedDto returnRental(Long userId, Long rentalId) {
        LocalDate actualReturnDate = LocalDate.now(clock);
        Rental rental = getRentalWithAccessCheck(userId, rentalId);

        if (rental.getActualReturnDate() != null) {
            throw new IllegalArgumentException("Rental with id " + rentalId
                    + " is already returned on " + rental.getActualReturnDate());
        }

        rental.setActualReturnDate(actualReturnDate);
        rentalRepository.save(rental);

        inventoryService.adjustInventory(rental.getCarId(), 1, OperationType.INCREASE);

        return buildRentalDetailedDto(rental);
    }

    private void checkBeforeRent(RentalRequestDto requestDto, LocalDate rentalDate) {
        if (requestDto.returnDate().isBefore(rentalDate)) {
            throw new IllegalArgumentException("Return date must be in the future");
        }
        if (requestDto.returnDate().isBefore(rentalDate.plusDays(MIN_RENTAL_DAYS))) {
            throw new IllegalArgumentException("Return date must be no earlier than "
                    + MIN_RENTAL_DAYS + " day in the future");
        }
        if (ChronoUnit.DAYS.between(rentalDate, requestDto.returnDate()) > MAX_RENTAL_DAYS) {
            throw new IllegalArgumentException("Maximum rental period is " + MAX_RENTAL_DAYS
                    + " days.");
        }
        carService.validateCarAvailableForRental(requestDto.carId());
    }

    private Rental getRentalWithAccessCheck(Long userId, Long rentalId) {
        Rental rental;
        boolean isManager = userService.isManager(userId);
        String errorMsg = "Rental not found with id: " + rentalId;

        if (isManager) {
            rental = rentalRepository.findById(rentalId)
                    .orElseThrow(() -> new EntityNotFoundException(errorMsg));
        } else {
            rental = rentalRepository.findRentalByIdAndUserId(rentalId, userId)
                    .orElseThrow(() -> new EntityNotFoundException(errorMsg));
        }
        return rental;
    }

    private BigDecimal calculateBaseRentalCost(BigDecimal dailyFee, Rental rental) {
        return dailyFee.multiply(BigDecimal.valueOf(
                ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate())));
    }

    private BigDecimal calculateSurcharge(BigDecimal dailyFee, Rental rental) {
        LocalDate actualReturnDate = rental.getActualReturnDate();

        if (actualReturnDate != null && actualReturnDate.isAfter(rental.getReturnDate())) {
            long daysLate = ChronoUnit.DAYS.between(rental.getReturnDate(), actualReturnDate);
            return dailyFee.multiply(BigDecimal.valueOf(daysLate)).multiply(LATE_FEE_MULTIPLIER);
        }
        return BigDecimal.ZERO;
    }

    private RentalStatus getRentalStatus(Rental rental) {
        if (rental.getActualReturnDate() == null) {
            return RentalStatus.ACTIVE;
        }
        return RentalStatus.RETURNED;
    }

    private RentalResponseDto buildRentalResponseDto(Rental rental, CarDto carDto) {
        RentalResponseDto dto = rentalMapper.toRentalDto(rental);
        dto.setCarDto(carDto);
        dto.setStatus(getRentalStatus(rental));
        dto.setBaseRentalCost(calculateBaseRentalCost(carDto.getDailyFee(), rental));
        return dto;
    }

    private RentalDetailedDto buildRentalDetailedDto(Rental rental) {
        RentalDetailedDto rentalDetailedDto = rentalMapper.toRentalDetailedDto(rental);

        CarDto carDto = carService.getById(rental.getCarId());
        BigDecimal baseRentalCost = calculateBaseRentalCost(carDto.getDailyFee(), rental);
        BigDecimal penaltyAmount = calculateSurcharge(carDto.getDailyFee(), rental);
        BigDecimal totalCost = baseRentalCost.add(penaltyAmount);
        BigDecimal amountPaid = baseRentalCost; // will be change
        BigDecimal amountDue = totalCost.subtract(amountPaid);

        rentalDetailedDto.setCarDto(carDto);
        rentalDetailedDto.setBaseRentalCost(baseRentalCost);
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
