package com.github.ipantazi.carsharing.service.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.ACTUAL_RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_INSTANT;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_DTO_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ZONE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createNewTestRentalResponseDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestNewRentalPayload;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRental;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalDetailedDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalDetailedDtoWithPenalty;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalResponseDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestUserResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.enums.OperationType;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.dto.user.UserResponseDto;
import com.github.ipantazi.carsharing.exception.CarNotAvailableException;
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.exception.InvalidRentalDatesException;
import com.github.ipantazi.carsharing.exception.PendingPaymentsExistException;
import com.github.ipantazi.carsharing.mapper.RentalMapper;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.notification.NotificationMapper;
import com.github.ipantazi.carsharing.notification.NotificationService;
import com.github.ipantazi.carsharing.notification.NotificationType;
import com.github.ipantazi.carsharing.notification.dto.NewRentalPayload;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalSpecificationBuilder;
import com.github.ipantazi.carsharing.service.car.CarServiceImpl;
import com.github.ipantazi.carsharing.service.car.InventoryServiceImpl;
import com.github.ipantazi.carsharing.service.payment.PaymentValidator;
import com.github.ipantazi.carsharing.service.rental.impl.RentalServiceImpl;
import com.github.ipantazi.carsharing.service.user.UserService;
import java.time.Clock;
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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
public class RentalServiceTest {
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private CarServiceImpl carService;
    @Mock
    private InventoryServiceImpl inventoryService;
    @Mock
    private RentalSpecificationBuilder specificationBuilder;
    @Mock
    private Specification<Rental> specification;
    @Mock
    private PaymentValidator paymentValidator;
    @Mock
    private RentalValidator rentalValidator;
    @Mock
    private Calculator calculator;
    @Mock
    private Clock clock;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMapper notificationMapper;
    @InjectMocks
    private RentalServiceImpl rentalService;

    @Test
    @DisplayName("Test createRental() method with valid data")
    public void createRental_ValidData_ReturnsRentalResponseDto() {
        // Given
        RentalResponseDto expectedRentalResponseDto = createNewTestRentalResponseDto(
                EXISTING_USER_ID, RENTAL_DATE);
        Rental rental = createTestRental(expectedRentalResponseDto);
        RentalRequestDto rentalRequestDto = createTestRentalRequestDto(expectedRentalResponseDto);
        CarDto carDto = expectedRentalResponseDto.getCarDto();
        UserResponseDto userDto = createTestUserResponseDto(expectedRentalResponseDto.getUserId());
        NewRentalPayload rentalPayload = createTestNewRentalPayload(rental);

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(RENTAL_DATE.atStartOfDay(ZONE).toInstant());
        when(rentalMapper.toRentalEntity(rentalRequestDto)).thenReturn(rental);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);
        when(userService.getUserDetails(expectedRentalResponseDto.getUserId())).thenReturn(userDto);
        when(notificationMapper.toRentalPayload(rental, userDto, carDto)).thenReturn(rentalPayload);
        when(rentalMapper.toRentalDto(rental)).thenReturn(expectedRentalResponseDto);
        when(calculator.calculateBaseRentalCost(carDto.getDailyFee(), rental))
                .thenReturn(expectedRentalResponseDto.getBaseRentalCost());

        // When
        RentalResponseDto actualRentalResponseDto = rentalService.createRental(
                EXISTING_USER_ID,
                rentalRequestDto
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualRentalResponseDto,
                expectedRentalResponseDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(paymentValidator, times(1)).checkForPendingPayments(EXISTING_USER_ID);
        verify(rentalValidator, times(1))
                .checkDatesBeforeRenting(rentalRequestDto, RENTAL_DATE);
        verify(carService, times(1)).validateCarAvailableForRental(rental.getCarId());
        verify(rentalMapper, times(1)).toRentalEntity(rentalRequestDto);
        verify(rentalRepository, times(1)).save(rental);
        verify(inventoryService, times(1))
                .adjustInventory(rentalRequestDto.carId(), 1, OperationType.DECREASE);
        verify(carService, times(1)).getById(rental.getCarId());
        verify(userService, times(1)).getUserDetails(expectedRentalResponseDto.getUserId());
        verify(notificationMapper, times(1)).toRentalPayload(rental, userDto, carDto);
        verify(notificationService, times(1))
                .sendMessage(NotificationType.NEW_RENTAL_CREATED, rentalPayload);
        verify(rentalMapper, times(1)).toRentalDto(rental);
        verify(calculator, times(1)).calculateBaseRentalCost(carDto.getDailyFee(), rental);
        verifyNoMoreInteractions(carService, paymentValidator, rentalValidator, rentalMapper);
        verifyNoMoreInteractions(rentalRepository, inventoryService, calculator);
        verifyNoMoreInteractions(userService, notificationService, notificationMapper);
    }

