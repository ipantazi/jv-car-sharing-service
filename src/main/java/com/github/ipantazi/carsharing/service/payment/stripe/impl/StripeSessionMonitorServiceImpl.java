package com.github.ipantazi.carsharing.service.payment.stripe.impl;

import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeClient;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeSessionMonitorService;
import com.stripe.exception.StripeException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeSessionMonitorServiceImpl implements StripeSessionMonitorService {
    private final PaymentRepository paymentRepository;
    private final StripeClient stripeClient;

    @Override
    @Transactional
    public void checkAndExpireSessions() {
        Payment.Status pendingStatus = Payment.Status.PENDING;
        Payment.Status expiredStatus = Payment.Status.EXPIRED;
        List<Payment> pendingPayments = paymentRepository.findPaymentsByStatus(pendingStatus);

        List<Payment> expiredPayments = new ArrayList<>();
        for (Payment payment : pendingPayments) {
            try {
                if (stripeClient.isStripeSessionExpired(payment.getSessionId())) {
                    payment.setStatus(expiredStatus);
                    expiredPayments.add(payment);
                }
            } catch (StripeException e) {
                log.error(
                        "Stripe session check failed for session ID {}: {}",
                        payment.getSessionId(),
                        e.getMessage());
            }
        }
        if (!expiredPayments.isEmpty()) {
            paymentRepository.saveAll(expiredPayments);
        }
    }
}
