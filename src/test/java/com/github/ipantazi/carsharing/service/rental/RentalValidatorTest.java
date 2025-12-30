package com.github.ipantazi.carsharing.service.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NUMBER_OF_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RETURN_DATE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.exception.InvalidRentalDatesException;
import com.github.ipantazi.carsharing.service.rental.impl.RentalValidatorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RentalValidatorTest {
    @InjectMocks
    private RentalValidatorImpl rentalValidator;

    @Test
    @DisplayName("Test checkDatesBeforeRenting() method when valid dates.")
    public void checkDatesBeforeRenting_ValidDates_NoException() {
        // Given
        RentalRequestDto requestDto = new RentalRequestDto(
                RETURN_DATE,
                EXISTING_CAR_ID
        );

        //When
        rentalValidator.checkDatesBeforeRenting(requestDto, RENTAL_DATE);
    }

    @Test
    @DisplayName("Test checkDatesBeforeRenting() method when invalid min rental days.")
    public void checkDatesBeforeRenting_InvalidMinRentalDays_ThrowsException() {
        // Given
        RentalRequestDto requestDto = new RentalRequestDto(
                RENTAL_DATE.plusDays(INVALID_MIN_RENTAL_DAYS),
                EXISTING_CAR_ID
        );

        // When & Then
        assertThatThrownBy(() -> rentalValidator.checkDatesBeforeRenting(requestDto, RENTAL_DATE))
                .isInstanceOf(InvalidRentalDatesException.class)
                .hasMessage("Return date must be no earlier than %d day in the future"
                        .formatted(MIN_RENTAL_DAYS));
    }

    @Test
    @DisplayName("Test checkDatesBeforeRenting() method when invalid max rental days.")
    public void checkDatesBeforeRenting_InvalidMaxRentalDays_ThrowsException() {
        // Given
        RentalRequestDto requestDto = new RentalRequestDto(
                RENTAL_DATE.plusDays(INVALID_MAX_RENTAL_DAYS),
                EXISTING_CAR_ID
        );

        // When & Then
        assertThatThrownBy(() -> rentalValidator.checkDatesBeforeRenting(requestDto, RENTAL_DATE))
                .isInstanceOf(InvalidRentalDatesException.class)
                .hasMessage("Maximum rental period is %d days.".formatted(MAX_RENTAL_DAYS));
    }

    @Test
    @DisplayName("Test checkDatesBeforeRenting() method when return date is in the past.")
    public void checkDatesBeforeRenting_ReturnDateInThePast_ThrowsException() {
        // Given
        RentalRequestDto requestDto = new RentalRequestDto(
                RENTAL_DATE.minusDays(NUMBER_OF_RENTAL_DAYS),
                EXISTING_CAR_ID
        );

        // When & Then
        assertThatThrownBy(() -> rentalValidator.checkDatesBeforeRenting(
                requestDto,
                RENTAL_DATE
        ))
                .isInstanceOf(InvalidRentalDatesException.class)
                .hasMessage("Return date must be in the future");
    }
}