    @Test
    @DisplayName("Test createRental() with invalid return date")
    public void createRental_InvalidReturnDate_ThrowsException() {
        // Given
        RentalRequestDto rentalRequestDto = new RentalRequestDto(RETURN_DATE, EXISTING_CAR_ID);

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(RENTAL_DATE.atStartOfDay(ZONE).toInstant());
        doThrow(new InvalidRentalDatesException(
                "Return date must be no earlier than %d day in the future"
                        .formatted(MIN_RENTAL_DAYS)
        )).when(rentalValidator).checkDatesBeforeRenting(rentalRequestDto, RENTAL_DATE);

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(EXISTING_USER_ID, rentalRequestDto))
                .isInstanceOf(InvalidRentalDatesException.class)
                .hasMessage("Return date must be no earlier than %d day in the future"
                        .formatted(MIN_RENTAL_DAYS));

        verify(rentalValidator, times(1))
                .checkDatesBeforeRenting(rentalRequestDto, RENTAL_DATE);
        verify(rentalRepository, never()).save(any());
        verify(inventoryService, never())
                .adjustInventory(rentalRequestDto.carId(), 1, OperationType.DECREASE);

        verifyNoMoreInteractions(rentalValidator);
        verifyNoInteractions(carService, paymentValidator, rentalMapper, rentalRepository);
        verifyNoInteractions(inventoryService, calculator);
        verifyNoInteractions(userService, notificationService, notificationMapper);
    }

    @Test
    @DisplayName("Test createRental() when car is not available for renting")
    public void createRental_CarNotAvailableForRenting_ThrowsException() {
        // Given
        RentalRequestDto rentalRequestDto = new RentalRequestDto(RETURN_DATE, EXISTING_CAR_ID);

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(RENTAL_DATE.atStartOfDay(ZONE).toInstant());
        doThrow(new CarNotAvailableException(
                "Car with id: %d is not available for rental."
                        .formatted(rentalRequestDto.carId())
        )).when(carService).validateCarAvailableForRental(rentalRequestDto.carId());

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(EXISTING_USER_ID, rentalRequestDto))
                .isInstanceOf(CarNotAvailableException.class)
                .hasMessage("Car with id: %d is not available for rental."
                        .formatted(rentalRequestDto.carId()));

        verify(rentalValidator, times(1)).checkDatesBeforeRenting(rentalRequestDto, RENTAL_DATE);
        verify(carService, times(1)).validateCarAvailableForRental(rentalRequestDto.carId());
        verify(rentalRepository, never()).save(any());
        verify(inventoryService, never())
                .adjustInventory(rentalRequestDto.carId(), 1, OperationType.DECREASE);

        verifyNoMoreInteractions(rentalValidator, carService);
        verifyNoInteractions(paymentValidator, rentalMapper, rentalRepository);
        verifyNoInteractions(inventoryService, calculator);
        verifyNoInteractions(userService, notificationService, notificationMapper);
    }

    @Test
    @DisplayName("Test createRental() when user has pending payments")
    public void createRental_UserHasPendingPayments_ThrowsException() {
        // Given
        RentalRequestDto rentalRequestDto = new RentalRequestDto(RETURN_DATE, EXISTING_CAR_ID);

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(RENTAL_DATE.atStartOfDay(ZONE).toInstant());
        doThrow(new PendingPaymentsExistException("User has pending payments"))
                .when(paymentValidator).checkForPendingPayments(EXISTING_USER_ID);

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(EXISTING_USER_ID, rentalRequestDto))
                .isInstanceOf(PendingPaymentsExistException.class)
                .hasMessage("User has pending payments");

        verify(rentalValidator, times(1)).checkDatesBeforeRenting(rentalRequestDto, RENTAL_DATE);
        verify(carService, times(1)).validateCarAvailableForRental(rentalRequestDto.carId());
        verify(paymentValidator, times(1)).checkForPendingPayments(EXISTING_USER_ID);
        verify(rentalRepository, never()).save(any());
        verify(inventoryService, never())
                .adjustInventory(rentalRequestDto.carId(), 1, OperationType.DECREASE);

        verifyNoMoreInteractions(rentalValidator, carService, paymentValidator);
        verifyNoInteractions(rentalMapper, rentalRepository);
        verifyNoInteractions(inventoryService, calculator);
        verifyNoInteractions(userService, notificationService, notificationMapper);
    }

    @Test
    @DisplayName("Test getRentalsByFilter() with valid data and active rentals")
    public void getRentalsByFilter_ValidDataAndActiveRentals_ReturnsRentalResponseDtoPage() {
        // Given
        Boolean isActive = true;
        RentalResponseDto expectedRentalDto = createTestRentalResponseDto(EXISTING_USER_ID, null);
        Rental rental = createTestRental(expectedRentalDto);
        List<Rental> rentalList = Collections.singletonList(rental);
        Page<Rental> rentalPage = new PageImpl<>(rentalList, RENTAL_PAGEABLE, rentalList.size());
        CarDto carDto = expectedRentalDto.getCarDto();
        List<CarDto> carDtoList = Collections.singletonList(carDto);
        Set<Long> carIds = Collections.singleton(carDto.getId());

        when(specificationBuilder.build(EXISTING_USER_ID, isActive)).thenReturn(specification);
        when(rentalRepository.findAll(specification, RENTAL_PAGEABLE)).thenReturn(rentalPage);
        when(carService.getByIds(carIds)).thenReturn(carDtoList);
        when(rentalMapper.toRentalDto(rental)).thenReturn(expectedRentalDto);

        // When
        Page<RentalResponseDto> actualRentalDtoPage = rentalService.getRentalsByFilter(
                EXISTING_USER_ID,
                isActive,
                RENTAL_PAGEABLE
        );

        // Then
        List<RentalResponseDto> actualRentalDtoList = actualRentalDtoPage
                .getContent();
        assertThat(actualRentalDtoList).hasSize(1).containsExactly(expectedRentalDto);
        assertObjectsAreEqualIgnoringFields(
                actualRentalDtoList.get(0),
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertPageMetadataEquals(actualRentalDtoPage, rentalPage);
        verify(specificationBuilder, times(1)).build(EXISTING_USER_ID, isActive);
        verify(rentalRepository, times(1)).findAll(specification, RENTAL_PAGEABLE);
        verify(carService, times(1)).getByIds(carIds);
        verify(rentalMapper, times(1)).toRentalDto(rental);
        verifyNoMoreInteractions(specificationBuilder, rentalMapper, rentalRepository, carService);
    }

    @Test
    @DisplayName("Test getRentalsByFilter() returns empty page.")
    public void getRentalsByFilter_NotRentalExists_ReturnsEmptyPage() {
        // Given
        Boolean isActive = false;
        Page<Rental> rentalPage = new PageImpl<>(Collections.emptyList(), RENTAL_PAGEABLE, 0);

        when(specificationBuilder.build(EXISTING_USER_ID, isActive)).thenReturn(specification);
        when(rentalRepository.findAll(specification, RENTAL_PAGEABLE)).thenReturn(rentalPage);

        // When
        Page<RentalResponseDto> actualRentalDtoPage = rentalService.getRentalsByFilter(
                EXISTING_USER_ID,
                isActive,
                RENTAL_PAGEABLE
        );

        // Then
        assertThat(actualRentalDtoPage).isEmpty();
        assertPageMetadataEquals(actualRentalDtoPage, rentalPage);
        verify(specificationBuilder, times(1)).build(EXISTING_USER_ID, isActive);
        verify(rentalRepository, times(1)).findAll(specification, RENTAL_PAGEABLE);
        verifyNoMoreInteractions(specificationBuilder, rentalRepository);
        verifyNoInteractions(carService, rentalMapper);
    }

    @Test
    @DisplayName("Test that rental status is active when non actual return date.")
    public void getRentalsByFilter_NonActualReturnDate_ReturnsRentalResponseDtoPage() {
        // Given
        final RentalStatus expectedRentalStatus = RentalStatus.ACTIVE;
        Boolean isActive = null;
        RentalResponseDto expectedRentalDto = createTestRentalResponseDto(EXISTING_USER_ID, null);
        Rental rental = createTestRental(expectedRentalDto);
        List<Rental> rentalList = Collections.singletonList(rental);
        Page<Rental> rentalPage = new PageImpl<>(rentalList, RENTAL_PAGEABLE, rentalList.size());
        CarDto carDto = expectedRentalDto.getCarDto();
        List<CarDto> carDtoList = Collections.singletonList(carDto);
        Set<Long> carIds = Collections.singleton(carDto.getId());

        when(specificationBuilder.build(EXISTING_USER_ID, isActive)).thenReturn(specification);
        when(rentalRepository.findAll(specification, RENTAL_PAGEABLE)).thenReturn(rentalPage);
        when(carService.getByIds(carIds)).thenReturn(carDtoList);
        when(rentalMapper.toRentalDto(rental)).thenReturn(expectedRentalDto);

        // When
        Page<RentalResponseDto> actualRentalDtoPage = rentalService.getRentalsByFilter(
                EXISTING_USER_ID,
                isActive,
                RENTAL_PAGEABLE
        );

        // Then
        List<RentalResponseDto> actualRentalDtoList = actualRentalDtoPage
                .getContent();
        assertThat(actualRentalDtoList).hasSize(1).containsExactly(expectedRentalDto);
        assertThat(actualRentalDtoList.get(0).getStatus()).isEqualTo(expectedRentalStatus);
        assertPageMetadataEquals(actualRentalDtoPage, rentalPage);

        verify(specificationBuilder, times(1)).build(EXISTING_USER_ID, isActive);
        verify(rentalRepository, times(1)).findAll(specification, RENTAL_PAGEABLE);
        verify(carService, times(1)).getByIds(carIds);
        verify(rentalMapper, times(1)).toRentalDto(rental);
        verifyNoMoreInteractions(specificationBuilder, rentalMapper, rentalRepository, carService);
    }

    @Test
    @DisplayName("Test that rental status is returned when exists actual return date.")
    public void getRentalsByFilter_ExistsActualReturnDate_ReturnsRentalResponseDtoPage() {
        // Given
        final RentalStatus expectedRentalStatus = RentalStatus.RETURNED;
        Boolean isActive = false;
        RentalResponseDto expectedRentalDto = createTestRentalResponseDto(
                EXISTING_USER_ID,
                ACTUAL_RETURN_DATE
        );
        Rental rental = createTestRental(expectedRentalDto);
        List<Rental> rentalList = Collections.singletonList(rental);
        Page<Rental> rentalPage = new PageImpl<>(rentalList, RENTAL_PAGEABLE, rentalList.size());
        CarDto carDto = expectedRentalDto.getCarDto();
        List<CarDto> carDtoList = Collections.singletonList(carDto);
        Set<Long> carIds = Collections.singleton(carDto.getId());

        when(specificationBuilder.build(EXISTING_USER_ID, isActive)).thenReturn(specification);
        when(rentalRepository.findAll(specification, RENTAL_PAGEABLE)).thenReturn(rentalPage);
        when(carService.getByIds(carIds)).thenReturn(carDtoList);
        when(rentalMapper.toRentalDto(rental)).thenReturn(expectedRentalDto);

        // When
        Page<RentalResponseDto> actualRentalDtoPage = rentalService.getRentalsByFilter(
                EXISTING_USER_ID,
                isActive,
                RENTAL_PAGEABLE
        );

        // Then
        List<RentalResponseDto> actualRentalDtoList = actualRentalDtoPage
                .getContent();
        assertThat(actualRentalDtoList).hasSize(1).containsExactly(expectedRentalDto);
        assertThat(actualRentalDtoList.get(0).getStatus()).isEqualTo(expectedRentalStatus);
        assertPageMetadataEquals(actualRentalDtoPage, rentalPage);

        verify(specificationBuilder, times(1)).build(EXISTING_USER_ID, isActive);
        verify(rentalRepository, times(1)).findAll(specification, RENTAL_PAGEABLE);
        verify(carService, times(1)).getByIds(carIds);
        verify(rentalMapper, times(1)).toRentalDto(rental);
        verifyNoMoreInteractions(specificationBuilder, rentalMapper, rentalRepository, carService);
    }

    @Test
    @DisplayName("Test that returns all rentals for MANAGER when isActive and userId is null")
    public void getRentalsByFilter_NonUserId_ReturnsRentalResponseDtoPage() {
        // Given
        Boolean isActive = null;
        RentalResponseDto expectedRentalDto = createTestRentalResponseDto(EXISTING_USER_ID, null);
        Rental rental = createTestRental(expectedRentalDto);
        List<Rental> rentalList = Collections.singletonList(rental);
        Page<Rental> rentalPage = new PageImpl<>(rentalList, RENTAL_PAGEABLE, rentalList.size());
        CarDto carDto = expectedRentalDto.getCarDto();
        List<CarDto> carDtoList = Collections.singletonList(carDto);
        Set<Long> carIds = Collections.singleton(carDto.getId());

        when(specificationBuilder.build(null, isActive)).thenReturn(specification);
        when(rentalRepository.findAll(specification, RENTAL_PAGEABLE)).thenReturn(rentalPage);
        when(carService.getByIds(carIds)).thenReturn(carDtoList);
        when(rentalMapper.toRentalDto(rental)).thenReturn(expectedRentalDto);

        // When
        Page<RentalResponseDto> actualRentalDtoPage = rentalService.getRentalsByFilter(
                null,
                isActive,
                RENTAL_PAGEABLE
        );

        // Then
        List<RentalResponseDto> actualRentalDtoList = actualRentalDtoPage
                .getContent();
        assertThat(actualRentalDtoList).hasSize(1).containsExactly(expectedRentalDto);
        assertObjectsAreEqualIgnoringFields(
                actualRentalDtoList.get(0),
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        assertPageMetadataEquals(actualRentalDtoPage, rentalPage);
        verify(specificationBuilder, times(1)).build(null, isActive);
        verify(rentalRepository, times(1)).findAll(specification, RENTAL_PAGEABLE);
        verify(carService, times(1)).getByIds(carIds);
        verify(rentalMapper, times(1)).toRentalDto(rental);
        verifyNoMoreInteractions(specificationBuilder, rentalMapper, rentalRepository, carService);
    }

    @Test
    @DisplayName("""
            Test getRentalById() method when MANAGER or CUSTOMER find rental by any customer and
             actual return date is null.
            """)
    public void getRentalById_GetRentalWithNonActualReturnDate_ReturnsRentalDetailedDto() {
        // Given
        RentalDetailedDto expectedRentalDto = createTestRentalDetailedDto(EXISTING_USER_ID, null);
        Rental rental = createTestRental(expectedRentalDto);
        CarDto carDto = expectedRentalDto.getCarDto();

        when(rentalValidator.getRentalWithAccessCheck(EXISTING_ID_ANOTHER_USER, rental.getId()))
                .thenReturn(rental);
        when(rentalMapper.toRentalDetailedDto(rental)).thenReturn(expectedRentalDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);
        when(calculator.calculateBaseRentalCost(carDto.getDailyFee(), rental))
                .thenReturn(expectedRentalDto.getBaseRentalCost());
        when(calculator.calculateTotalAmountPaid(rental.getId()))
                .thenReturn(expectedRentalDto.getAmountPaid());

        // When
        RentalDetailedDto actualRentalDto = rentalService.getRental(
                EXISTING_ID_ANOTHER_USER,
                rental.getId()
        );

        // Then
        assertThat(actualRentalDto.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertObjectsAreEqualIgnoringFields(
                actualRentalDto,
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(rentalValidator, times(1)).getRentalWithAccessCheck(
                EXISTING_ID_ANOTHER_USER,
                rental.getId()
        );
        verify(rentalMapper, times(1)).toRentalDetailedDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verify(calculator, times(1)).calculateBaseRentalCost(carDto.getDailyFee(), rental);
        verify(calculator, never()).calculatePenaltyAmount(carDto.getDailyFee(), rental);
        verify(calculator, times(1)).calculateTotalAmountPaid(rental.getId());
        verifyNoMoreInteractions(rentalValidator, calculator, carService, rentalMapper);
    }

    @Test
    @DisplayName("Test getRentalById() when actualReturnedDate is greater than returnedDate.")
    public void getRentalById_ActualReturnedDateGreaterReturnedDate_ReturnsRentalDetailedDto() {
        // Given
        RentalDetailedDto expectedRentalDto = createTestRentalDetailedDtoWithPenalty(
                EXISTING_USER_ID);
        Rental rental = createTestRental(expectedRentalDto);
        CarDto carDto = expectedRentalDto.getCarDto();

        when(rentalValidator.getRentalWithAccessCheck(EXISTING_USER_ID, rental.getId()))
                .thenReturn(rental);
        when(rentalMapper.toRentalDetailedDto(rental)).thenReturn(expectedRentalDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);
        when(calculator.calculateBaseRentalCost(carDto.getDailyFee(), rental))
                .thenReturn(expectedRentalDto.getBaseRentalCost());
        when(calculator.calculatePenaltyAmount(carDto.getDailyFee(), rental))
                .thenReturn(expectedRentalDto.getPenaltyAmount());
        when(calculator.calculateTotalAmountPaid(rental.getId()))
                .thenReturn(expectedRentalDto.getAmountPaid());

        // When
        RentalDetailedDto actualRentalDto = rentalService.getRental(
                EXISTING_USER_ID,
                rental.getId()
        );

        // Then
        assertThat(actualRentalDto.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertObjectsAreEqualIgnoringFields(
                actualRentalDto,
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(rentalValidator, times(1)).getRentalWithAccessCheck(
                EXISTING_USER_ID,
                rental.getId()
        );
        verify(rentalMapper, times(1)).toRentalDetailedDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verify(calculator, times(1)).calculateBaseRentalCost(carDto.getDailyFee(), rental);
        verify(calculator, times(1)).calculatePenaltyAmount(carDto.getDailyFee(), rental);
        verify(calculator, times(1)).calculateTotalAmountPaid(rental.getId());
        verifyNoMoreInteractions(rentalValidator, calculator, carService, rentalMapper);
    }

    @Test
    @DisplayName("Test returnRental() method with valid request.")
    public void returnRental_ValidRequest_ReturnsRentalDetailedDto() {
        // Given
        RentalDetailedDto expectedRentalDto = createTestRentalDetailedDto(EXISTING_USER_ID, null);
        final Rental rental = createTestRental(expectedRentalDto);
        final CarDto carDto = expectedRentalDto.getCarDto();
        expectedRentalDto.setActualReturnDate(String.valueOf(FIXED_DATE));

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(rentalValidator.getRentalWithAccessCheck(EXISTING_USER_ID, rental.getId()))
                .thenReturn(rental);
        when(rentalMapper.toRentalDetailedDto(rental)).thenReturn(expectedRentalDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);
        when(calculator.calculateBaseRentalCost(carDto.getDailyFee(), rental))
                .thenReturn(expectedRentalDto.getBaseRentalCost());
        when(calculator.calculateTotalAmountPaid(rental.getId()))
                .thenReturn(expectedRentalDto.getAmountPaid());

        // When
        RentalDetailedDto actualRentalDto = rentalService.returnRental(
                EXISTING_USER_ID,
                rental.getId()
        );

        // Then
        assertThat(actualRentalDto.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertObjectsAreEqualIgnoringFields(
                actualRentalDto,
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );

        verify(rentalValidator, times(1)).getRentalWithAccessCheck(
                EXISTING_USER_ID,
                rental.getId()
        );
        verify(rentalRepository, times(1)).save(rental);
        verify(inventoryService, times(1))
                .adjustInventory(rental.getCarId(), 1, OperationType.INCREASE);
        verify(rentalMapper, times(1)).toRentalDetailedDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verify(calculator, times(1)).calculateBaseRentalCost(carDto.getDailyFee(), rental);
        verify(calculator, never()).calculatePenaltyAmount(carDto.getDailyFee(), rental);
        verify(calculator, times(1)).calculateTotalAmountPaid(rental.getId());
        verifyNoMoreInteractions(rentalValidator, rentalRepository, carService);
        verifyNoMoreInteractions(rentalMapper, inventoryService);
    }

    @Test
    @DisplayName("Test returnRental() method when rental already returned.")
    public void returnRental_AlreadyReturned_ThrowsException() {
        // Given
        Rental rental = createTestRental(
                EXISTING_USER_ID,
                ACTUAL_RETURN_DATE
        );

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(rentalValidator.getRentalWithAccessCheck(EXISTING_USER_ID, rental.getId()))
                .thenReturn(rental);

        // When & Then
        assertThatThrownBy(() -> rentalService.returnRental(
                EXISTING_USER_ID,
                rental.getId()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rental with id %d is already returned on %s"
                        .formatted(rental.getId(), rental.getActualReturnDate()));

        verify(rentalValidator, times(1)).getRentalWithAccessCheck(
                EXISTING_USER_ID,
                rental.getId()
        );
        verify(rentalRepository, never()).save(rental);
        verify(inventoryService, never())
                .adjustInventory(rental.getCarId(), 1, OperationType.INCREASE);
        verifyNoMoreInteractions(rentalValidator);
        verifyNoInteractions(carService, rentalMapper, inventoryService, rentalRepository);
    }

    @Test
    @DisplayName("Test getRentalByIdAndUserId() method works.")
    public void getRentalByIdAndUserId_ValidData_ReturnsRental() {
        // Given
        Rental expectedRental = createTestRental(EXISTING_USER_ID,null);

        when(rentalRepository.findRentalByIdAndUserId(
                EXISTING_RENTAL_ID,
                EXISTING_USER_ID
        )).thenReturn(Optional.of(expectedRental));

        // When
        Rental actualRental = rentalService.getRentalByIdAndUserId(
                EXISTING_RENTAL_ID,
                EXISTING_USER_ID
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualRental,
                expectedRental,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(rentalRepository, times(1)).findRentalByIdAndUserId(
                EXISTING_RENTAL_ID,
                EXISTING_USER_ID
        );
        verifyNoMoreInteractions(rentalRepository);
    }

    @Test
    @DisplayName("Test getRentalByIdAndUserId() method throws exception when rental not found.")
    public void getRentalByIdAndUserId_RentalNotFound_ThrowsException() {
        // Given
        when(rentalRepository.findRentalByIdAndUserId(
                EXISTING_RENTAL_ID,
                EXISTING_USER_ID
        )).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rentalService.getRentalByIdAndUserId(
                EXISTING_RENTAL_ID,
                EXISTING_USER_ID
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rental not found with id: %d and user id: %d."
                        .formatted(EXISTING_RENTAL_ID, EXISTING_USER_ID));

        verify(rentalRepository, times(1)).findRentalByIdAndUserId(
                EXISTING_RENTAL_ID,
                EXISTING_USER_ID
        );
        verifyNoMoreInteractions(rentalRepository);
    }
}
