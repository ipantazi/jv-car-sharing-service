package com.github.ipantazi.carsharing.service.payment.stripe;

import com.stripe.exception.StripeException;

public interface StripeWebhookService {
    void processStripeEvent(String payload, String sigHeader)
            throws StripeException;
}
