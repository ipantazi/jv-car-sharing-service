package com.github.ipantazi.carsharing.notification;

import com.github.ipantazi.carsharing.config.MapperConfig;
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
    @Mapping(target = "carModel", source = "rental.car.model")
    @Mapping(target = "carBrand", source = "rental.car.brand")
    @Mapping(target = "carType", source = "rental.car.type")
    @Mapping(target = "email", source = "rental.user.email")
    @Mapping(target = "firstName", source = "rental.user.firstName")
    @Mapping(target = "lastName", source = "rental.user.lastName")
    NewRentalPayload toRentalPayload(Rental rental);

    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "amount", source = "payment.amountToPay")
    PaymentPayload toPaymentPayload(Payment payment, String email);

    @Mapping(target = "rentalId", source = "rental.id")
    @Mapping(target = "carModel", source = "rental.car.model")
    @Mapping(target = "carBrand", source = "rental.car.brand")
    @Mapping(target = "carType", source = "rental.car.type")
    @Mapping(target = "email", source = "rental.user.email")
    OverdueRentalPayload toOverdueRentalPayload(Rental rental,
                                                long daysOverdue);
}
