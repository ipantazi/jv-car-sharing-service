package com.github.ipantazi.carsharing.mapper;

import com.github.ipantazi.carsharing.config.MapperConfig;
import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.model.Rental;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface RentalMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "actualReturnDate", ignore = true)
    Rental toRentalEntity(RentalRequestDto rentalRequestDto, Long userId, LocalDate rentalDate);

    @Mapping(target = "id", source = "rental.id")
    RentalResponseDto toRentalDto(Rental rental,
                                  CarDto carDto,
                                  RentalStatus status,
                                  BigDecimal baseRentalCost);

    @Mapping(target = "id", source = "rental.id")
    RentalDetailedDto toRentalDetailedDto(Rental rental,
                                          CarDto carDto,
                                          BigDecimal baseRentalCost,
                                          BigDecimal penaltyAmount,
                                          BigDecimal totalCost,
                                          BigDecimal amountPaid,
                                          BigDecimal amountDue,
                                          RentalStatus status);
}
