package com.github.ipantazi.carsharing.service.payment.impl;

import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.PaymentResponseDto;
import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidPaymentStatusException;
import com.github.ipantazi.carsharing.exception.PaymentAlreadyPaidException;
import com.github.ipantazi.carsharing.exception.PendingPaymentsExistException;
import com.github.ipantazi.carsharing.mapper.PaymentMapper;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.notification.NotificationMapper;
import com.github.ipantazi.carsharing.notification.NotificationService;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.github.ipantazi.carsharing.service.payment.PaymentService;
import com.github.ipantazi.carsharing.service.payment.PaymentValidator;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeClient;
import com.github.ipantazi.carsharing.service.rental.Calculator;
import com.github.ipantazi.carsharing.service.rental.RentalService;
import com.github.ipantazi.carsharing.service.user.UserService;
import com.stripe.exception.StripeException;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Payment.Status STATUS_PAID = Payment.Status.PAID;
    private static final Payment.Status STATUS_PENDING = Payment.Status.PENDING;
    private static final String SUCCESS_URL = "/payments/success";
    private static final String CANCEL_URL = "/payments/cancel";
    private final PaymentRepository paymentRepository;
    private final RentalService rentalService;
    private final StripeClient stripeClient;
    private final PaymentMapper paymentMapper;
    private final Calculator calculator;
    private final PaymentValidator paymentValidator;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDto> getPayments(Long userId, Pageable pageable) {

        Page<Payment> paymentPage = paymentRepository.findByUserIdOrAll(
                userId,
                pageable
        );

        return paymentPage.map(paymentMapper::toPaymentResponseDto);
    }

    @Override
    @Transactional
    public PaymentResponseDto createPaymentSession(
            Long userId,
            PaymentRequestDto paymentRequestDto,
            UriComponentsBuilder uriBuilder
    ) throws StripeException {

        Payment.Type type = Payment.Type.valueOfType(paymentRequestDto.type());
        Long rentalId = paymentRequestDto.rentalId();

        Rental rental = rentalService.getRentalEntityByIdAndUserId(userId, rentalId);

        Optional<Payment> paymentOpt = paymentRepository.lockPaymentForUpdate(rentalId, type)
                .map(this::validatePaymentStatus);

        StripeSessionMetadataDto sessionDto = createStripeSession(
                rental,
                uriBuilder,
                paymentRequestDto
        );

        Payment payment = paymentOpt
                .map(existing -> updatePaymentWithNewSession(existing, sessionDto, STATUS_PENDING))
                .orElseGet(() -> buildPayment(sessionDto, STATUS_PENDING));

        try {
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            log.info("Concurrent payment insert detected for rentalId={}, type={} (createPayment)",
                    rentalId, type);
            payment = paymentRepository.lockPaymentForUpdate(rentalId, type)
                    .orElseThrow(() -> e);
        }

        return paymentMapper.toPaymentResponseDto(payment);
    }

    @Override
    public String getPaymentSuccessMessage(String sessionId) {
        Payment payment = paymentRepository.findPaymentBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment not found for session id: " + sessionId));

        return switch (payment.getStatus()) {
            case PAID -> "Thank you for your payment. Payment successful.";
            case PENDING -> "Thank you for your payment. Payment is being processed.";
            default -> throw new InvalidPaymentStatusException("Unexpected status: "
                    + payment.getStatus());
        };
    }

    @Override
    @Transactional
    public PaymentResponseDto renewPaymentSession(
            Long userId,
            PaymentRequestDto paymentRequestDto,
            UriComponentsBuilder uriBuilder
    ) throws StripeException {

        Payment.Type type = Payment.Type.valueOfType(paymentRequestDto.type());
        Long rentalId = paymentRequestDto.rentalId();

        Rental rental = rentalService.getRentalEntityByIdAndUserId(userId, rentalId);

        Payment payment = paymentRepository.lockPaymentForUpdate(rentalId, type)
                .map(this::validatePaymentStatus)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No previous session found for this rental: %d and type: %s."
                                .formatted(rentalId, type)
                                + " Please create a new payment session."
                ));

        StripeSessionMetadataDto sessionDto = createStripeSession(
                rental,
                uriBuilder,
                paymentRequestDto
        );

        updatePaymentWithNewSession(payment, sessionDto, STATUS_PENDING);

        return paymentMapper.toPaymentResponseDto(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(StripeSessionMetadataDto metadataDto) {
        Long rentalId = metadataDto.rentalId();
        Payment.Type type = metadataDto.type();
        Payment payment;

        paymentValidator.checkingAmountToPay(metadataDto);
        Optional<Payment> paymentOpt = paymentRepository.lockPaymentForUpdate(rentalId, type);

        if (paymentOpt.isPresent()) {
            payment = paymentOpt.get();
            if (payment.getStatus() == STATUS_PAID) {
                return;
            }
            updatePaymentWithNewSession(payment, metadataDto, STATUS_PAID);
            paymentRepository.save(payment);

        } else {
            payment = buildPayment(metadataDto, STATUS_PAID);
            try {
                paymentRepository.save(payment);
            } catch (DataIntegrityViolationException e) {
                log.info("Concurrent payment insert detected for rentalId={}, type={} (WebHook)",
                        rentalId, type);
            }
            payment = paymentRepository.lockPaymentForUpdate(rentalId, type)
                    .orElseThrow(() ->
                            new IllegalStateException("Payment must exist after webhook"));
        }
        if (payment.getStatus() != STATUS_PAID) {
            updatePaymentWithNewSession(payment, metadataDto, STATUS_PAID);
            paymentRepository.save(payment);
        }

        PaymentPayload paymentPayload = notificationMapper.toPaymentPayload(
                payment,
                userService.getEmailByRentalId(payment.getRentalId())
        );
        notificationService.sendMessage(
                NotificationType.PAYMENT_SUCCESSFUL,
                paymentPayload
        );
    }

    private StripeSessionMetadataDto createStripeSession(
            Rental rental,
            UriComponentsBuilder uriBuilder,
            PaymentRequestDto requestDto
    ) throws StripeException {

        Payment.Type type = Payment.Type.valueOfType(requestDto.type());
        String successUrl = uriBuilder.path(SUCCESS_URL).toUriString();
        String cancelUrl = uriBuilder.path(CANCEL_URL).toUriString();
        BigDecimal amount = calculator.calculateAmountToPayByType(rental, type);

        return stripeClient.createSession(amount, successUrl, cancelUrl, requestDto);
    }

    private Payment validatePaymentStatus(Payment payment) {
        Long rentalId = payment.getRentalId();
        String type = payment.getType().name();

        return switch (payment.getStatus()) {
            case PAID -> throw new PaymentAlreadyPaidException(rentalId, type);
            case PENDING -> throw new PendingPaymentsExistException(
                    "There is already a pending payment for rental: %d and type: %s. "
                            .formatted(rentalId, type)
                            + "Please complete your session by url: %s"
                            .formatted(payment.getSessionUrl())
            );
            case EXPIRED -> payment;
        };
    }

    private Payment updatePaymentWithNewSession(Payment payment,
                                                StripeSessionMetadataDto metadataDto,
                                                Payment.Status status) {

        if (metadataDto.sessionUrl() == null) {
            log.warn("The StripeSessionMetadataDto does not contain the URL for session ID: {}",
                    metadataDto.sessionId());
        } else {
            payment.setSessionUrl(metadataDto.sessionUrl());
        }
        payment.setSessionId(metadataDto.sessionId());
        payment.setAmountToPay(metadataDto.amountToPay());
        payment.setStatus(status);
        return payment;
    }

    private Payment buildPayment(StripeSessionMetadataDto metadataDto, Payment.Status status) {
        Payment payment = new Payment();
        payment.setRentalId(metadataDto.rentalId());
        payment.setType(metadataDto.type());
        payment.setSessionId(metadataDto.sessionId());
        payment.setAmountToPay(metadataDto.amountToPay());
        payment.setSessionUrl(metadataDto.sessionUrl());
        payment.setStatus(status);
        return payment;
    }
}
