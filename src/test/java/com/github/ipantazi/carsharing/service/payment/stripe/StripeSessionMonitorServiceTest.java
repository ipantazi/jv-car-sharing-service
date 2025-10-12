package com.github.ipantazi.carsharing.service.payment.stripe;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_PAYMENT_WITH_ID_101;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.stripe.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeSessionMonitorServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripeClient stripeClient;

    @InjectMocks
    private StripeSessionMonitorServiceImpl monitorService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );
    }

    @Test
    @DisplayName("Should expire payment when Stripe session is expired")
    void checkAndExpireSessions_SessionExpired_UpdatesStatus() throws Exception {
        // Given
        Payment payment = createTestPayment(
                EXISTING_PAYMENT_WITH_ID_101,
                Payment.Status.PENDING
        );

        when(paymentRepository.findPaymentsByStatus(Payment.Status.PENDING))
                .thenReturn(List.of(payment));
        when(stripeClient.isStripeSessionExpired(payment.getSessionId()))
                .thenReturn(true);

        // When
        monitorService.checkAndExpireSessions();

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.EXPIRED);
        verify(paymentRepository, times(1)).findPaymentsByStatus(Payment.Status.PENDING);
        verify(stripeClient, times(1)).isStripeSessionExpired(payment.getSessionId());
        verify(paymentRepository, times(1)).saveAll(List.of(payment));
        verifyNoMoreInteractions(paymentRepository, stripeClient);
    }

    @Test
    @DisplayName("Should not expire payment when Stripe session is still valid")
    void checkAndExpireSessions_SessionNotExpired_NoUpdate() throws Exception {
        // Given
        when(paymentRepository.findPaymentsByStatus(Payment.Status.PENDING))
                .thenReturn(List.of(payment));
        when(stripeClient.isStripeSessionExpired(payment.getSessionId()))
                .thenReturn(false);

        // When
        monitorService.checkAndExpireSessions();

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PENDING);
        verify(paymentRepository, times(1)).findPaymentsByStatus(Payment.Status.PENDING);
        verify(stripeClient, times(1)).isStripeSessionExpired(payment.getSessionId());
        verify(paymentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should log error and continue when Stripe throws exception")
    void checkAndExpireSessions_StripeException_LogsError() throws Exception {
        // Given
        when(paymentRepository.findPaymentsByStatus(Payment.Status.PENDING))
                .thenReturn(List.of(payment));
        when(stripeClient.isStripeSessionExpired(payment.getSessionId()))
                .thenThrow(new ApiException("fail", "requestId", "Bad request", 400, null));

        // When
        monitorService.checkAndExpireSessions();

        // Then
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.PENDING);
        verify(paymentRepository, times(1)).findPaymentsByStatus(Payment.Status.PENDING);
        verify(stripeClient, times(1)).isStripeSessionExpired(payment.getSessionId());
        verify(paymentRepository, never()).saveAll(any());
    }
}

