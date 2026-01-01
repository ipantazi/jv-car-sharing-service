package com.github.ipantazi.carsharing.service.payment;

import static com.github.ipantazi.carsharing.util.TestDataUtil.AMOUNT_TO_PAY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_AMOUNT_TO_PAY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRental;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestStripeSessionMetadataDto;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.payment.StripeSessionMetadataDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidPaymentAmountException;
import com.github.ipantazi.carsharing.exception.PendingPaymentsExistException;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.service.payment.impl.PaymentValidatorImpl;
import com.github.ipantazi.carsharing.service.rental.Calculator;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentValidatorTest {
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private Calculator calculator;
    @Mock
    private PaymentRepository paymentRepository;
    @InjectMocks
    private PaymentValidatorImpl paymentValidator;

    @Test
    @DisplayName("Test checkForPendingPayments() method when user has pending payments")
    public void checkForPendingPayments_UserHasPendingPayments_ThrowsException() {
        // Given
        Long isExistPendingPayments = 1L;
        when(paymentRepository.existsByUserIdAndStatus(
                EXISTING_USER_ID,
                String.valueOf(Payment.Status.PENDING)
        )).thenReturn(isExistPendingPayments);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.checkForPendingPayments(EXISTING_USER_ID))
                .isInstanceOf(PendingPaymentsExistException.class)
                .hasMessage("User has pending payments");

        verify(paymentRepository, times(1)).existsByUserIdAndStatus(
                EXISTING_USER_ID,
                String.valueOf(Payment.Status.PENDING)
        );
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Test checkForPendingPayments() method when user has no pending payments")
    public void checkForPendingPayments_UserHasNoPendingPayments_DoesNotThrowException() {
        // Given
        Long isExistPendingPayments = 0L;
        when(paymentRepository.existsByUserIdAndStatus(
                EXISTING_USER_ID,
                String.valueOf(Payment.Status.PENDING)
        )).thenReturn(isExistPendingPayments);

        // When & Then
        paymentValidator.checkForPendingPayments(EXISTING_USER_ID);

        verify(paymentRepository, times(1)).existsByUserIdAndStatus(
                EXISTING_USER_ID,
                String.valueOf(Payment.Status.PENDING)
        );
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Test checkingAmountToPay() method when amount to pay is correct.")
    public void checkingAmountToPay_AmountToPayIsCorrect_Success() {
        // Given
        Rental rental = createTestRental(EXISTING_USER_ID, null);
        StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(
                EXISTING_RENTAL_ID,
                AMOUNT_TO_PAY
        );

        when(rentalRepository.findById(EXISTING_RENTAL_ID))
                .thenReturn(Optional.of(rental));
        when(calculator.calculateAmountToPayByType(rental, metadataDto.type()))
                .thenReturn(metadataDto.amountToPay());

        // When
        paymentValidator.checkingAmountToPay(metadataDto);

        // Then
        verify(rentalRepository, times(1)).findById(EXISTING_RENTAL_ID);
        verify(calculator, times(1)).calculateAmountToPayByType(rental, metadataDto.type());
        verifyNoMoreInteractions(rentalRepository, calculator);
    }

    @Test
    @DisplayName("Test checkingAmountToPay() method when rental not found")
    public void checkingAmountToPay_RentalNotFound_ThrowsException() {
        // Given
        StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(
                EXISTING_RENTAL_ID,
                AMOUNT_TO_PAY
        );

        when(rentalRepository.findById(metadataDto.rentalId()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentValidator.checkingAmountToPay(metadataDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rental not found with id: " + metadataDto.rentalId());

        verify(rentalRepository, times(1)).findById(metadataDto.rentalId());
        verifyNoMoreInteractions(rentalRepository);
        verifyNoInteractions(calculator);
    }

    @Test
    @DisplayName("Test checkingAmountToPay() method when amount to pay is incorrect.")
    public void checkingAmountToPay_AmountToPayIsIncorrect_ThrowsException() {
        // Given
        StripeSessionMetadataDto metadataDto = createTestStripeSessionMetadataDto(
                EXISTING_RENTAL_ID,
                INVALID_AMOUNT_TO_PAY
        );
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        when(rentalRepository.findById(metadataDto.rentalId()))
                .thenReturn(Optional.of(rental));
        when(calculator.calculateAmountToPayByType(rental, metadataDto.type()))
                .thenReturn(AMOUNT_TO_PAY);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.checkingAmountToPay(metadataDto))
                .isInstanceOf(InvalidPaymentAmountException.class)
                .hasMessage("Invalid amount paid for the rental. Expected: %s, Paid: %s"
                                .formatted(AMOUNT_TO_PAY, metadataDto.amountToPay()));

        verify(rentalRepository, times(1)).findById(EXISTING_RENTAL_ID);
        verify(calculator, times(1)).calculateAmountToPayByType(rental, metadataDto.type());
        verifyNoMoreInteractions(rentalRepository, calculator);
    }
}
