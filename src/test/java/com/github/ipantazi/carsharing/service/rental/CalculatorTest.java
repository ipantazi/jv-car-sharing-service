package com.github.ipantazi.carsharing.service.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.ACTUAL_RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ACTUAL_RETURN_DATE_AFTER_RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_DAILY_FEE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.LATE_FEE_MULTIPLIER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRental;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CalculatorTest {
    @Mock
    private CarRepository carRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @InjectMocks
    private CalculatorImpl calculator;

    @Test
    @DisplayName("Test calculateBaseRentalCost() method works as expected")
    public void calculateBaseRentalCost_ValidData_ReturnsBaseRentalCost() {
        // Given
        Rental rental = createTestRental(EXISTING_USER_ID, null);
        long number = ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate());
        BigDecimal expected = CAR_DAILY_FEE.multiply(BigDecimal.valueOf(number));

        // When
        BigDecimal actual = calculator.calculateBaseRentalCost(CAR_DAILY_FEE, rental);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Test calculateBaseRentalCost() method when daily fee is null")
    public void calculateBaseRentalCost_NullDailyFee_ThrowsException() {
        // Given
        BigDecimal dailyFee = null;
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        // When & Then
        assertThatThrownBy(() -> calculator.calculateBaseRentalCost(dailyFee, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid daily fee: " + dailyFee);
    }

    @Test
    @DisplayName("Test calculateBaseRentalCost() method when daily fee is negative")
    public void calculateBaseRentalCost_NegativeDailyFee_ThrowsException() {
        // Given
        BigDecimal dailyFee = BigDecimal.valueOf(-1);
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        // When & Then
        assertThatThrownBy(() -> calculator.calculateBaseRentalCost(dailyFee, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid daily fee: " + dailyFee);
    }

    @Test
    @DisplayName("Test calculateBaseRentalCost() method when rental is null")
    public void calculateBaseRentalCost_NullRental_ThrowsException() {
        // Given
        Rental rental = null;

        // When & Then
        assertThatThrownBy(() -> calculator.calculateBaseRentalCost(CAR_DAILY_FEE, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid rental: " + rental);
    }

    @Test
    @DisplayName("Test calculateBaseRentalCost() method when rental dates are null")
    public void calculateBaseRentalCost_NullRentalDates_ThrowsException() {
        // Given
        Rental rental = createTestRental(EXISTING_USER_ID, null);
        rental.setRentalDate(null);
        rental.setReturnDate(null);

        // When & Then
        assertThatThrownBy(() -> calculator.calculateBaseRentalCost(CAR_DAILY_FEE, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid rental: " + rental);
    }

    @Test
    @DisplayName(
            "Test calculatePenaltyAmount() method when actual return date is after return date"
    )
    public void calculatePenaltyAmount_ValidData_ReturnsPenaltyAmount() {
        // Given
        Rental rental = createTestRental(
                EXISTING_USER_ID,
                ACTUAL_RETURN_DATE_AFTER_RETURN_DATE
        );
        long daysLate = ChronoUnit.DAYS
                .between(rental.getReturnDate(), rental.getActualReturnDate());
        BigDecimal expected = CAR_DAILY_FEE
                .multiply(BigDecimal.valueOf(daysLate))
                .multiply(LATE_FEE_MULTIPLIER);

        // When
        BigDecimal actual = calculator.calculatePenaltyAmount(CAR_DAILY_FEE, rental);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Test calculatePenaltyAmount() method when daily fee is null")
    public void calculatePenaltyAmount_NullDailyFee_ThrowsException() {
        // Given
        BigDecimal dailyFee = null;
        Rental rental = createTestRental(EXISTING_USER_ID, ACTUAL_RETURN_DATE);

        // When & Then
        assertThatThrownBy(() -> calculator.calculatePenaltyAmount(dailyFee, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid daily fee: " + dailyFee);
    }

    @Test
    @DisplayName("Test calculatePenaltyAmount() method when daily fee is negative")
    public void calculatePenaltyAmount_NegativeDailyFee_ThrowsException() {
        // Given
        BigDecimal dailyFee = BigDecimal.valueOf(-1);
        Rental rental = createTestRental(EXISTING_USER_ID, ACTUAL_RETURN_DATE);

        // When & Then
        assertThatThrownBy(() -> calculator.calculatePenaltyAmount(dailyFee, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid daily fee: " + dailyFee);
    }

    @Test
    @DisplayName("Test calculatePenaltyAmount() method when rental is null")
    public void calculatePenaltyAmount_NullRental_ThrowsException() {
        // Given
        Rental rental = null;

        // When & Then
        assertThatThrownBy(() -> calculator.calculatePenaltyAmount(CAR_DAILY_FEE, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid rental: " + rental);
    }

    @Test
    @DisplayName("Test calculatePenaltyAmount() method when actual return date is null")
    public void calculatePenaltyAmount_NullActualReturnDate_ThrowsException() {
        // Given
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        // When & Then
        assertThatThrownBy(() -> calculator.calculatePenaltyAmount(CAR_DAILY_FEE, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rental is not returned");
    }

    @Test
    @DisplayName(
            "Test calculatePenaltyAmount() method when actual return date is before return date"
    )
    public void calculatePenaltyAmount_ActualReturnDateBeforeReturnDate_ThrowsException() {
        // Given
        Rental rental = createTestRental(EXISTING_USER_ID, ACTUAL_RETURN_DATE);

        // When & Then
        assertThatThrownBy(() -> calculator.calculatePenaltyAmount(CAR_DAILY_FEE, rental))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rental is not late");
    }

    @Test
    @DisplayName("Test calculateAmountToPayByType() method with type PAYMENT")
    public void calculateAmountToPayByType_PaymentType_ReturnsAmountToPay() {
        // Given
        Payment.Type type = Payment.Type.PAYMENT;
        Rental rental = createTestRental(EXISTING_USER_ID, null);
        BigDecimal expected = calculator.calculateBaseRentalCost(CAR_DAILY_FEE, rental);

        when(carRepository.findDailyFeeByCarId(rental.getCarId()))
                .thenReturn(Optional.of(CAR_DAILY_FEE));

        // When
        BigDecimal actual = calculator.calculateAmountToPayByType(rental, type);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Test calculateAmountToPayByType() method with type FINE")
    public void calculateAmountToPayByType_PenaltyType_ReturnsAmountToPay() {
        // Given
        Payment.Type type = Payment.Type.FINE;
        Rental rental = createTestRental(
                EXISTING_USER_ID,
                ACTUAL_RETURN_DATE_AFTER_RETURN_DATE
        );
        BigDecimal expected = calculator.calculatePenaltyAmount(CAR_DAILY_FEE, rental);

        when(carRepository.findDailyFeeByCarId(rental.getCarId()))
                .thenReturn(Optional.of(CAR_DAILY_FEE));

        // When
        BigDecimal actual = calculator.calculateAmountToPayByType(rental, type);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Test calculateAmountToPayByType() method when dailyFee not found")
    public void calculateAmountToPayByType_DailyFeeNotFound_ThrowsException() {
        // Given
        Payment.Type type = Payment.Type.PAYMENT;
        Rental rental = createTestRental(EXISTING_USER_ID, null);

        when(carRepository.findDailyFeeByCarId(rental.getCarId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> calculator.calculateAmountToPayByType(rental, type))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Car not found");
    }

    @Test
    @DisplayName("Test calculateTotalAmountPaid() method with payments")
    public void calculateTotalAmountPaid_Payments_ReturnsTotalAmountPaid() {
        // Given
        Payment.Status status = Payment.Status.PAID;
        BigDecimal expected = BigDecimal.TEN;

        when(paymentRepository.sumAmountToPayByRentalIdAndStatus(EXISTING_RENTAL_ID, status))
                .thenReturn(expected);

        // When
        BigDecimal actual = calculator.calculateTotalAmountPaid(EXISTING_RENTAL_ID);

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("Test calculateTotalAmountPaid() method with no payments")
    public void calculateTotalAmountPaid_NoPayments_ReturnsZero() {
        // Given
        Payment.Status status = Payment.Status.PAID;

        when(paymentRepository.sumAmountToPayByRentalIdAndStatus(EXISTING_RENTAL_ID, status))
                .thenReturn(BigDecimal.ZERO);

        // When
        BigDecimal actual = calculator.calculateTotalAmountPaid(EXISTING_RENTAL_ID);

        // Then
        assertThat(actual).isEqualTo(BigDecimal.ZERO);
    }
}
