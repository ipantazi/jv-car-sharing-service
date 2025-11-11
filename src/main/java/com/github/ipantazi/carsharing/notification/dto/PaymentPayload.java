package com.github.ipantazi.carsharing.notification.dto;

import com.github.ipantazi.carsharing.model.Payment;
import java.math.BigDecimal;

public record PaymentPayload(
        Long paymentId,
        Long rentalId,
        String email,
        BigDecimal amount,

        Payment.Type type
) {
}
