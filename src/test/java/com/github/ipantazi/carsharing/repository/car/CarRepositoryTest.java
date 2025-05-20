package com.github.ipantazi.carsharing.repository.car;

import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_BRAND;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_MODEL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SAFE_DELETED_CAR_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CarRepositoryTest {
    @Autowired
    private CarRepository carRepository;

    @Test
    @DisplayName("Check for safe deleted car existence by model and brand.")
    @Sql(scripts = "classpath:database/cars/insert-test-cars.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:database/cars/clear-all-cars.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void existsSoftDeletedByModelAndBrand_IsExistCarByModelAndBrand_ReturnsLong() {
        // Given
        String carModel = CAR_MODEL + SAFE_DELETED_CAR_ID;
        String carBrand = CAR_BRAND + SAFE_DELETED_CAR_ID;
        Long expectedStatus = 1L;

        // When
        Long actualStatus = carRepository.existsSoftDeletedByModelAndBrand(carModel, carBrand);

        // Then
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }
}
