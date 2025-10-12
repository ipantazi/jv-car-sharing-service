package com.github.ipantazi.carsharing.service.rental;

import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import com.github.ipantazi.carsharing.repository.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CalculatorImpl implements Calculator {
    private static final BigDecimal LATE_FEE_MULTIPLIER = BigDecimal.valueOf(1.5);
    private final PaymentRepository paymentRepository;
    private final CarRepository carRepository;

    @Override
    public BigDecimal calculateBaseRentalCost(BigDecimal dailyFee, Rental rental) {
        validateInputs(dailyFee, rental);

        return dailyFee.multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(
                rental.getRentalDate(),
                rental.getReturnDate()
        )));
    }

    @Override
    public BigDecimal calculatePenaltyAmount(BigDecimal dailyFee, Rental rental) {
        validateInputs(dailyFee, rental);

        LocalDate actualReturnDate = rental.getActualReturnDate();
        if (actualReturnDate == null) {
            throw new IllegalArgumentException("Rental is not returned");
        }
        long daysLate = ChronoUnit.DAYS.between(rental.getReturnDate(), actualReturnDate);
        if (daysLate <= 0) {
            throw new IllegalArgumentException("Rental is not late");
        }
        return dailyFee
                .multiply(BigDecimal.valueOf(daysLate))
                .multiply(LATE_FEE_MULTIPLIER);
    }

    @Override
    public BigDecimal calculateAmountToPayByType(Rental rental, Payment.Type type) {
        BigDecimal dailyFee = carRepository.findDailyFeeByCarId(rental.getCarId())
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        if (type.equals(Payment.Type.PAYMENT)) {
            return calculateBaseRentalCost(dailyFee, rental);
        } else if (type.equals(Payment.Type.FINE)) {
            return calculatePenaltyAmount(dailyFee, rental);
        }
        throw new IllegalArgumentException("Invalid payment type: " + type);
    }

    @Override
    public BigDecimal calculateTotalAmountPaid(Long rentalId) {
        return paymentRepository.sumAmountToPayByRentalIdAndStatus(rentalId, Payment.Status.PAID);
    }

    private void validateInputs(BigDecimal dailyFee, Rental rental) {
        if (dailyFee == null || dailyFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid daily fee: " + dailyFee);
        }
        if (rental == null || rental.getRentalDate() == null || rental.getReturnDate() == null) {
            throw new IllegalArgumentException("Invalid rental: " + rental);
        }
    }
}
