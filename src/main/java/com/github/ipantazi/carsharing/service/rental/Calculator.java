package com.github.ipantazi.carsharing.service.rental;

import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import java.math.BigDecimal;

public interface Calculator {
    BigDecimal calculateBaseRentalCost(BigDecimal dailyFee, Rental rental);

    BigDecimal calculatePenaltyAmount(BigDecimal dailyFee, Rental rental);

    BigDecimal calculateTotalAmountPaid(Long rentalId);

    BigDecimal calculateAmountToPayByType(Rental rental, Payment.Type type);
}
