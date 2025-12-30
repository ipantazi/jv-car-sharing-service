/*
package com.github.ipantazi.carsharing.service.car;

import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_INVENTORY;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {
    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    @DisplayName("Test operation SET adjustInventory() method with valid request.")
    public void adjustInventory_SetOperation_ReturnsCarDto() {
        // Given
        int newInventory = 10;
        Car car = createTestCar(EXISTING_USER_ID);

        when(carRepository.findByIdForUpdate(car.getId())).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);

        // When
        Car actualCar = inventoryService.adjustInventory(
                car.getId(),
                newInventory,
                OperationType.SET
        );

        // Then
        assertThat(actualCar.getInventory()).isEqualTo(newInventory);
        verify(carRepository, times(1)).findByIdForUpdate(car.getId());
        verify(carRepository, times(1)).save(car);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test operation INCREASE adjustInventory() method with valid request.")
    public void adjustInventory_IncreaseOperation_ReturnsCarDto() {
        // Given
        int inventoryFromRequest = 3;
        Car car = createTestCar(EXISTING_USER_ID);
        int oldInventory = car.getInventory();

        when(carRepository.findByIdForUpdate(car.getId())).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);

        // When
        Car actualCar = inventoryService.adjustInventory(
                car.getId(),
                inventoryFromRequest,
                OperationType.INCREASE
        );

        // Then
        assertThat(actualCar.getInventory())
                .isEqualTo(oldInventory + inventoryFromRequest);
        verify(carRepository, times(1)).findByIdForUpdate(car.getId());
        verify(carRepository, times(1)).save(car);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test operation DECREASE adjustInventory() method with valid request.")
    public void adjustInventory_DecreaseOperation_ReturnsCarDto() {
        // Given
        int inventoryFromRequest = 3;
        Car car = createTestCar(EXISTING_USER_ID);
        int oldInventory = car.getInventory();

        when(carRepository.findByIdForUpdate(car.getId())).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);

        // When
        Car actualCar = inventoryService.adjustInventory(
                car.getId(),
                inventoryFromRequest,
                OperationType.DECREASE
        );

        // Then
        assertThat(actualCar.getInventory())
                .isEqualTo(oldInventory - inventoryFromRequest);
        verify(carRepository, times(1)).findByIdForUpdate(car.getId());
        verify(carRepository, times(1)).save(car);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test adjustInventory() method when not enough inventory to decrease.")
    public void adjustInventory_NotEnoughInventoryToDecrease_ThrowsException() {
        // Given
        int inventoryFromRequest = 10;
        Car car = createTestCar(EXISTING_USER_ID);

        when(carRepository.findByIdForUpdate(car.getId())).thenReturn(Optional.of(car));

        // When & Then
        assertThatThrownBy(() -> inventoryService.adjustInventory(
                        car.getId(),
                        inventoryFromRequest,
                        OperationType.DECREASE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not enough inventory to decrease.");
        verify(carRepository, times(1)).findByIdForUpdate(car.getId());
        verify(carRepository, never()).save(any());
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test adjustInventory() method with invalid car id.")
    public void adjustInventory_InvalidCarId_ThrowsException() {
        // Given
        when(carRepository.findByIdForUpdate(NOT_EXISTING_CAR_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.adjustInventory(
                NOT_EXISTING_CAR_ID,
                CAR_INVENTORY,
                OperationType.SET
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Car not found with id: " + NOT_EXISTING_CAR_ID);
        verify(carRepository, times(1)).findByIdForUpdate(NOT_EXISTING_CAR_ID);
        verify(carRepository, never()).save(any());
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test adjustInventory() method with negative inventory value.")
    public void adjustInventory_NegativeInventoryValue_ThrowsException() {
        // Given
        int negativeInventoryValue = -1;

        //When
        assertThatThrownBy(() -> inventoryService.adjustInventory(
                EXISTING_CAR_ID,
                negativeInventoryValue,
                OperationType.SET
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid quantity. Quantity should be positive.");

        // Then
        verify(carRepository, never()).findByIdForUpdate(any());
        verify(carRepository, never()).save(any());
        verifyNoInteractions(carRepository);
    }
}

 */
