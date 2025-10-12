package com.github.ipantazi.carsharing.service.payment.stripe;

import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;

public interface StripeClient {
    StripeSessionMetadataDto createSession(
            BigDecimal amount,
            String successUrl,
            String cancelUrl,
            PaymentRequestDto paymentRequestDto
    ) throws StripeException;

    boolean isStripeSessionExpired(String sessionId) throws StripeException;

    Event constructEvent(String payload, String sigHeader, String endpointSecret)
            throws SignatureVerificationException;

    StripeSessionMetadataDto getMetadataFromSession(Session session);
}
