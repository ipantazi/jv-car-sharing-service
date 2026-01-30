package com.github.ipantazi.carsharing.mapper;

import com.github.ipantazi.carsharing.config.MapperConfig;
import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.car.CarRequestDto;
import com.github.ipantazi.carsharing.dto.car.UpdateCarDto;
import com.github.ipantazi.carsharing.model.Car;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class)
public interface CarMapper {
    CarDto toCarDto(Car car);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "type", ignore = true)
    Car toCarEntity(CarRequestDto carRequestDto);

    @AfterMapping
    default void setType(@MappingTarget Car car, CarRequestDto carRequestDto) {
        car.setType(Car.Type.valueOfType(carRequestDto.type()));
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "inventory", ignore = true)
    void updateCarEntity(UpdateCarDto updateCarDto, @MappingTarget Car car);
}
