package com.github.ipantazi.carsharing.controller;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.service.car.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Car management", description = "Endpoints of managing cars")
@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {
    private final CarService carService;

    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new car.", description = "Add a new car.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarDto createCar(@RequestBody @Valid CarRequestDto carRequestDto) {
        return carService.save(carRequestDto);
    }

    @Operation(summary = "Get all cars.", description = "Get a list of all available cars.")
    @GetMapping
    public Page<CarDto> getAll(@ParameterObject Pageable pageable) {
        return carService.findAll(pageable);
    }

    @Operation(summary = "Get a car by id", description = "Get a car by id.")
    @GetMapping("/{id}")
    public CarDto getCarById(@PathVariable Long id) {
        return carService.getById(id);
    }

    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update a car", description = "Update a car by id.")
    @PutMapping("/{id}")
    public CarDto updateCar(@PathVariable Long id, @RequestBody @Valid UpdateCarDto carRequestDto) {
        return carService.update(id, carRequestDto);
    }

    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update car inventory", description = "Update car inventory by car id.")
    @PatchMapping("/{id}")
    public CarDto updateInventory(@PathVariable Long id,
                                  @RequestBody @Valid InventoryRequestDto requestDto) {
        return carService.manageInventory(id, requestDto);
    }

    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete a car", description = "Delete a car by id.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCar(@PathVariable Long id) {
        carService.delete(id);
    }
}
