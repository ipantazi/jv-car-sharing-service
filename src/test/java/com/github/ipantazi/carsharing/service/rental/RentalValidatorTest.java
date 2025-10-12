package com.github.ipantazi.carsharing.service.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NUMBER_OF_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_DTO_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRental;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidRentalDatesException;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.service.user.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
public class RentalValidatorTest {
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private UserServiceImpl userService;
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

    @Test
    @DisplayName("Test getRentalWithAccessCheck() method when MANAGER is looking for alien rental.")
    public void getRentalWithAccessCheck_ManagerAndAlienRental_ThrowsException() {
        // Given
        Rental expectedRental = createTestRental(EXISTING_ID_ANOTHER_USER, null);

        when(rentalRepository.findById(EXISTING_RENTAL_ID))
                .thenReturn(Optional.of(expectedRental));
        when(userService.canAccessRental(EXISTING_ID_ANOTHER_USER, expectedRental))
                .thenReturn(Boolean.TRUE);

        //When
        Rental actualRental = rentalValidator.getRentalWithAccessCheck(
                EXISTING_ID_ANOTHER_USER,
                EXISTING_RENTAL_ID
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualRental,
                expectedRental,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(rentalRepository, times(1)).findById(EXISTING_RENTAL_ID);
        verify(userService, times(1)).canAccessRental(EXISTING_ID_ANOTHER_USER, expectedRental);
        verifyNoMoreInteractions(rentalRepository, userService);
    }

    @Test
    @DisplayName("Test getRentalWithAccessCheck() method when rental is not found.")
    public void getRentalWithAccessCheck_NonExistsRentalId_ThrowsException() {
        // Given
        when(rentalRepository.findById(NOT_EXISTING_RENTAL_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rentalValidator.getRentalWithAccessCheck(
                EXISTING_USER_ID,
                NOT_EXISTING_RENTAL_ID
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rental not found with id: " + NOT_EXISTING_RENTAL_ID);
        verify(rentalRepository, times(1)).findById(NOT_EXISTING_RENTAL_ID);
        verifyNoMoreInteractions(rentalRepository);
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName(
            "Test getRentalWithAccessCheck() method when CUSTOMER is looking for their rental."
    )
    public void getRentalWithAccessCheck_GivenCustomerRental_ReturnsRental() {
        // Given
        Rental expectedRental = createTestRental(EXISTING_USER_ID, null);

        when(rentalRepository.findById(expectedRental.getId()))
                .thenReturn(Optional.of(expectedRental));
        when(userService.canAccessRental(EXISTING_USER_ID, expectedRental))
                .thenReturn(Boolean.TRUE);

        // When
        Rental actualRental = rentalValidator.getRentalWithAccessCheck(
                EXISTING_USER_ID,
                expectedRental.getId()
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualRental,
                expectedRental,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(rentalRepository, times(1))
                .findById(expectedRental.getId());
        verify(userService, times(1)).canAccessRental(EXISTING_USER_ID, expectedRental);
        verifyNoMoreInteractions(userService, rentalRepository);
    }

    @Test
    @DisplayName(
            "Verify getRentalWithAccessCheck() method when CUSTOMER is looking for alien rental."
    )
    public void getRentalWithAccessCheck_CustomerAndAlienRental_ThrowsException() {
        // Given
        Rental expectedRental = createTestRental(EXISTING_USER_ID, null);

        when(rentalRepository.findById(EXISTING_RENTAL_ID_ANOTHER_USER))
                .thenReturn(Optional.of(expectedRental));
        when(userService.canAccessRental(EXISTING_USER_ID, expectedRental))
                .thenReturn(Boolean.FALSE);

        // When & Then
        assertThatThrownBy(() -> rentalValidator.getRentalWithAccessCheck(
                EXISTING_USER_ID,
                EXISTING_RENTAL_ID_ANOTHER_USER
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You do not have permission to access this rental");
        verify(rentalRepository, times(1)).findById(EXISTING_RENTAL_ID_ANOTHER_USER);
        verify(userService, times(1)).canAccessRental(EXISTING_USER_ID, expectedRental);
        verifyNoMoreInteractions(userService, rentalRepository);
    }
}
