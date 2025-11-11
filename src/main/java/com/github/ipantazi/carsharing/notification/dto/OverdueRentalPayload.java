package com.github.ipantazi.carsharing.notification.dto;

import java.time.LocalDate;

public record OverdueRentalPayload(
        Long rentalId,
        String email,
        String carModel,
        String carBrand,
        String carType,
        LocalDate returnDate,
        long daysOverdue
) {
}
