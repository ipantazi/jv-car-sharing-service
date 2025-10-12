package com.github.ipantazi.carsharing.service.payment;

import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.PaymentResponseDto;
import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.stripe.exception.StripeException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.util.UriComponentsBuilder;

public interface PaymentService {
    Page<PaymentResponseDto> getPayments(Long userId, Pageable pageable);

    PaymentResponseDto createPaymentSession(Long id,
                                           PaymentRequestDto paymentRequestDto,
                                           UriComponentsBuilder uriBuilder) throws StripeException;

    void handlePaymentSuccess(StripeSessionMetadataDto metadataDto) throws StripeException;

    String getPaymentSuccessMessage(String sessionId);

    PaymentResponseDto renewPaymentSession(
            Long userId,
            PaymentRequestDto paymentRequestDto,
            UriComponentsBuilder uriBuilder
    ) throws StripeException;
}
