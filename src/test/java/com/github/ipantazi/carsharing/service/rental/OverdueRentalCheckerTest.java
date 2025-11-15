package com.github.ipantazi.carsharing.service.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.DAYS_OVERDUE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_INSTANT;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NO_RENTALS_OVERDUE_MESSAGE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ZONE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestCarDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestOverdueRental;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestOverdueRentalPayload;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUser;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import com.github.ipantazi.carsharing.service.rental.impl.OverdueRentalCheckerImpl;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OverdueRentalCheckerTest {
    @Mock
    private Clock clock;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CarService carService;

    @Mock
    private Calculator calculator;

    @InjectMocks
    private OverdueRentalCheckerImpl overdueRentalChecker;

    @BeforeEach()
    public void setUp() {
        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);
    }

    @Test
    @DisplayName("Should send overdue rental notifications when overdue rentals exist")
    public void checkOverdueRental_OverdueRentalExists_SendOverdueRentalNotification() {
        // Given
        Rental overdueRental = createTestOverdueRental(EXISTING_RENTAL_ID);
        List<Rental> overdueRentals = List.of(overdueRental);

        Set<Long> userIds = Set.of(overdueRental.getUserId());
        Set<Long> carIds = Set.of(overdueRental.getCarId());

        User user = createTestUser(overdueRental.getUserId());
        List<User> users = List.of(user);

        CarDto carDto = createTestCarDto(overdueRental.getCarId());
        List<CarDto> cars = List.of(carDto);

        OverdueRentalPayload payload = createTestOverdueRentalPayload(EXISTING_RENTAL_ID);

        when(rentalRepository.findAllByReturnDateLessThanEqualAndActualReturnDateIsNull(
                FIXED_DATE.plusDays(1)
        )).thenReturn(overdueRentals);
        when(userRepository.findAllById(userIds)).thenReturn(users);
        when(carService.getByIds(carIds)).thenReturn(cars);
        when(calculator.calculateDaysOverdue(overdueRental.getReturnDate(), FIXED_DATE))
                .thenReturn(DAYS_OVERDUE);
        when(notificationMapper.toOverdueRentalPayload(
                overdueRental,
                user.getEmail(),
                carDto,
                DAYS_OVERDUE
        )).thenReturn(payload);

        // When
        overdueRentalChecker.checkOverdueRental();

        // Then
        verify(rentalRepository, times(1))
                .findAllByReturnDateLessThanEqualAndActualReturnDateIsNull(FIXED_DATE.plusDays(1));
        verify(userRepository, times(1)).findAllById(userIds);
        verify(carService, times(1)).getByIds(carIds);
        verify(calculator, times(1))
                .calculateDaysOverdue(overdueRental.getReturnDate(), FIXED_DATE);
        verify(notificationMapper, times(1)).toOverdueRentalPayload(
                overdueRental,
                user.getEmail(),
                carDto,
                DAYS_OVERDUE
        );
        verify(notificationService, times(1))
                .sendMessage(NotificationType.OVERDUE_RENTAL, payload);
        verify(notificationService, never()).sendToDefault(anyString());
        verifyNoMoreInteractions(rentalRepository, userRepository, carService, calculator);
        verifyNoMoreInteractions(notificationMapper, notificationService);
    }

    @Test
    @DisplayName("Should  send default notifications when no overdue rentals exist")
    public void checkOverdueRental_NoOverdueRentals_SendDefaultNotification() {
        // Given
        when(rentalRepository.findAllByReturnDateLessThanEqualAndActualReturnDateIsNull(
                FIXED_DATE.plusDays(1)
        )).thenReturn(List.of());

        // When
        overdueRentalChecker.checkOverdueRental();

        // Then
        verify(rentalRepository, times(1))
                .findAllByReturnDateLessThanEqualAndActualReturnDateIsNull(FIXED_DATE.plusDays(1));
        verify(notificationService, times(1)).sendToDefault(NO_RENTALS_OVERDUE_MESSAGE);
        verifyNoMoreInteractions(rentalRepository, notificationService);
        verifyNoInteractions(carService, calculator, userRepository);
        verifyNoInteractions(notificationMapper);
    }
}
