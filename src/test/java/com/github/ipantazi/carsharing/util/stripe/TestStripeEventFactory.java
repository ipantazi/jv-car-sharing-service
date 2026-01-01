package com.github.ipantazi.carsharing.util.stripe;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_SESSION_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_SESSION_URL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SESSION_STATUS_COMPLETED;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SIG_HEADER_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestStripeSessionMetadataDto;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.model.Payment;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TestStripeEventFactory {

    private TestStripeEventFactory() {
    }

    public static String dummyPayload(
            Long rentalId,
            Payment.Type type,
            BigDecimal amount
    ) {
        return """
    {
      "id": "evt_test_123",
      "object": "event",
      "api_version": "2023-10-16",
      "type": "%s",
      "data": {
        "object": {
          "id": "%s",
          "object": "checkout.session",
          "url": "%s",
          "metadata": {
            "rentalId": "%d",
            "type": "%s",
            "amountToPay": "%s"
          }
        }
      }
    }
    """.formatted(
                SESSION_STATUS_COMPLETED,
                EXISTING_SESSION_ID,
                EXISTING_SESSION_URL,
                rentalId,
                type.name(),
                amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );
    }

    public static String validSignature() {
        return SIG_HEADER_TEST;
    }

    public static StripeTestContextHolder checkoutSessionCompleted(
            Long rentalId,
            Payment.Type type,
            BigDecimal amount
    ) {
        Session session = buildSession(rentalId, type, amount);

        Event event = buildEvent(session);

        StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(
                rentalId, amount.setScale(2, RoundingMode.HALF_UP));

        return new StripeTestContextHolder(event, session, metadataDto);
    }

    private static Session buildSession(
            Long rentalId,
            Payment.Type type,
            BigDecimal amount
    ) {
        Session session = new Session();
        session.setId(EXISTING_SESSION_ID);
        session.setUrl(EXISTING_SESSION_URL);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("rentalId", rentalId.toString());
        metadata.put("type", type.name());
        metadata.put(
                "amountToPay",
                amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );

        session.setMetadata(metadata);
        return session;
    }

    private static Event buildEvent(Session session) {
        Event event = mock(Event.class);
        EventDataObjectDeserializer deserializer =
                mock(EventDataObjectDeserializer.class);

        when(event.getType()).thenReturn(SESSION_STATUS_COMPLETED);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));

        return event;
    }
}
