package com.github.ipantazi.carsharing.service.car;

import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_DTO_IGNORING_FIELD;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.CAR_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NEW_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCar;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCarDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCarRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestInventoryRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUpdateCarDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.InventoryRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.exception.CarNotAvailableException;
import com.github.ipantazi.carsharing.exception.DataProcessingException;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.mapper.CarMapper;
import com.github.ipantazi.carsharing.model.Car;
import com.github.ipantazi.carsharing.repository.car.CarRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
public class CarServiceTest {
    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private CarServiceImpl carService;

    @Test
    @DisplayName("Test save() method with valid data")
    public void save_ValidData_ReturnsCarDto() {
        // Given
        Long existsStatus = 0L;
        CarDto expectedCarDto = createTestCarDto(NEW_CAR_ID);
        Car car = createTestCar(expectedCarDto);
        CarRequestDto carRequestDto = createTestCarRequestDto(expectedCarDto);

        when(carRepository.existsByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        )).thenReturn(false);
        when(carRepository.existsSoftDeletedByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        )).thenReturn(existsStatus);
        when(carMapper.toCarEntity(carRequestDto)).thenReturn(car);
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toCarDto(car)).thenReturn(expectedCarDto);

        // When
        CarDto actualCarDto = carService.save(carRequestDto);

        // Then
        assertObjectsAreEqualIgnoringFields(actualCarDto, expectedCarDto, CAR_DTO_IGNORING_FIELD);
        verify(carRepository, times(1)).existsByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        );
        verify(carRepository, times(1)).existsSoftDeletedByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        );
        verify(carMapper, times(1)).toCarEntity(carRequestDto);
        verify(carRepository, times(1)).save(car);
        verify(carMapper, times(1)).toCarDto(car);
        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("Test save() method with existing car")
    public void save_ExistingCar_ThrowsException() {
        // Given
        CarRequestDto carRequestDto = createTestCarRequestDto(createTestCarDto(EXISTING_CAR_ID));

        when(carRepository.existsByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        )).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> carService.save(carRequestDto))
                .isInstanceOf(DataProcessingException.class)
                .hasMessage("Car with model: " + carRequestDto.model() + " and brand: "
                        + carRequestDto.brand() + " already exists.");
        verify(carRepository, times(1)).existsByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        );
        verify(carRepository, never()).save(any());
        verifyNoMoreInteractions(carRepository);
        verifyNoInteractions(carMapper);
    }

    @Test
    @DisplayName("Test save() method with existing safe deleted car")
    public void save_ExistingSafeDeletedCar_ThrowsException() {
        // Given
        Long existsStatus = 1L;
        CarRequestDto carRequestDto = createTestCarRequestDto(createTestCarDto(EXISTING_CAR_ID));

        when(carRepository.existsByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        )).thenReturn(false);
        when(carRepository.existsSoftDeletedByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        )).thenReturn(existsStatus);

        // When & Then
        assertThatThrownBy(() -> carService.save(carRequestDto))
                .isInstanceOf(DataProcessingException.class)
                .hasMessage("Car with model: " + carRequestDto.model() + " and brand: "
                        + carRequestDto.brand() + " already exists, but was previously deleted.");
        verify(carRepository, times(1)).existsByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        );
        verify(carRepository, times(1)).existsSoftDeletedByModelAndBrand(
                carRequestDto.model(),
                carRequestDto.brand()
        );
        verify(carRepository, never()).save(any());
        verifyNoMoreInteractions(carRepository);
        verifyNoInteractions(carMapper);
    }

    @Test
    @DisplayName("Test getById() method with existing car")
    public void getById_ExistingCar_ReturnsCarDto() {
        // Given
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);
        Car car = createTestCar(expectedCarDto);

        when(carRepository.findById(EXISTING_CAR_ID)).thenReturn(Optional.of(car));
        when(carMapper.toCarDto(car)).thenReturn(expectedCarDto);

        // When
        CarDto actualCarDto = carService.getById(EXISTING_CAR_ID);

        // Then
        assertObjectsAreEqualIgnoringFields(actualCarDto, expectedCarDto, CAR_DTO_IGNORING_FIELD);
        verify(carRepository, times(1)).findById(EXISTING_CAR_ID);
        verify(carMapper, times(1)).toCarDto(car);
        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("Test getById() method with non-existing car")
    public void getById_NonExistingCar_ThrowsException() {
        // Given
        when(carRepository.findById(NOT_EXISTING_CAR_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> carService.getById(NOT_EXISTING_CAR_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Car not found with id: " + NOT_EXISTING_CAR_ID);
        verify(carRepository, times(1)).findById(NOT_EXISTING_CAR_ID);
        verifyNoMoreInteractions(carRepository);
        verifyNoInteractions(carMapper);
    }

    @Test
    @DisplayName("Test update() method with existing car")
    public void update_ExistingCar_ReturnsCarDto() {
        // Given
        Long existsStatus = 0L;
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);
        Car car = createTestCar(expectedCarDto);
        UpdateCarDto updateCarDto = createTestUpdateCarDto(expectedCarDto);
        String brand = updateCarDto.brand();
        String model = updateCarDto.model();

        when(carRepository.findById(EXISTING_CAR_ID)).thenReturn(Optional.of(car));
        when(carRepository.existsByModelAndBrand(model, brand)).thenReturn(false);
        when(carRepository.existsSoftDeletedByModelAndBrand(model, brand)).thenReturn(existsStatus);
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toCarDto(car)).thenReturn(expectedCarDto);

        // When
        CarDto actualCarDto = carService.update(EXISTING_CAR_ID, updateCarDto);

        // Then
        assertObjectsAreEqualIgnoringFields(actualCarDto, expectedCarDto, CAR_DTO_IGNORING_FIELD);
        verify(carRepository, times(1)).findById(EXISTING_CAR_ID);
        verify(carRepository, times(1)).existsByModelAndBrand(model, brand);
        verify(carRepository, times(1)).existsSoftDeletedByModelAndBrand(model, brand);
        verify(carMapper, times(1)).updateCarEntity(updateCarDto, car);
        verify(carRepository, times(1)).save(car);
        verify(carMapper, times(1)).toCarDto(car);
        verifyNoMoreInteractions(carRepository, carMapper);
    }

    @Test
    @DisplayName("Test method update() with an existing car with the given model and brand")
    public void update_ExistingCarWithSameModelAndBrand_ThrowsException() {
        // Given
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);
        Car car = createTestCar(expectedCarDto);
        UpdateCarDto updateCarDto = createTestUpdateCarDto(expectedCarDto);
        String brand = updateCarDto.brand();
        String model = updateCarDto.model();

        when(carRepository.findById(EXISTING_CAR_ID)).thenReturn(Optional.of(car));
        when(carRepository.existsByModelAndBrand(model, brand)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> carService.update(EXISTING_CAR_ID, updateCarDto))
                .isInstanceOf(DataProcessingException.class)
                .hasMessage("Car with model: " + updateCarDto.model() + " and brand: "
                        + updateCarDto.brand() + " already exists.");
        verify(carRepository, times(1)).findById(EXISTING_CAR_ID);
        verify(carRepository, times(1)).existsByModelAndBrand(model, brand);
        verify(carRepository, never()).save(any());
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName(
            "Test method update() with an existing soft deleted car with the given model and brand"
    )
    public void update_ExistingCarSoftDeletedWithSameModelAndBrand_ThrowsException() {
        // Given
        Long existsStatus = 1L;
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);
        Car car = createTestCar(expectedCarDto);
        UpdateCarDto updateCarDto = createTestUpdateCarDto(expectedCarDto);
        String brand = updateCarDto.brand();
        String model = updateCarDto.model();

        when(carRepository.findById(EXISTING_CAR_ID)).thenReturn(Optional.of(car));
        when(carRepository.existsByModelAndBrand(model, brand)).thenReturn(false);
        when(carRepository.existsSoftDeletedByModelAndBrand(model, brand)).thenReturn(existsStatus);

        // When & Then
        assertThatThrownBy(() -> carService.update(EXISTING_CAR_ID, updateCarDto))
                .isInstanceOf(DataProcessingException.class)
                .hasMessage("Car with model: " + updateCarDto.model() + " and brand: "
                        + updateCarDto.brand() + " already exists, but was previously deleted.");
        verify(carRepository, times(1)).findById(EXISTING_CAR_ID);
        verify(carRepository, times(1)).existsByModelAndBrand(model, brand);
        verify(carRepository, times(1)).existsSoftDeletedByModelAndBrand(model, brand);
        verify(carRepository, never()).save(any());
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test update() method with non-existing car")
    public void update_NonExistingCar_ThrowsException() {
        // Given
        UpdateCarDto updateCarDto = createTestUpdateCarDto(createTestCarDto(NOT_EXISTING_CAR_ID));

        when(carRepository.findById(NOT_EXISTING_CAR_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> carService.update(NOT_EXISTING_CAR_ID, updateCarDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Car not found with id: " + NOT_EXISTING_CAR_ID);
        verify(carRepository, times(1)).findById(NOT_EXISTING_CAR_ID);
        verify(carRepository, never()).save(any());
        verifyNoMoreInteractions(carRepository);
        verifyNoInteractions(carMapper);
    }

    @Test
    @DisplayName("Test delete() method with existing car.")
    public void delete_ExistingCar_SafeDeleteBook() {
        // Given
        when(carRepository.existsById(EXISTING_CAR_ID)).thenReturn(true);

        // When
        carService.delete(EXISTING_CAR_ID);

        // Then
        verify(carRepository, times(1)).existsById(EXISTING_CAR_ID);
        verify(carRepository, times(1)).deleteById(EXISTING_CAR_ID);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test delete() method with non-existing car.")
    public void delete_NonExistingCar_ThrowsException() {
        // Given
        when(carRepository.existsById(NOT_EXISTING_CAR_ID)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> carService.delete(NOT_EXISTING_CAR_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Car not found with id: " + NOT_EXISTING_CAR_ID);
        verify(carRepository, times(1)).existsById(NOT_EXISTING_CAR_ID);
        verify(carRepository, never()).deleteById(NOT_EXISTING_CAR_ID);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test findAll() method with valid request.")
    public void findAll_ValidRequest_ReturnsAllCarDtos() {
        // Given
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);
        Car car = createTestCar(expectedCarDto);
        List<Car> cars = Collections.singletonList(car);
        Page<Car> carPage = new PageImpl<>(cars, CAR_PAGEABLE, cars.size());
        when(carRepository.findAll(CAR_PAGEABLE)).thenReturn(carPage);
        when(carMapper.toCarDto(car)).thenReturn(expectedCarDto);

        // When
        Page<CarDto> actualCarDtoPage = carService.findAll(CAR_PAGEABLE);

        // Then
        assertPageMetadataEquals(actualCarDtoPage, carPage);
        List<CarDto> actualCarDtoList = actualCarDtoPage.getContent();
        assertThat(actualCarDtoList).hasSize(1).containsExactly(expectedCarDto);
        assertObjectsAreEqualIgnoringFields(
                actualCarDtoList.get(0),
                expectedCarDto,
                CAR_DTO_IGNORING_FIELD
        );
    }

    @Test
    @DisplayName("Test findAll() method with empty page request.")
    public void findAll_NoCars_ReturnsEmptyPage() {
        // Given
        Page<Car> carPage = Page.empty(CAR_PAGEABLE);
        when(carRepository.findAll(CAR_PAGEABLE)).thenReturn(carPage);

        // When
        Page<CarDto> actualCarDtoPage = carService.findAll(CAR_PAGEABLE);

        // Then
        assertPageMetadataEquals(actualCarDtoPage, carPage);
        assertThat(actualCarDtoPage.getContent()).isEmpty();
        verify(carRepository, times(1)).findAll(CAR_PAGEABLE);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test operation SET manageInventory() method with valid request.")
    public void manageInventory_SetOperation_ReturnsCarDto() {
        // Given
        int inventoryBeforeUpdate = 10;
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);
        Car car = createTestCar(expectedCarDto);
        car.setInventory(inventoryBeforeUpdate);
        InventoryRequestDto inventoryRequestDto = createTestInventoryRequestDto(
                expectedCarDto.getInventory(),
                OperationType.SET
        );

        when(inventoryService.adjustInventory(
                EXISTING_CAR_ID,
                inventoryRequestDto.getInventory(),
                inventoryRequestDto.getOperation()
        )).thenReturn(car);
        when(carMapper.toCarDto(car)).thenReturn(expectedCarDto);

        // When
        CarDto actualCarDto = carService.manageInventory(EXISTING_CAR_ID, inventoryRequestDto);

        // Then
        assertObjectsAreEqualIgnoringFields(actualCarDto, expectedCarDto, CAR_DTO_IGNORING_FIELD);
        assertThat(actualCarDto.getInventory()).isNotEqualTo(inventoryBeforeUpdate);
        verify(inventoryService, times(1)).adjustInventory(
                EXISTING_CAR_ID,
                inventoryRequestDto.getInventory(),
                inventoryRequestDto.getOperation()
        );
        verify(carMapper, times(1)).toCarDto(car);
        verifyNoMoreInteractions(inventoryService, carMapper);
    }

    @Test
    @DisplayName("Test validateCarAvailableForRental() method when the car is available")
    void validateCarAvailableForRental_CarAvailable_CheckSuccess() {
        // Given
        Car car = createTestCar(EXISTING_CAR_ID);
        when(carRepository.findById(EXISTING_CAR_ID)).thenReturn(Optional.of(car));

        // When
        carService.validateCarAvailableForRental(EXISTING_CAR_ID);

        // Then
        verify(carRepository, times(1)).findById(EXISTING_CAR_ID);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test validateCarAvailableForRental() method when the car is not available")
    void validateCarAvailableForRental_CarNotAvailable_ThrowsException() {
        // Given
        Car car = createTestCar(EXISTING_CAR_ID);
        car.setInventory(0);
        when(carRepository.findById(EXISTING_CAR_ID)).thenReturn(Optional.of(car));

        // When & Then
        assertThatThrownBy(() -> carService.validateCarAvailableForRental(EXISTING_CAR_ID))
                .isInstanceOf(CarNotAvailableException.class)
                .hasMessage("Car with id: " + EXISTING_CAR_ID + " is not available.");
        verify(carRepository, times(1)).findById(EXISTING_CAR_ID);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test validateCarAvailableForRental() method when the car does not exist")
    void validateCarAvailableForRental_CarDoesNotExist_ThrowsException() {
        // Given
        when(carRepository.findById(NOT_EXISTING_CAR_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> carService.validateCarAvailableForRental(NOT_EXISTING_CAR_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Car not found with id: " + NOT_EXISTING_CAR_ID);
        verify(carRepository, times(1)).findById(NOT_EXISTING_CAR_ID);
        verifyNoMoreInteractions(carRepository);
    }

    @Test
    @DisplayName("Test getByIds() method")
    void getByIds_ReturnsCarDtos() {
        // Given
        CarDto expectedCarDto = createTestCarDto(EXISTING_CAR_ID);
        Car car = createTestCar(expectedCarDto);
        Set<Long> carIds = Set.of(EXISTING_CAR_ID);
        List<Car> cars = Collections.singletonList(car);
        when(carRepository.findAllById(carIds)).thenReturn(cars);
        when(carMapper.toCarDto(car)).thenReturn(expectedCarDto);

        // When
        List<CarDto> actualCarDtos = carService.getByIds(carIds);

        // Then
        assertThat(actualCarDtos).hasSize(1);
        assertObjectsAreEqualIgnoringFields(
                actualCarDtos.get(0),
                expectedCarDto,
                CAR_IGNORING_FIELDS
        );
        verify(carRepository, times(1)).findAllById(carIds);
        verify(carMapper, times(1)).toCarDto(car);
        verifyNoMoreInteractions(carRepository, carMapper);
    }
}
