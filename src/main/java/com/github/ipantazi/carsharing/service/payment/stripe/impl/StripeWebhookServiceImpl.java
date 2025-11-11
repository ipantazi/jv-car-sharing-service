package com.github.ipantazi.carsharing.service.payment.stripe.impl;

import com.github.ipantazi.carsharing.exception.InvalidStripePayloadException;
import com.github.ipantazi.carsharing.service.payment.PaymentService;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeClient;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeWebhookService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements StripeWebhookService {
    private static final String SESSION_STATUS_COMPLETE = "checkout.session.completed";
    private final StripeClient stripeClient;
    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Override
    public void processStripeEvent(String payload, String sigHeader)
            throws StripeException {
        Event event = stripeClient.constructEvent(payload, sigHeader, endpointSecret);

        if (SESSION_STATUS_COMPLETE.equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new InvalidStripePayloadException("Could not deserialize "
                            + "Stripe session object"));

            paymentService.handlePaymentSuccess(stripeClient.getMetadataFromSession(session));
        }
    }
}
