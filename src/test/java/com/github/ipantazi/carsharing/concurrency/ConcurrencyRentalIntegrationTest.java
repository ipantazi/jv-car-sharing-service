package com.github.ipantazi.carsharing.concurrency;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RETURN_DATE_FOR_NEW_RENTAL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestIdsList;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.ipantazi.carsharing.config.BaseConcurrencyIntegrationTest;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.service.rental.RentalService;
import com.github.ipantazi.carsharing.util.concurrency.ConcurrencyTestHelper;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

public class ConcurrencyRentalIntegrationTest extends BaseConcurrencyIntegrationTest {
    @Autowired
    private RentalService rentalService;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @BeforeAll
    public static void beforeAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
        executeSqlScript(
                dataSource,
                "database/users/insert-test-users.sql",
                "database/cars/insert-test-cars.sql");
    }

    @AfterEach
    public void afterEach(@Autowired DataSource dataSource) {
        executeSqlScript(
                dataSource,
                "database/rentals/clear-all-rentals.sql",
                "database/cars/restoring-car-id101.sql");
    }

    @AfterAll
    public static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    public static void teardown(DataSource dataSource) {
        executeSqlScript(
                dataSource,
                "database/rentals/clear-all-rentals.sql",
                "database/cars/clear-all-cars.sql",
                "database/users/clear-all-users.sql"
        );
    }

    @Test
    @DisplayName("Should allow only one rental when 5 threads try to rent the same car")
    @Sql(scripts = "classpath:database/cars/set-inventory-one-for-car-id101.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldAllowOnlyOneRentalWhenFiveThreadsTryToRentSameCar() throws InterruptedException {
        // Given
        int threadCount = 5;
        List<Long> userIds = createTestIdsList(threadCount);

        RentalRequestDto dto = new RentalRequestDto(RETURN_DATE_FOR_NEW_RENTAL, EXISTING_CAR_ID);
        List<Callable<Boolean>> tasks = getTasksNewRentalWithMultipleThreads(userIds, dto);
        ConcurrencyTestHelper concurrencyTestHelper = new ConcurrencyTestHelper(threadCount);

        // When
        List<Future<Boolean>> results = concurrencyTestHelper.runConcurrentTasks(tasks);

        // Then
        long successCount = results.stream()
                .map(ConcurrencyTestHelper::safeGet)
                .filter(v -> v)
                .count();
        assertThat(successCount)
                .as("Only one rental should succeed when inventory = 1")
                .isEqualTo(1);

        assertThat(rentalRepository.count())
                .as("Only one rental must be stored in DB")
                .isEqualTo(1);

        int actualInventory = carRepository.findById(EXISTING_CAR_ID)
                .orElseThrow()
                .getInventory();
        assertThat(actualInventory)
                .as("The number of available cars should be zero")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("Return rental completes first → new rental must succeed")
    @Sql(scripts = {
            "classpath:database/cars/set-inventory-null-for-car-id101.sql",
            "classpath:database/rentals/insert-one-test-rental.sql"
    },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void returnRentalFirst_thenNewRentalSucceeds() throws Exception {
        // Given
        int threadCount = 2;
        RentalRequestDto dto = new RentalRequestDto(RETURN_DATE_FOR_NEW_RENTAL, EXISTING_CAR_ID);
        List<Callable<String>> tasks = getTasksReturnFirstThenNewRental(dto);
        ConcurrencyTestHelper concurrencyTestHelper = new ConcurrencyTestHelper(threadCount);

        // When
        List<Future<String>> results = concurrencyTestHelper.runConcurrentTasks(tasks);

        // Then
        String returnResult = ConcurrencyTestHelper.safeGet(results.get(0));
        String rentResult = ConcurrencyTestHelper.safeGet(results.get(1));

        assertThat(returnResult).isEqualTo("RETURN_OK");
        assertThat(rentResult).isEqualTo("RENT_OK");

        int actualInventory = carRepository.findById(EXISTING_CAR_ID)
                .orElseThrow()
                .getInventory();
        assertThat(actualInventory)
                .as("The number of available cars should be zero")
                .isEqualTo(0);

        assertThat(rentalRepository.count())
                .as("Returned rental plus new rental must be stored in DB")
                .isEqualTo(2);
    }

    @RepeatedTest(5)
    @DisplayName("New rental attempted first before the rent is return → new rental may fail; "
            + "inventory must not go negative")
    @Sql(scripts = {
            "classpath:database/cars/set-inventory-null-for-car-id101.sql",
            "classpath:database/rentals/insert-one-test-rental.sql"
    },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void rentFirst_thenReturn() throws Exception {
        // Given
        int threadCount = 2;
        RentalRequestDto dto = new RentalRequestDto(RETURN_DATE_FOR_NEW_RENTAL, EXISTING_CAR_ID);
        List<Callable<String>> tasks = getTasksNewRentalThenReturn(dto);
        ConcurrencyTestHelper concurrencyTestHelper = new ConcurrencyTestHelper(threadCount);

        // When
        List<Future<String>> results = concurrencyTestHelper.runConcurrentTasks(tasks);

        // Then
        String rentResult = ConcurrencyTestHelper.safeGet(results.get(0));
        String returnResult = ConcurrencyTestHelper.safeGet(results.get(1));

        assertThat(returnResult).startsWith("RETURN_");
        assertThat(rentResult).startsWith("RENT_");

        int actualInventory = carRepository.findById(EXISTING_CAR_ID)
                .orElseThrow()
                .getInventory();
        assertThat(actualInventory)
                .as("The number of available cars should be greater than or equal to zero")
                .isGreaterThanOrEqualTo(0);

        if ("RENT_OK".equals(rentResult)) {
            // Rent succeeded only if return committed first
            assertThat(rentalRepository.count()).isEqualTo(2);
            assertThat(actualInventory).isEqualTo(0);
        } else {
            // Rent failed
            assertThat(rentalRepository.count()).isEqualTo(1);
            assertThat(actualInventory).isEqualTo(1);
        }
    }

    private List<Callable<Boolean>> getTasksNewRentalWithMultipleThreads(List<Long> userIds,
                                                                         RentalRequestDto dto) {
        return userIds.stream()
                .map(userId -> (Callable<Boolean>) () -> {
                    try {
                        rentalService.createRental(userId, dto);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    private List<Callable<String>> getTasksReturnFirstThenNewRental(RentalRequestDto dto) {
        return List.of(
                // Thread 1: return first
                () -> {
                    rentalService.returnRental(EXISTING_USER_ID, EXISTING_RENTAL_ID);
                    return "RETURN_OK";
                },
                // Thread 2: new rental
                () -> {
                    rentalService.createRental(EXISTING_ID_ANOTHER_USER, dto);
                    return "RENT_OK";
                }
        );
    }

    private List<Callable<String>> getTasksNewRentalThenReturn(RentalRequestDto dto) {
        return List.of(
                // Thread 1: new rental first
                () -> {
                    try {
                        rentalService.createRental(EXISTING_ID_ANOTHER_USER, dto);
                        return "RENT_OK";
                    } catch (Exception e) {
                        return "RENT_FAIL";
                    }
                },
                // Thread 2: return
                () -> {
                    try {
                        rentalService.returnRental(EXISTING_USER_ID, EXISTING_RENTAL_ID);
                        return "RETURN_OK";
                    } catch (Exception e) {
                        return "RETURN_FAIL";
                    }
                }
        );
    }
}
