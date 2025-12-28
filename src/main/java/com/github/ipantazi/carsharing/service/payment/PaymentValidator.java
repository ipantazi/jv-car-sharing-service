package com.github.ipantazi.carsharing.service.payment;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;

public interface PaymentValidator {
    void checkForPendingPayments(Long userId);

    void checkingAmountToPay(StripeSessionMetadataDto metadataDto);
}
