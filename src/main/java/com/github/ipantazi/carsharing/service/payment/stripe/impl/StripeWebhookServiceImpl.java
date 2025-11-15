package com.github.ipantazi.carsharing.service.payment.stripe.impl;

import com.github.ipantazi.carsharing.exception.InvalidStripePayloadException;
import com.github.ipantazi.carsharing.service.payment.PaymentService;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeClient;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeWebhookService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements StripeWebhookService {
    private static final String SESSION_COMPLETED = "checkout.session.completed";
    private static final String PAYMENT_SUCCEEDED = "payment_intent.succeeded";

    private final StripeClient stripeClient;
    private final PaymentService paymentService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Override
    public void processStripeEvent(String payload, String sigHeader) throws StripeException {
        Event event = stripeClient.constructEvent(payload, sigHeader, endpointSecret);

        switch (event.getType()) {
            case SESSION_COMPLETED -> handleCheckoutSessionCompleted(event);
            case PAYMENT_SUCCEEDED -> log.debug("Ignoring payment_intent.succeeded "
                    + "(handled by checkout.session.completed)");
            default -> log.debug("Ignoring unsupported Stripe event type: {}", event.getType());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) throws StripeException {
        Optional<StripeObject> objectOpt = event.getDataObjectDeserializer().getObject();

        if (objectOpt.isPresent()) {
            Session session = (Session) objectOpt.get();
            paymentService.handlePaymentSuccess(stripeClient.getMetadataFromSession(session));
        } else {
            log.error("❌ Could not deserialize Stripe session object");
            throw new InvalidStripePayloadException("Could not deserialize Stripe session object");
        }
    }
}
