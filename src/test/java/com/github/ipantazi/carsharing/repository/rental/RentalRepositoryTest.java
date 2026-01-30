package com.github.ipantazi.carsharing.repository.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRental;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.ipantazi.carsharing.config.BaseJpaIntegrationTest;
import com.github.ipantazi.carsharing.model.Rental;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {
        "classpath:database/rentals/clear-all-rentals.sql",
        "classpath:database/cars/clear-all-cars.sql",
        "classpath:database/users/clear-all-users.sql",
        "classpath:database/users/insert-test-users.sql",
        "classpath:database/cars/insert-test-cars.sql",
        "classpath:database/rentals/insert-test-rentals.sql"
},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = {
        "classpath:database/rentals/clear-all-rentals.sql",
        "classpath:database/cars/clear-all-cars.sql",
        "classpath:database/users/clear-all-users.sql"
},
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public class RentalRepositoryTest extends BaseJpaIntegrationTest {
    @Autowired
    private RentalRepository rentalRepository;

    @Test
    @DisplayName("Test lockRentalForUpdate() method when rental id is exists.")
    public void lockRentalForUpdate_ExistingRental_ReturnsRentalOptional() {
        // Given
        Rental expectedRental = createTestRental(EXISTING_RENTAL_ID, null);

        // When
        Optional<Rental> actualRentalOpt = rentalRepository.lockRentalForUpdate(
                EXISTING_RENTAL_ID);

        // Then
        assertTrue(actualRentalOpt.isPresent());
        assertObjectsAreEqualIgnoringFields(
                actualRentalOpt.get(),
                expectedRental,
                RENTAL_IGNORING_FIELDS
        );
    }

    @Test
    @DisplayName("Test lockRentalForUpdate() method when rental id is not exists.")
    public void lockRentalForUpdate_NotExistingRental_ReturnsEmptyOptional() {
        // When
        Optional<Rental> actualRentalOpt = rentalRepository.lockRentalForUpdate(
                NOT_EXISTING_RENTAL_ID);

        // Then
        assertTrue(actualRentalOpt.isEmpty());
    }

    @Test
    @DisplayName("Test lockActiveRentalsForUpdateByCarId() method when rentals exist.")
    public void lockActiveRentalsForUpdateByCarId_ExistingRentals_ReturnsRentalsList() {
        // Given
        Rental expectedRental = createTestRental(EXISTING_RENTAL_ID, null);

        // When
        List<Rental> actualRentals = rentalRepository.lockActiveRentalsForUpdateByCarId(
                EXISTING_CAR_ID);

        // Then
        assertThat(actualRentals).isNotEmpty();
        assertThat(actualRentals).hasSize(1);
        assertObjectsAreEqualIgnoringFields(
                actualRentals.get(0),
                expectedRental,
                RENTAL_IGNORING_FIELDS
        );
    }

    @Test
    @DisplayName("""
            Test lockActiveRentalsForUpdateByCarId() method when active rentals didn't find 
            by car id.
            """)
    public void lockActiveRentalsForUpdateByCarId_NotExistingRentals_ReturnsEmptyList() {
        // When
        List<Rental> actualRentals = rentalRepository.lockActiveRentalsForUpdateByCarId(
                NOT_EXISTING_RENTAL_ID);

        // Then
        assertThat(actualRentals).isEmpty();
    }
}
