package com.github.ipantazi.carsharing.service.rental.impl;

import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.exception.InvalidRentalDatesException;
import com.github.ipantazi.carsharing.service.rental.RentalValidator;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentalValidatorImpl implements RentalValidator {
    private static final int MIN_RENTAL_DAYS = 2;
    private static final int MAX_RENTAL_DAYS = 30;

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
}
