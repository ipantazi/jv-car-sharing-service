package com.github.ipantazi.carsharing.service.rental;

import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface Calculator {
    BigDecimal calculateBaseRentalCost(BigDecimal dailyFee, Rental rental);

    BigDecimal calculatePenaltyAmount(BigDecimal dailyFee, Rental rental);

    BigDecimal calculateTotalAmountPaid(Long rentalId);

    BigDecimal calculateAmountToPayByType(Rental rental, Payment.Type type);

    long calculateDaysOverdue(LocalDate returnDate, LocalDate today);
}
