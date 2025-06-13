/*package com.github.ipantazi.carsharing.service.car;

import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL")
public class ConcurrencyTest {
    @Autowired
    private CarService carService;

    @Autowired
    private CarRepository carRepository;

    private Long carId;

    @BeforeEach
    void setup() {
        Car car = createTestCar(EXISTING_CAR_ID);
        car.setInventory(1);
        carId = carRepository.save(car).getId();
    }

    @Test
    void shouldAllowOnlyOneThreadToDecreaseInventory() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        carService.adjustInventory(carId, 1, OperationType.DECREASE);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        System.out.println("Thread failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertTrue(completed, "Test timed out waiting for threads to finish");
        }

        assertEquals(1, successCount.get(), "Exactly one thread should succeed");
        assertEquals(1, failureCount.get(), "One thread should fail due to inventory");
    }
}*/
