package com.github.ipantazi.carsharing.service.car;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.exception.DataProcessingException;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.mapper.CarMapper;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;

    @Override
    public CarDto save(CarRequestDto carRequestDto) {
        validateCarDoesNotExist(carRequestDto.model(), carRequestDto.brand());
        Car car = carMapper.toCarEntity(carRequestDto);
        return carMapper.toCarDto(carRepository.save(car));
    }

    @Override
    public CarDto getById(Long id) {
        return carMapper.toCarDto(findCarById(id));
    }

    @Override
    public CarDto update(Long id, UpdateCarDto updateCarDto) {
        Car car = findCarById(id);
        validateCarDoesNotExist(updateCarDto.model(), updateCarDto.brand());
        carMapper.updateCarEntity(updateCarDto, car);
        return carMapper.toCarDto(carRepository.save(car));
    }

    @Override
    public CarDto manageInventory(Long id, InventoryRequestDto requestDto) {
        Car car = findCarById(id);
        int quantity = requestDto.getInventory();
        if (quantity <= 0) {
            throw new IllegalArgumentException("Invalid quantity. Quantity should be positive.");
        }

        switch (requestDto.getOperation()) {
            case INCREASE -> car.setInventory(car.getInventory() + quantity);
            case DECREASE -> {
                if (car.getInventory() < quantity) {
                    throw new IllegalArgumentException("Not enough inventory to decrease.");
                }
                car.setInventory(car.getInventory() - quantity);
            }
            case SET -> car.setInventory(quantity);
            default -> throw new UnsupportedOperationException("Unsupported operation: "
                    + requestDto.getOperation());
        }
        carRepository.save(car);
        return carMapper.toCarDto(car);
    }

    @Override
    public void delete(Long id) {
        if (!carRepository.existsById(id)) {
            throw new EntityNotFoundException("Car not found with id: " + id);
        }
        carRepository.deleteById(id);
    }

    @Override
    public Page<CarDto> findAll(Pageable pageable) {
        return carRepository.findAll(pageable)
                .map(carMapper::toCarDto);
    }

    @Override
    public Car findCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + id));
    }

    private void validateCarDoesNotExist(String model, String brand) {
        if (carRepository.existsByModelAndBrand(model, brand)) {
            throw new DataProcessingException("Car with model: " + model + " and brand: " + brand
                    + " already exists.");
        }

        if (carRepository.existsSoftDeletedByModelAndBrand(model, brand) == 1L) {
            throw new DataProcessingException("Car with model: " + model + " and brand: " + brand
                    + " already exists, but was previously deleted.");
        }
    }
}
