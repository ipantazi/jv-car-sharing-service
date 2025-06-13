package com.github.ipantazi.carsharing.service.car;

import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.model.Car;

public interface InventoryService {
    Car adjustInventory(Long carId, int quantity, OperationType operation);
}
