package com.github.ipantazi.carsharing.service.car;

import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final CarRepository carRepository;

    @Override
    @Transactional
    public Car adjustInventory(Long carId, int quantity, OperationType operation) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Invalid quantity. Quantity should be positive.");
        }
        Car car = carRepository.findByIdForUpdate(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + carId));

        switch (operation) {
            case INCREASE -> car.setInventory(car.getInventory() + quantity);
            case DECREASE -> {
                if (car.getInventory() < quantity) {
                    throw new IllegalArgumentException("Not enough inventory to decrease.");
                }
                car.setInventory(car.getInventory() - quantity);
            }
            case SET -> car.setInventory(quantity);
            default -> throw new UnsupportedOperationException("Unsupported operation: "
                    + operation);
        }
        return carRepository.save(car);
    }
}
