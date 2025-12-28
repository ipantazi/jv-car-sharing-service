package com.github.ipantazi.carsharing.service.payment.impl;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidPaymentAmountException;
import com.github.ipantazi.carsharing.exception.PendingPaymentsExistException;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.service.payment.PaymentValidator;
import com.github.ipantazi.carsharing.service.rental.Calculator;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentValidatorImpl implements PaymentValidator {
    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final Calculator calculator;

    @Override
    public void checkForPendingPayments(Long userId) {
        Long isExistPendingPayments = paymentRepository.existsByUserIdAndStatus(
                userId,
                String.valueOf(Payment.Status.PENDING)
        );

        if (isExistPendingPayments == 1L) {
            throw new PendingPaymentsExistException("User has pending payments");
        }
    }

    @Override
    public void checkingAmountToPay(StripeSessionMetadataDto metadataDto) {
        Long rentalId = metadataDto.rentalId();
        BigDecimal amountFromSession = metadataDto.amountToPay();

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found with id: "
                        + rentalId));
        BigDecimal expectedAmount = calculator.calculateAmountToPayByType(rental,
                metadataDto.type());

        if (!(expectedAmount.compareTo(amountFromSession) == 0)) {
            throw new InvalidPaymentAmountException(expectedAmount, amountFromSession);
        }
    }
}
