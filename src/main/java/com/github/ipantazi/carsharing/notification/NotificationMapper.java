package com.github.ipantazi.carsharing.notification;

import com.github.ipantazi.carsharing.config.MapperConfig;
import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.model.Payment;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.notification.dto.PaymentPayload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface NotificationMapper {
    @Mapping(target = "rentalId", source = "rental.id")
    @Mapping(target = "carModel", source = "carDto.model")
    @Mapping(target = "carBrand", source = "carDto.brand")
    @Mapping(target = "carType", source = "carDto.type")
    NewRentalPayload toRentalPayload(Rental rental, UserResponseDto user, CarDto carDto);

    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "amount", source = "payment.amountToPay")
    PaymentPayload toPaymentPayload(Payment payment, String email);

    @Mapping(target = "rentalId", source = "rental.id")
    @Mapping(target = "carModel", source = "carDto.model")
    @Mapping(target = "carBrand", source = "carDto.brand")
    @Mapping(target = "carType", source = "carDto.type")
    OverdueRentalPayload toOverdueRentalPayload(Rental rental,
                                                String email,
                                                CarDto carDto,
                                                long daysOverdue);
}
