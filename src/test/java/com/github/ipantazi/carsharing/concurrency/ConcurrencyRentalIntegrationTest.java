package com.github.ipantazi.carsharing.concurrency;

import static com.github.ipantazi.carsharing.concurrency.ConcurrencyRentalIntegrationTest.Outcome.RENT_FAIL;
import static com.github.ipantazi.carsharing.concurrency.ConcurrencyRentalIntegrationTest.Outcome.RENT_OK;
import static com.github.ipantazi.carsharing.concurrency.ConcurrencyRentalIntegrationTest.Outcome.RETURN_FAIL;
import static com.github.ipantazi.carsharing.concurrency.ConcurrencyRentalIntegrationTest.Outcome.RETURN_OK;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RETURN_DATE_FOR_NEW_RENTAL;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestIdsList;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

public class ConcurrencyRentalIntegrationTest extends BaseConcurrencyIntegrationTest {
    private static RentalRequestDto dto;

    public enum Outcome {
        RENT_OK, RENT_FAIL, RETURN_OK, RETURN_FAIL
    }

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

        dto = new RentalRequestDto(RETURN_DATE_FOR_NEW_RENTAL, EXISTING_CAR_ID);
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

        List<Callable<Boolean>> tasks = getTasksNewRentalWithMultipleThreads(userIds);

        // When
        List<Future<Boolean>> results = new ConcurrencyTestHelper(threadCount)
                .runConcurrentTasks(tasks);

        // Then
        long successCount = results.stream()
                .map(ConcurrencyTestHelper::safeGet)
                .filter(Boolean.TRUE::equals)
                .count();

        assertThat(successCount)
                .as("Only one rental should succeed when inventory = 1")
                .isEqualTo(1);

        assertThat(rentalRepository.count())
                .as("Only one rental must be stored in DB")
                .isEqualTo(1);

        assertThat(getInventory())
                .as("The number of available cars should be zero")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("""
    Concurrent return and new rental:
    - return may happen first or second
    - inventory must never go negative
    - final state must be consistent
            """)
    @Sql(scripts = {
            "classpath:database/cars/set-inventory-null-for-car-id101.sql",
            "classpath:database/rentals/insert-one-test-rental.sql"
    },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void concurrentReturnThenNewRental_consistentFinalState() throws Exception {
        // Given
        List<Callable<Outcome>> tasks = List.of(
                // Thread 1: return first
                this::tryReturnRental,
                // Thread 2: new rental
                this::tryCreateRental
        );

        // When
        List<Outcome> outcomes = runAndCollectOutcomes(tasks);

        // Then
        int actualInventory = getInventory();
        long actualRentals = rentalRepository.count();

        assertInvariantState(actualInventory, actualRentals);

        // Case 1: return committed first → rent succeeds
        if (outcomes.contains(RETURN_OK) && outcomes.contains(RENT_OK)) {
            assertFinalState(actualInventory, 0, actualRentals, 2);
            return;
        }

        // Case 2: rent attempted first → rent failed
        if (outcomes.contains(RETURN_OK) && outcomes.contains(RENT_FAIL)) {
            assertFinalState(actualInventory, 1, actualRentals, 1);
            return;
        }

        failUnexpected(outcomes);
    }

    @Test
    @DisplayName("""
    Concurrent New rental and return:
    - new rental may happen first or second
    - inventory must never go negative
    - final state must be consistent
            """)
    @Sql(scripts = {
            "classpath:database/cars/set-inventory-null-for-car-id101.sql",
            "classpath:database/rentals/insert-one-test-rental.sql"
    },
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void concurrentNewRentalThenReturn_consistentFinalState() throws Exception {
        // Given
        List<Callable<Outcome>> tasks = List.of(
                // Thread 1: new rental first
                this::tryCreateRental,
                // Thread 2: return
                this::tryReturnRental
        );

        // When
        List<Outcome> outcomes = runAndCollectOutcomes(tasks);

        // Then
        int actualInventory = getInventory();
        long actualRentals = rentalRepository.count();

        assertInvariantState(actualInventory, actualRentals);

        // Case 1: rent attempted first → rent failed
        if (outcomes.contains(RETURN_OK) && outcomes.contains(RENT_FAIL)) {
            assertFinalState(actualInventory,1, actualRentals,1);
            return;
        }

        // Case 2: return committed first → rent succeeds
        if (outcomes.contains(RETURN_OK) && outcomes.contains(RENT_OK)) {
            assertFinalState(actualInventory,0, actualRentals,2);
            return;
        }

        failUnexpected(outcomes);
    }

    private int getInventory() {
        return carRepository.findById(EXISTING_CAR_ID)
                .orElseThrow()
                .getInventory();
    }

    private List<Callable<Boolean>> getTasksNewRentalWithMultipleThreads(List<Long> userIds) {
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

    private List<Outcome> runAndCollectOutcomes(List<Callable<Outcome>> tasks)
            throws InterruptedException {

        List<Future<Outcome>> results = new ConcurrencyTestHelper(tasks.size())
                .runConcurrentTasks(tasks);

        return results.stream()
                .map(ConcurrencyTestHelper::safeGet)
                .toList();
    }

    private Outcome tryCreateRental() {
        try {
            rentalService.createRental(EXISTING_ID_ANOTHER_USER, dto);
            return RENT_OK;
        } catch (Exception e) {
            return RENT_FAIL;
        }
    }

    private Outcome tryReturnRental() {
        try {
            rentalService.returnRental(EXISTING_USER_ID, EXISTING_RENTAL_ID);
            return RETURN_OK;
        } catch (Exception e) {
            return RETURN_FAIL;
        }
    }

    private void assertInvariantState(int actualInventory, long rentalCount) {
        assertThat(actualInventory)
                .as("Inventory must never be negative")
                .isGreaterThanOrEqualTo(0);

        assertThat(rentalCount)
                .as("Rental count must be either 1 or 2")
                .isIn(1L, 2L);
    }

    private void assertFinalState(int actualInventory,
                                  int expectedInventory,
                                  long actualRentals,
                                  long expectedRentals) {
        assertThat(actualInventory).isEqualTo(expectedInventory);
        assertThat(actualRentals).isEqualTo(expectedRentals);
    }

    private void failUnexpected(List<Outcome> outcomes) {
        fail("Unexpected concurrency outcome: " + outcomes);
    }
}
