package com.github.ipantazi.carsharing.mapper;

import com.github.ipantazi.carsharing.config.MapperConfig;
import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.model.Car;
import java.math.RoundingMode;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class)
public interface CarMapper {
    @Mapping(target = "dailyFee", ignore = true)
    CarDto toCarDto(Car car);

    @AfterMapping
    default void formatDailyFeeDto(@MappingTarget CarDto carDto, Car car) {
        if (car.getDailyFee() != null) {
            carDto.setDailyFee(car.getDailyFee().setScale(1, RoundingMode.HALF_UP));
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "dailyFee", ignore = true)
    Car toCarEntity(CarRequestDto carRequestDto);

    @AfterMapping
    default void formatDailyFeeEntity(@MappingTarget Car car, CarRequestDto carRequestDto) {
        if (carRequestDto.dailyFee() != null) {
            car.setDailyFee(carRequestDto.dailyFee().setScale(1, RoundingMode.HALF_UP));
        }
    }

    @AfterMapping
    default void setType(@MappingTarget Car car, CarRequestDto carRequestDto) {
        car.setType(Car.Type.valueOfType(carRequestDto.type()));
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    @Mapping(target = "dailyFee", ignore = true)
    void updateCarEntity(UpdateCarDto updateCarDto, @MappingTarget Car car);

    @AfterMapping
    default void formatDailyFee(@MappingTarget Car car, UpdateCarDto updateCarDto) {
        if (updateCarDto.dailyFee() != null) {
            car.setDailyFee(updateCarDto.dailyFee().setScale(1, RoundingMode.HALF_UP));
        }
    }
}
