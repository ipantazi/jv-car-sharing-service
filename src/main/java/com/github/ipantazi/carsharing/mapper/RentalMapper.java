package com.github.ipantazi.carsharing.mapper;

import com.github.ipantazi.carsharing.config.MapperConfig;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface RentalMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rentalDate", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Rental toRentalEntity(RentalRequestDto rentalRequestDto);

    @Mapping(target = "baseRentalCost", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "carDto", ignore = true)
    RentalResponseDto toRentalDto(Rental rental);

    @Mapping(target = "carDto", ignore = true)
    @Mapping(target = "baseRentalCost", ignore = true)
    @Mapping(target = "penaltyAmount", ignore = true)
    @Mapping(target = "totalCost", ignore = true)
    @Mapping(target = "amountPaid", ignore = true)
    @Mapping(target = "amountDue", ignore = true)
    @Mapping(target = "status", ignore = true)
    RentalDetailedDto toRentalDetailedDto(Rental rental);
}
