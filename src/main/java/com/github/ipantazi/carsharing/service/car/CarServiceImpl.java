package com.github.ipantazi.carsharing.service.car;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.exception.CarNotAvailableException;
import com.github.ipantazi.carsharing.exception.DataProcessingException;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.mapper.CarMapper;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;
    private final InventoryService inventoryService;

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
    @Transactional
    public CarDto manageInventory(Long id, InventoryRequestDto requestDto) {
        Car car = inventoryService.adjustInventory(
                id,
                requestDto.getInventory(),
                requestDto.getOperation()
        );
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
        return carRepository.findAll(pageable).map(carMapper::toCarDto);
    }

    @Override
    public Car findCarById(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Car not found with id: " + id));
    }

    @Override
    public void validateCarAvailableForRental(Long carId) {
        if (!carRepository.existsCarByIdAndInventoryIsGreaterThan(carId, 0)) {
            throw new CarNotAvailableException("Car with id: %d is not available for rental."
                    .formatted(carId));
        }
    }

    @Override
    public List<CarDto> getByIds(Set<Long> ids) { // Test
        return carRepository.findAllById(ids).stream()
                .map(carMapper::toCarDto)
                .toList();
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
