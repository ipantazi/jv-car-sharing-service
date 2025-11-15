package com.github.ipantazi.carsharing.notification.dto;

import java.time.LocalDate;

public record NewRentalPayload(
        Long rentalId,
        String email,
        String firstName,
        String lastName,
        String carModel,
        String carBrand,
        String carType,
        LocalDate rentalDate,
        LocalDate returnDate
) {
}
