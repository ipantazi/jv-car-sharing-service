package com.github.ipantazi.carsharing.service.payment;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidPaymentAmountException;
import com.github.ipantazi.carsharing.exception.PendingPaymentsExistException;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
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
    public void checkingAmountToPay(StripeSessionMetadataDto metadataDto, Payment payment) {
        Long rentalId = metadataDto.rentalId();
        Payment.Type type = metadataDto.type();
        BigDecimal amountFromSession = metadataDto.amountToPay();
        BigDecimal expectedAmount;

        if (payment == null) {
            Rental rental = rentalRepository.findById(rentalId)
                    .orElseThrow(() -> new EntityNotFoundException("Rental not found with id: "
                            + rentalId));
            expectedAmount = calculator.calculateAmountToPayByType(rental, type);
        } else {
            expectedAmount = payment.getAmountToPay();
        }

        if (!expectedAmount.equals(amountFromSession)) {
            throw new InvalidPaymentAmountException(expectedAmount, amountFromSession);
        }
    }
}
