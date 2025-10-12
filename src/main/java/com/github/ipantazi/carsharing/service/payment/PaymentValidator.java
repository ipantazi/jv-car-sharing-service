package com.github.ipantazi.carsharing.service.payment;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.model.Payment;

public interface PaymentValidator {
    void checkForPendingPayments(Long userId);

    void checkingAmountToPay(StripeSessionMetadataDto metadataDto, Payment payment);
}
