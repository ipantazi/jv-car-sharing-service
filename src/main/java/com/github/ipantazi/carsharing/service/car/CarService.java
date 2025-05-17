package com.github.ipantazi.carsharing.service.car;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.model.Car;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService {
    CarDto save(CarRequestDto carRequestDto);

    CarDto getById(Long id);

    CarDto update(Long id, UpdateCarDto updateCarDto);

    CarDto manageInventory(Long id, InventoryRequestDto inventoryRequestDto);

    void delete(Long id);

    Page<CarDto> findAll(Pageable pageable);

    Car findCarById(Long id);
}
