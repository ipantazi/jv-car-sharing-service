package com.github.ipantazi.carsharing.service.rental.impl;

import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidRentalDatesException;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.service.rental.RentalValidator;
import com.github.ipantazi.carsharing.service.user.UserService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalValidatorImpl implements RentalValidator {
    private static final int MIN_RENTAL_DAYS = 2;
    private static final int MAX_RENTAL_DAYS = 30;
    private final RentalRepository rentalRepository;
    private final UserService userService;

    @Override
    public void checkDatesBeforeRenting(RentalRequestDto requestDto, LocalDate rentalDate) {
        if (requestDto.returnDate().isBefore(rentalDate)) {
            throw new InvalidRentalDatesException("Return date must be in the future");
        }
        if (requestDto.returnDate().isBefore(rentalDate.plusDays(MIN_RENTAL_DAYS))) {
            throw new InvalidRentalDatesException(
                    "Return date must be no earlier than %d day in the future"
                            .formatted(MIN_RENTAL_DAYS)
            );
        }
        if (ChronoUnit.DAYS.between(rentalDate, requestDto.returnDate()) > MAX_RENTAL_DAYS) {
            throw new InvalidRentalDatesException("Maximum rental period is %d days."
                    .formatted(MAX_RENTAL_DAYS)
            );
        }
    }

    @Override
    public Rental getRentalWithAccessCheck(Long userId, Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found with id: "
                        + rentalId));

        if (!userService.canAccessRental(userId, rental)) {
            throw new AccessDeniedException("You do not have permission to access this rental");
        }
        return rental;
    }
}
