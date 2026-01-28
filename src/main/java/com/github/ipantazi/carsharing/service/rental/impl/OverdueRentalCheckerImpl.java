package com.github.ipantazi.carsharing.service.rental.impl;

import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.notification.NotificationMapper;
import com.github.ipantazi.carsharing.notification.NotificationService;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.dto.OverdueRentalPayload;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.service.rental.Calculator;
import com.github.ipantazi.carsharing.service.rental.OverdueRentalChecker;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
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

        overdueRentals.forEach(rental -> {
            long daysOverdue = calculator.calculateDaysOverdue(rental.getReturnDate(), today);

            OverdueRentalPayload overdueRentalPayload = notificationMapper.toOverdueRentalPayload(
                    rental, daysOverdue);

            notificationService.sendMessage(
                    NotificationType.OVERDUE_RENTAL,
                    overdueRentalPayload);
        });
    }
}
