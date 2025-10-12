package com.github.ipantazi.carsharing.service.rental;

import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.model.Rental;
import java.time.LocalDate;

public interface RentalValidator {
    void checkDatesBeforeRenting(RentalRequestDto requestDto, LocalDate rentalDate);

    Rental getRentalWithAccessCheck(Long userId, Long rentalId);
}
