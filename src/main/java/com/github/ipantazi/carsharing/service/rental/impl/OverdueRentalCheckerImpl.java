package com.github.ipantazi.carsharing.service.rental.impl;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.notification.NotificationMapper;
import com.github.ipantazi.carsharing.notification.NotificationService;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.repository.user.UserRepository;
import com.github.ipantazi.carsharing.service.car.CarService;
import com.github.ipantazi.carsharing.service.rental.Calculator;
import com.github.ipantazi.carsharing.service.rental.OverdueRentalChecker;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OverdueRentalCheckerImpl implements OverdueRentalChecker {
    private final Clock clock;
    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final CarService carService;
    private final Calculator calculator;

    @Override
    @Transactional(readOnly = true)
    public void checkOverdueRental() {
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);

        List<Rental> overdueRentals =
                rentalRepository.findAllByReturnDateLessThanEqualAndActualReturnDateIsNull(
                        tomorrow);

        if (overdueRentals.isEmpty()) {
            notificationService.sendToDefault("✅ No rentals overdue today!");
            return;
        }

        Set<Long> userIds = overdueRentals.stream()
                .map(Rental::getUserId)
                .collect(Collectors.toSet());
        Set<Long> carIds = overdueRentals.stream()
                .map(Rental::getCarId)
                .collect(Collectors.toSet());

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, CarDto> carsById = carService.getByIds(carIds).stream()
                .collect(Collectors.toMap(CarDto::getId, carDto -> carDto));

        overdueRentals.forEach(rental -> {
            User user = usersById.get(rental.getUserId());
            CarDto carDto = carsById.get(rental.getCarId());
            long daysOverdue = calculator.calculateDaysOverdue(rental.getReturnDate(), today);

            OverdueRentalPayload overdueRentalPayload = notificationMapper.toOverdueRentalPayload(
                    rental, user.getEmail(), carDto, daysOverdue);

            notificationService.sendMessage(
                    NotificationType.OVERDUE_RENTAL,
                    overdueRentalPayload);

        });
    }
}
