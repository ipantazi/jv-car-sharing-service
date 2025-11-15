package com.github.ipantazi.carsharing.service.payment.stripe;

import static com.github.ipantazi.carsharing.util.TestDataUtil.AMOUNT_TO_PAY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ENDPOINT_SECRET_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_SESSION_STATUS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYLOAD_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SESSION_STATUS_COMPLETED;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SIG_HEADER_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestStripeSessionMetadataDto;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.InvalidStripePayloadException;
import com.github.ipantazi.carsharing.service.payment.PaymentService;
import com.github.ipantazi.carsharing.service.payment.stripe.impl.StripeWebhookServiceImpl;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class StripeWebhookServiceTest {
    @Mock
    private StripeClient stripeClient;

    @Mock
    private PaymentService paymentService;

    @Mock
    private Session session;

    @Mock
    private Event event;

    @Mock
    private EventDataObjectDeserializer deserializer;

    @InjectMocks
    private StripeWebhookServiceImpl stripeWebhookService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(stripeWebhookService, "endpointSecret", ENDPOINT_SECRET_TEST);
    }

    @Test
    @DisplayName("Test processStripeEvent() method when session is completed")
    public void processStripeEvent_SessionCompleted_shouldHandlePaymentSuccess()
            throws StripeException {
        // Given
        StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(
                EXISTING_RENTAL_ID,
                AMOUNT_TO_PAY);

        when(stripeClient.constructEvent(PAYLOAD_TEST, SIG_HEADER_TEST, ENDPOINT_SECRET_TEST))
                .thenReturn(event);
        when(event.getType()).thenReturn(SESSION_STATUS_COMPLETED);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));
        when(stripeClient.getMetadataFromSession(session)).thenReturn(metadataDto);

        // When
        stripeWebhookService.processStripeEvent(PAYLOAD_TEST, SIG_HEADER_TEST);

        // Then
        verify(stripeClient, times(1))
                .constructEvent(PAYLOAD_TEST, SIG_HEADER_TEST, ENDPOINT_SECRET_TEST);
        verify(event, times(1)).getType();
        verify(event, times(1)).getDataObjectDeserializer();
        verify(deserializer, times(1)).getObject();
        verify(stripeClient, times(1)).getMetadataFromSession(session);
        verify(paymentService, times(1)).handlePaymentSuccess(metadataDto);
        verifyNoMoreInteractions(stripeClient, event, deserializer, paymentService);
    }

    @Test
    @DisplayName("Test processStripeEvent() method when session is not completed")
    public void processStripeEvent_SessionNotCompleted_shouldNotHandlePaymentSuccess()
            throws StripeException {
        // Given
        when(stripeClient.constructEvent(PAYLOAD_TEST, SIG_HEADER_TEST, ENDPOINT_SECRET_TEST))
                .thenReturn(event);
        when(event.getType()).thenReturn(INVALID_SESSION_STATUS);

        // When
        stripeWebhookService.processStripeEvent(PAYLOAD_TEST, SIG_HEADER_TEST);

        // Then
        verify(stripeClient, times(1))
                .constructEvent(PAYLOAD_TEST, SIG_HEADER_TEST, ENDPOINT_SECRET_TEST);
        verify(event, times(2)).getType();
        verify(paymentService, never()).handlePaymentSuccess(any());
        verifyNoMoreInteractions(stripeClient, event);
        verifyNoInteractions(deserializer, paymentService);
    }

    @Test
    @DisplayName("Test processStripeEvent() method when deserialization fails")
    public void processStripeEvent_DeserializationFails_ThrowsException() throws StripeException {
        // Given
        when(stripeClient.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);
        when(event.getType()).thenReturn(SESSION_STATUS_COMPLETED);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.empty());

        //When & Then
        assertThatThrownBy(() -> stripeWebhookService.processStripeEvent(
                PAYLOAD_TEST,
                SIG_HEADER_TEST
        ))
                .isInstanceOf(InvalidStripePayloadException.class)
                .hasMessage("Could not deserialize Stripe session object");

        verify(stripeClient, times(1))
                .constructEvent(PAYLOAD_TEST, SIG_HEADER_TEST, ENDPOINT_SECRET_TEST);
        verify(event, times(1)).getType();
        verify(event, times(1)).getDataObjectDeserializer();
        verify(deserializer, times(1)).getObject();
        verify(paymentService, never()).handlePaymentSuccess(any());
        verifyNoMoreInteractions(stripeClient, event, deserializer);
        verifyNoInteractions(paymentService);
    }
}
