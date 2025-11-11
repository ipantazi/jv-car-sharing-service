package com.github.ipantazi.carsharing.service.payment.stripe;

import static com.github.ipantazi.carsharing.util.TestDataUtil.AMOUNT_TO_PAY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CANCEL_URL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ENDPOINT_SECRET;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_SESSION_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_SESSION_URL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXPIRED_CREATED_TIME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXPIRY_SECONDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_CLOCK;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_PAYMENT_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_PAYMENT_TYPE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_STRIPE_AMOUNT_TO_PAY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYLOAD_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RECENT_CREATED_TIME;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SESSION_METADATA_IGNORING_FIELD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SIG_HEADER_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SUCCESS_URL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TEST_AMOUNT;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestStripeSessionMetadataDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.InvalidStripePayloadException;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.service.payment.stripe.impl.StripeClientImpl;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class StripeClientTest {
    private static StripeClientImpl stripeClient;

    @BeforeAll
    public static void init() {
        stripeClient = new StripeClientImpl(FIXED_CLOCK);
        ReflectionTestUtils.setField(stripeClient, "expirySeconds", EXPIRY_SECONDS);
    }

    @Test
    @DisplayName("Test getMetadataFromSession() method with valid session")
    public void getMetadataFromSession_ValidSession_ReturnsStripeSessionMetadataDto() {
        // Given
        StripeSessionMetadataDto expectedMetadataDto = createTestStripeSessionMetadataDto(
                EXISTING_RENTAL_ID,
                AMOUNT_TO_PAY
        );
        Map<String, String> metadataMap = Map.of(
                "rentalId", String.valueOf(EXISTING_RENTAL_ID),
                "type", String.valueOf(Payment.Type.PAYMENT),
                "amountToPay", String.valueOf(AMOUNT_TO_PAY)
        );
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(expectedMetadataDto.sessionId());
        when(session.getUrl()).thenReturn(expectedMetadataDto.sessionUrl());
        when(session.getMetadata()).thenReturn(metadataMap);

        // When
        StripeSessionMetadataDto actualMetadataDto = stripeClient.getMetadataFromSession(session);

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualMetadataDto,
                expectedMetadataDto,
                SESSION_METADATA_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test getMetadataFromSession() method when session id is missing")
    public void getMetadataFromSession_SessionIdMissing_ThrowsException() {
        // Given
        Session session = mock(Session.class);

        // When & Then
        assertThatThrownBy(() -> stripeClient.getMetadataFromSession(session))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessage("Missing sessionId in Stripe session");
    }

    @Test
    @DisplayName("Test getMetadataFromSession() method when rental id is missing")
    public void getMetadataFromSession_RentalIdMissing_ThrowsException() {
        // Given
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(EXISTING_SESSION_ID);

        // When & Then
        assertThatThrownBy(() -> stripeClient.getMetadataFromSession(session))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessage("Missing rentalId in session metadata, sessionId: "
                        + EXISTING_SESSION_ID);
    }

    @Test
    @DisplayName("Test getMetadataFromSession() method when type is missing")
    public void getMetadataFromSession_TypeMissing_ThrowsException() {
        // Given
        Map<String, String> metadataMap = Map.of(
                "rentalId", String.valueOf(EXISTING_RENTAL_ID),
                "amountToPay", String.valueOf(AMOUNT_TO_PAY)
        );
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(EXISTING_SESSION_ID);
        when(session.getMetadata()).thenReturn(metadataMap);

        // When & Then
        assertThatThrownBy(() -> stripeClient.getMetadataFromSession(session))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessage("Missing type in session metadata, sessionId: " + EXISTING_SESSION_ID);
    }

    @Test
    @DisplayName("Test getMetadataFromSession() method when amount is missing")
    public void getMetadataFromSession_AmountMissing_ThrowsException() {
        // Given
        Map<String, String> metadataMap = Map.of(
                "rentalId", String.valueOf(EXISTING_RENTAL_ID),
                "type", String.valueOf(Payment.Type.PAYMENT)
        );
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(EXISTING_SESSION_ID);
        when(session.getMetadata()).thenReturn(metadataMap);

        // When & Then
        assertThatThrownBy(() -> stripeClient.getMetadataFromSession(session))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessage("Missing amount in session metadata, sessionId: "
                        + EXISTING_SESSION_ID);
    }

    @Test
    @DisplayName("getMetadataFromSession throws when amountToPay is not a number")
    void getMetadataFromSession_AmountNotANumber_ThrowsException() {
        Map<String, String> metadata = Map.of(
                "rentalId", String.valueOf(EXISTING_RENTAL_ID),
                "type", String.valueOf(Payment.Type.PAYMENT),
                "amountToPay", INVALID_STRIPE_AMOUNT_TO_PAY
        );
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(EXISTING_SESSION_ID);
        when(session.getMetadata()).thenReturn(metadata);

        assertThatThrownBy(() -> stripeClient.getMetadataFromSession(session))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessageContaining("Invalid amount format in session metadata, sessionId: "
                        + EXISTING_SESSION_ID);
    }

    @Test
    @DisplayName("getMetadataFromSession throws when type is invalid")
    void getMetadataFromSession_InvalidType_ThrowsException() {
        Map<String, String> metadata = Map.of(
                "rentalId", String.valueOf(EXISTING_RENTAL_ID),
                "type", INVALID_PAYMENT_TYPE,
                "amountToPay", String.valueOf(AMOUNT_TO_PAY)
        );
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(EXISTING_SESSION_ID);
        when(session.getMetadata()).thenReturn(metadata);

        assertThatThrownBy(() -> stripeClient.getMetadataFromSession(session))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessage("Invalid type in session metadata, sessionId: " + EXISTING_SESSION_ID);
    }

    @Test
    @DisplayName("getMetadataFromSession throws when rentalId is not a number")
    void getMetadataFromSession_InvalidRentalId_ThrowsException() {
        Map<String, String> metadata = Map.of(
                "rentalId", INVALID_PAYMENT_RENTAL_ID,
                "type", INVALID_PAYMENT_TYPE,
                "amountToPay", String.valueOf(AMOUNT_TO_PAY)
        );
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(EXISTING_SESSION_ID);
        when(session.getMetadata()).thenReturn(metadata);

        assertThatThrownBy(() -> stripeClient.getMetadataFromSession(session))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessage("Invalid rentalId in session metadata, sessionId: "
                        + EXISTING_SESSION_ID);
    }

    @Test
    @DisplayName("isStripeSessionExpired returns true when session is older than 24h")
    void isStripeSessionExpired_SessionExpired_ReturnsTrue() throws StripeException {
        // Given
        Session mockSession = mock(Session.class);
        when(mockSession.getCreated()).thenReturn(EXPIRED_CREATED_TIME);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve(EXISTING_SESSION_ID)).thenReturn(mockSession);

            // When
            boolean isExpired = stripeClient.isStripeSessionExpired(EXISTING_SESSION_ID);

            // Then
            assertThat(isExpired).isTrue();
        }

        verify(mockSession, times(1)).getCreated();
        verifyNoMoreInteractions(mockSession);
    }

    @Test
    @DisplayName("isStripeSessionExpired returns false when session is recent")
    void isStripeSessionExpired_SessionNotExpired_ReturnsFalse() throws StripeException {
        // Given
        Session mockSession = mock(Session.class);
        when(mockSession.getCreated()).thenReturn(RECENT_CREATED_TIME);

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.retrieve(EXISTING_SESSION_ID)).thenReturn(mockSession);

            // When
            boolean isExpired = stripeClient.isStripeSessionExpired(EXISTING_SESSION_ID);

            // Then
            assertThat(isExpired).isFalse();
        }

        verify(mockSession, times(1)).getCreated();
        verifyNoMoreInteractions(mockSession);
    }

    @Test
    @DisplayName("Test createSession() method with valid request")
    void createSession_ValidRequest_ReturnMetadataDto() throws StripeException {
        // Given
        Payment.Type paymentType = Payment.Type.PAYMENT;
        PaymentRequestDto dto = new PaymentRequestDto(
                EXISTING_RENTAL_ID,
                String.valueOf(paymentType)
        );

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(EXISTING_SESSION_ID);
        when(mockSession.getUrl()).thenReturn(EXISTING_SESSION_URL);
        when(mockSession.getMetadata()).thenReturn(
                Map.of(
                        "rentalId", String.valueOf(EXISTING_RENTAL_ID),
                        "type", String.valueOf(paymentType),
                        "amountToPay", String.valueOf(TEST_AMOUNT)
                )
        );

        try (MockedStatic<Session> mockedStatic = mockStatic(Session.class)) {
            mockedStatic
                    .when(() -> Session.create(
                            ArgumentMatchers.<SessionCreateParams>any(),
                            ArgumentMatchers.<RequestOptions>any()
                    ))
                    .thenReturn(mockSession);

            // When
            StripeSessionMetadataDto result = stripeClient.createSession(
                    TEST_AMOUNT,
                    SUCCESS_URL,
                    CANCEL_URL,
                    dto
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.sessionId()).isEqualTo(EXISTING_SESSION_ID);
            assertThat(result.sessionUrl()).isEqualTo(EXISTING_SESSION_URL);

            mockedStatic.verify(() -> Session.create(
                    ArgumentMatchers.<SessionCreateParams>any(),
                    ArgumentMatchers.<RequestOptions>any()
            ));
        }

        verify(mockSession, times(1)).getId();
        verify(mockSession, times(1)).getUrl();
        verify(mockSession, times(3)).getMetadata();
        verifyNoMoreInteractions(mockSession);
    }

    @Test
    @DisplayName("Test constructEvent() method when webhook succeeds")
    void constructEvent_WebhookSucceeds_returnsEvent() throws Exception {
        // Given
        Event mockedEvent = mock(Event.class);

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(
                            eq(PAYLOAD_TEST),
                            eq(SIG_HEADER_TEST),
                            eq(ENDPOINT_SECRET)
                    ))
                    .thenReturn(mockedEvent);

            // When
            Event result = stripeClient.constructEvent(
                    PAYLOAD_TEST,
                    SIG_HEADER_TEST,
                    ENDPOINT_SECRET
            );

            // Then
            assertThat(result).isSameAs(mockedEvent);

            mockedWebhook.verify(() -> Webhook.constructEvent(
                    eq(PAYLOAD_TEST),
                    eq(SIG_HEADER_TEST),
                    eq(ENDPOINT_SECRET)
            ));
        }
    }

    @Test
    @DisplayName("Test constructEvent() method when webhook fails")
    void constructEvent_WebhookFails_ThrowsException() {
        // Given
        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            mockedWebhook.when(() -> Webhook.constructEvent(
                            eq(PAYLOAD_TEST),
                            eq(SIG_HEADER_TEST),
                            eq(ENDPOINT_SECRET)
                    ))
                    .thenThrow(new SignatureVerificationException(
                            "Invalid signature",
                            SIG_HEADER_TEST
                    ));

            // When & Then
            assertThatThrownBy(() -> stripeClient.constructEvent(
                    PAYLOAD_TEST,
                    SIG_HEADER_TEST,
                    ENDPOINT_SECRET
            ))
                    .isInstanceOf(SignatureVerificationException.class)
                    .hasMessageContaining("Invalid signature");

            mockedWebhook.verify(() -> Webhook.constructEvent(
                    eq(PAYLOAD_TEST),
                    eq(SIG_HEADER_TEST),
                    eq(ENDPOINT_SECRET)
            ));
        }
    }
}
