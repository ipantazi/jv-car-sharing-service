package com.github.ipantazi.carsharing.concurrency;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.controller.DatabaseTestUtil.executeSqlScript;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.ipantazi.carsharing.config.BaseConcurrencyIntegrationTest;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import com.github.ipantazi.carsharing.service.car.InventoryService;
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
import org.springframework.beans.factory.annotation.Autowired;

public class ConcurrentInventoryIntegrationTest extends BaseConcurrencyIntegrationTest {
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CarRepository carRepository;

    @BeforeAll
    public static void beforeAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
        executeSqlScript(dataSource, "database/cars/insert-test-cars.sql");
    }

    @AfterEach
    public void afterEach(@Autowired DataSource dataSource) {
        executeSqlScript(dataSource, "database/cars/restoring-car-id101.sql");
    }

    @AfterAll
    public static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    public static void teardown(DataSource dataSource) {
        executeSqlScript(dataSource, "database/cars/clear-all-cars.sql");
    }

    @RepeatedTest(5)
    @DisplayName("Concurrent +1 and -1 inventory adjustments must not cause lost updates "
            + "(inventory remains consistent)")
    void inventoryAdjustmentsConcurrent_shouldNotLoseUpdates() throws Exception {
        // Given
        int threadCount = 2;
        ConcurrencyTestHelper helper = new ConcurrencyTestHelper(threadCount);
        int inventoryBeforeTest = carRepository.findById(EXISTING_CAR_ID)
                .orElseThrow()
                .getInventory();
        assertThat(inventoryBeforeTest).isGreaterThanOrEqualTo(1);

        List<Callable<String>> tasks = List.of(
                () -> {
                    try {
                        inventoryService.adjustInventory(
                                EXISTING_CAR_ID,
                                1,
                                OperationType.INCREASE
                        );
                        return "INCREASE_OK";
                    } catch (Exception e) {
                        return "INCREASE_FAIL:" + e.getMessage();
                    }
                },
                () -> {
                    try {
                        inventoryService.adjustInventory(
                                EXISTING_CAR_ID,
                                1,
                                OperationType.DECREASE
                        );
                        return "DECREASE_OK";
                    } catch (Exception e) {
                        return "DECREASE_FAIL:" + e.getMessage();
                    }
                }
        );

        // When
        List<Future<String>> results = helper.runConcurrentTasks(tasks);

        // Then
        String increaseResult = ConcurrencyTestHelper.safeGet(results.get(0));
        String decreaseResult = ConcurrencyTestHelper.safeGet(results.get(1));

        assertThat(increaseResult).isEqualTo("INCREASE_OK");
        assertThat(decreaseResult).isEqualTo("DECREASE_OK");

        int inventoryAfterTest = carRepository.findById(EXISTING_CAR_ID)
                .orElseThrow()
                .getInventory();
        assertThat(inventoryAfterTest).isEqualTo(inventoryBeforeTest);
        assertThat(inventoryAfterTest).isGreaterThanOrEqualTo(0);
    }
}
