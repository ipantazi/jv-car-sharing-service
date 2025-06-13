package com.github.ipantazi.carsharing.repository.car;

import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_BRAND;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_MODEL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SAFE_DELETED_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCar;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.ipantazi.carsharing.model.Car;
import jakarta.persistence.EntityManager;
import java.math.RoundingMode;
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
    @Autowired
    private EntityManager entityManager;

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
    @DisplayName("Test findByIdForUpdate() method works.")
    void findByIdForUpdate_ExistingCar_ReturnsCarOptional() {
        // Given
        Car expectedCar = createTestCar(EXISTING_CAR_ID);
        expectedCar.setDailyFee(expectedCar.getDailyFee().setScale(2, RoundingMode.HALF_UP));

        // When
        Optional<Car> actualCarOpt = carRepository.findByIdForUpdate(EXISTING_CAR_ID);

        // Then
        assertThat(actualCarOpt).isPresent();
        Car actualCar = actualCarOpt.get();
        assertObjectsAreEqualIgnoringFields(actualCarOpt.get(), expectedCar, CAR_IGNORING_FIELDS);
    }

    @Test
    @DisplayName("Test findByIdForUpdate() method works.")
    void findByIdForUpdate_NotExistingCar_ReturnsEmptyOptional() {
        // When
        Optional<Car> actualCarOpt = carRepository.findByIdForUpdate(NOT_EXISTING_CAR_ID);

        // Then
        assertThat(actualCarOpt).isEmpty();
    }
    /*
    @Test
    @DisplayName("Should lock the car row with PESSIMISTIC_WRITE")
    @Sql(scripts = {
            "classpath:database/cars/clear-all-cars.sql",
            "classpath:database/cars/insert-test-cars.sql"
    },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void shouldLockCarWithPessimisticWrite() throws Exception {
        // Prepare
        Car car = createTestCar(NEW_CAR_ID);
        car.setId(null);
        entityManager.persist(car);
        entityManager.flush();
        entityManager.clear();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<Void> future = executor.submit(() -> {
            TransactionTemplate txTemplate = new TransactionTemplate(
                    new JpaTransactionManager(entityManager.getEntityManagerFactory())
            );
            txTemplate.execute(status -> {
                Optional<Car> lockedCar = carRepository.findByIdForUpdate(car.getId());
                assertThat(lockedCar).isPresent();
                System.out.println("Thread 1 locked the car");
                latch.countDown();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Thread was interrupted", e);
                }

                return null;
            });
            return null;
        });

        latch.await();

        Future<Void> second = executor.submit(() -> {
            TransactionTemplate txTemplate = new TransactionTemplate(
                    new JpaTransactionManager(entityManager.getEntityManagerFactory())
            );
            txTemplate.setTimeout(5);
            txTemplate.execute(status -> {
                Optional<Car> lockedCar = carRepository.findByIdForUpdate(car.getId());
                assertThat(lockedCar).isPresent();
                System.out.println("Thread 2 also acquired lock after wait");
                return null;
            });
            return null;
        });

        future.get();
        second.get();
        executor.shutdown();
    }*/
}
