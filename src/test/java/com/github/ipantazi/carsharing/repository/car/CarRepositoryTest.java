package com.github.ipantazi.carsharing.repository.car;

import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_BRAND;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_DAILY_FEE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_MODEL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SAFE_DELETED_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCar;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.ipantazi.carsharing.model.Car;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:database/cars/insert-test-cars.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:database/cars/clear-all-cars.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
public class CarRepositoryTest {
    @Autowired
    private CarRepository carRepository;

    @Test
    @DisplayName("Check for safe deleted car existence by model and brand.")
    void existsSoftDeletedByModelAndBrand_ExistCarByModelAndBrand_ReturnsLong() {
        // Given
        String carModel = CAR_MODEL + SAFE_DELETED_CAR_ID;
        String carBrand = CAR_BRAND + SAFE_DELETED_CAR_ID;
        Long expectedStatus = 1L;

        // When
        Long actualStatus = carRepository.existsSoftDeletedByModelAndBrand(carModel, carBrand);

        // Then
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("Check for safe deleted car existence by model and brand.")
    void existsSoftDeletedByModelAndBrand_NotExistCarByModelAndBrand_ReturnsLong() {
        // Given
        String carModel = CAR_MODEL + EXISTING_CAR_ID;
        String carBrand = CAR_BRAND + EXISTING_CAR_ID;
        Long expectedStatus = 0L;

        // When
        Long actualStatus = carRepository.existsSoftDeletedByModelAndBrand(carModel, carBrand);

        // Then
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("Test lockCarForUpdate() method works with existing car ID.")
    void lockCarForUpdate_ExistingCar_ReturnsCarOptional() {
        // Given
        Car expectedCar = createTestCar(EXISTING_CAR_ID);
        // expectedCar.setDailyFee(expectedCar.getDailyFee().setScale(2, RoundingMode.HALF_UP));

        // When
        Optional<Car> actualCarOpt = carRepository.lockCarForUpdate(EXISTING_CAR_ID);

        // Then
        assertThat(actualCarOpt).isPresent();
        assertObjectsAreEqualIgnoringFields(actualCarOpt.get(), expectedCar, CAR_IGNORING_FIELDS);
    }

    @Test
    @DisplayName("Test lockCarForUpdate() method works when car ID does not exist.")
    void lockCarForUpdate_NotExistingCar_ReturnsEmptyOptional() {
        // When
        Optional<Car> actualCarOpt = carRepository.lockCarForUpdate(NOT_EXISTING_CAR_ID);

        // Then
        assertThat(actualCarOpt).isEmpty();
    }

    @Test
    @DisplayName("Find daily fee by existing car ID should return correct value")
    void findDailyFeeByCarId_ExistingCar_ReturnsDailyFee() {
        // When
        Optional<BigDecimal> actualDailyFeeOpt = carRepository.findDailyFeeByCarId(
                EXISTING_CAR_ID);

        // Then
        assertThat(actualDailyFeeOpt).isPresent();
        assertThat(actualDailyFeeOpt.get()).isEqualByComparingTo(CAR_DAILY_FEE);
    }

    @Test
    @DisplayName("Find daily fee by not existing car ID should return empty optional")
    void findDailyFeeByCarId_NotExistingCar_ReturnsEmptyOptional() {
        // When
        Optional<BigDecimal> actualDailyFeeOpt = carRepository.findDailyFeeByCarId(
                NOT_EXISTING_CAR_ID);

        // Then
        assertThat(actualDailyFeeOpt).isEmpty();
    }
}
