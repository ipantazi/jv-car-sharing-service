package com.github.ipantazi.carsharing.dto.payment;

import com.github.ipantazi.carsharing.model.Payment;
import java.math.BigDecimal;

public record StripeSessionMetadataDto(
        String sessionId,
        Long rentalId,
        Payment.Type type,
        BigDecimal amountToPay,
        String sessionUrl
) {
}
