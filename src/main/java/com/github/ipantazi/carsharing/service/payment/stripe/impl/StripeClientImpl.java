package com.github.ipantazi.carsharing.service.payment.stripe.impl;

import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.InvalidStripePayloadException;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeClient;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeClientImpl implements StripeClient {
    private static final String RENTAL_ID_KEY = "rentalId";
    private static final String TYPE_KEY = "type";
    private static final String AMOUNT_KEY = "amountToPay";
    private final Clock clock;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.session.expiry-seconds}")
    private long expirySeconds;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    public StripeSessionMetadataDto createSession(BigDecimal amount,
                                                  String successUrl,
                                                  String cancelUrl,
                                                  PaymentRequestDto paymentRequestDto)
            throws StripeException {
        Long rentalId = paymentRequestDto.rentalId();
        String type = paymentRequestDto.type();

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("Car Rental Payment")
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("usd")
                        .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceData)
                        .build();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(cancelUrl)
                        .putMetadata(RENTAL_ID_KEY, rentalId.toString())
                        .putMetadata(TYPE_KEY, type)
                        .putMetadata(AMOUNT_KEY, amount.setScale(2, RoundingMode.HALF_UP)
                                .toPlainString())
                        .addLineItem(lineItem)
                        .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("rental-" + rentalId + "-type-" + type + "-ts-"
                        + System.currentTimeMillis())
                .build();
        return getMetadataFromSession(Session.create(params, options));
    }

    @Override
    public boolean isStripeSessionExpired(String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);
        long createdEpoch = session.getCreated();
        long nowEpoch = Instant.now(clock).getEpochSecond();

        return (nowEpoch - createdEpoch) > expirySeconds;
    }

    @Override
    public Event constructEvent(String payload, String sigHeader, String endpointSecret) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            log.info("✅ Received Stripe event: {}", event.getType());
        } catch (SignatureVerificationException e) {
            log.warn("❌ Invalid Stripe signature: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid signature");
        } catch (Exception e) {
            log.warn("❌ Invalid Stripe payload: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid payload");
        }

        return event;
    }

    @Override
    public StripeSessionMetadataDto getMetadataFromSession(Session session) {
        String sessionId = Optional.ofNullable(session.getId())
                .orElseThrow(() -> new InvalidStripePayloadException("Missing sessionId in Stripe"
                        + " session"));

        Long rentalId = Optional.ofNullable(session.getMetadata().get(RENTAL_ID_KEY))
                .map(val -> {
                    try {
                        return Long.parseLong(val);
                    } catch (NumberFormatException e) {
                        throw new InvalidStripePayloadException("Invalid rentalId in session "
                                + "metadata, sessionId: " + sessionId, e);
                    }
                })
                .orElseThrow(() -> new InvalidStripePayloadException("Missing rentalId in session "
                        + "metadata, sessionId: " + sessionId));

        Payment.Type type = Optional.ofNullable(session.getMetadata().get(TYPE_KEY))
                .map(val -> {
                    try {
                        return Payment.Type.valueOf(val);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidStripePayloadException("Invalid type in session metadata, "
                                + "sessionId: " + sessionId, e);
                    }
                })
                .orElseThrow(() -> new InvalidStripePayloadException("Missing type in session "
                        + "metadata, sessionId: " + sessionId));

        BigDecimal amountFromSession = Optional.ofNullable(session.getMetadata().get(AMOUNT_KEY))
                .map(val -> {
                    try {
                        return new BigDecimal(val);
                    } catch (NumberFormatException e) {
                        throw new InvalidStripePayloadException("Invalid amount format in "
                                + "session metadata, sessionId: " + sessionId, e);
                    }
                })
                .map(amount -> amount.setScale(2, RoundingMode.HALF_UP))
                .orElseThrow(() -> new InvalidStripePayloadException("Missing amount in session "
                        + "metadata, sessionId: " + sessionId));

        String url = session.getUrl();
        if (url == null) {
            log.warn("Stripe session does not contain a Url, sessionId: {}", sessionId);
        }

        return new StripeSessionMetadataDto(sessionId, rentalId, type, amountFromSession, url);
    }
}
