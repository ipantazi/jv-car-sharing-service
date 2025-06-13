package com.github.ipantazi.carsharing.service.rental;

import static com.github.ipantazi.carsharing.util.TestDataUtil.ACTUAL_RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ACTUAL_RETURN_DATE_GREATER_THEN_RETURN_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_CAR_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_RENTAL_ID_ANOTHER_USER;
import static com.github.ipantazi.carsharing.util.TestDataUtil.EXISTING_USER_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_DATE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.FIXED_INSTANT;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MAX_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.MIN_RENTAL_DAYS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.NOT_EXISTING_RENTAL_ID;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_DTO_IGNORING_FIELDS;
import static com.github.ipantazi.carsharing.util.TestDataUtil.RENTAL_PAGEABLE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.ZONE;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createNewTestRentalResponseDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRental;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalDetailedDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalRequestDto;
import static com.github.ipantazi.carsharing.util.TestDataUtil.createTestRentalResponseDto;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertObjectsAreEqualIgnoringFields;
import static com.github.ipantazi.carsharing.util.assertions.TestAssertionsUtil.assertPageMetadataEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.github.ipantazi.carsharing.exception.EntityNotFoundException;
import com.github.ipantazi.carsharing.mapper.RentalMapper;
import com.github.ipantazi.carsharing.model.Rental;
import com.github.ipantazi.carsharing.repository.rental.RentalRepository;
import com.github.ipantazi.carsharing.repository.rental.RentalSpecificationBuilder;
import com.github.ipantazi.carsharing.service.car.CarServiceImpl;
import com.github.ipantazi.carsharing.service.car.InventoryServiceImpl;
import com.github.ipantazi.carsharing.service.user.UserServiceImpl;
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
    private UserServiceImpl userService;
    @Mock
    private Clock clock;
    @InjectMocks
    private RentalServiceImpl rentalService;

    @Test
    @DisplayName("Test createRental() method with valid data")
    public void createRental_ValidData_ReturnsRentalResponseDto() {
        // Given
        RentalResponseDto expectedRentalResponseDto = createNewTestRentalResponseDto(
                EXISTING_USER_ID);
        Rental rental = createTestRental(expectedRentalResponseDto);
        RentalRequestDto rentalRequestDto = createTestRentalRequestDto(expectedRentalResponseDto);
        CarDto carDto = expectedRentalResponseDto.getCarDto();

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(rentalMapper.toRentalEntity(rentalRequestDto)).thenReturn(rental);
        when(rentalMapper.toRentalDto(rental)).thenReturn(expectedRentalResponseDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);

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
        verify(carService, times(1)).validateCarAvailableForRental(rental.getCarId());
        verify(rentalMapper, times(1)).toRentalEntity(rentalRequestDto);
        verify(rentalRepository, times(1)).save(rental);
        verify(inventoryService, times(1))
                .adjustInventory(rentalRequestDto.carId(), 1, OperationType.DECREASE);
        verify(rentalMapper, times(1)).toRentalDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verifyNoMoreInteractions(carService, rentalMapper, rentalRepository, inventoryService);
    }

    @Test
    @DisplayName("Test createRental() method when invalid min rental days.")
    public void createRental_InvalidMinRentalDays_ThrowsException() {
        // Given
        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(INVALID_MIN_RENTAL_DAYS),
                EXISTING_USER_ID
        );

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(
                EXISTING_USER_ID,
                rentalRequestDto
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Return date must be no earlier than " + MIN_RENTAL_DAYS
                        + " day in the future");
        verifyNoInteractions(carService, rentalMapper, rentalRepository, inventoryService);
    }

    @Test
    @DisplayName("Test createRental() method when invalid max rental days.")
    public void createRental_InvalidMaxRentalDays_ThrowsException() {
        // Given
        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.plusDays(INVALID_MAX_RENTAL_DAYS),
                EXISTING_CAR_ID
        );

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(
                EXISTING_USER_ID,
                rentalRequestDto
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum rental period is " + MAX_RENTAL_DAYS + " days.");
        verifyNoInteractions(carService, rentalMapper, rentalRepository, inventoryService);
    }

    @Test
    @DisplayName("Test createRental() method when return date is in the past.")
    public void createRental_ReturnDateInThePast_ThrowsException() {
        // Given
        RentalRequestDto rentalRequestDto = new RentalRequestDto(
                FIXED_DATE.minusDays(INVALID_MAX_RENTAL_DAYS),
                EXISTING_CAR_ID
        );

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(
                EXISTING_USER_ID,
                rentalRequestDto
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Return date must be in the future");
        verifyNoInteractions(carService, rentalMapper, rentalRepository, inventoryService);
    }

    @Test
    @DisplayName("Test getRentalsByFilter()")
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
    @DisplayName("Test getRentalById() method when MANAGER find rental by any customer.")
    public void getRentalById_GetRentalAnyCustomer_ReturnsRentalDetailedDto() {
        // Given
        RentalDetailedDto expectedRentalDto = createTestRentalDetailedDto(EXISTING_USER_ID, null);
        Rental rental = createTestRental(expectedRentalDto);
        CarDto carDto = expectedRentalDto.getCarDto();

        when(userService.isManager(EXISTING_ID_ANOTHER_USER)).thenReturn(Boolean.TRUE);
        when(rentalRepository.findById(rental.getId())).thenReturn(Optional.of(rental));
        when(rentalMapper.toRentalDetailedDto(rental)).thenReturn(expectedRentalDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);

        // When
        RentalDetailedDto actualRentalDto = rentalService.getRentalById(
                EXISTING_ID_ANOTHER_USER,
                rental.getId()
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualRentalDto,
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(userService, times(1)).isManager(EXISTING_ID_ANOTHER_USER);
        verify(rentalRepository, times(1)).findById(rental.getId());
        verify(rentalMapper, times(1)).toRentalDetailedDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verifyNoMoreInteractions(userService, rentalRepository, carService, rentalMapper);
    }

    @Test
    @DisplayName("Verify that thrown an exception when MANAGER can't find non exist rental.")
    public void getRentalById_NonExistsRentalId_ThrowsException() {
        // Given
        when(userService.isManager(EXISTING_ID_ANOTHER_USER)).thenReturn(Boolean.TRUE);
        when(rentalRepository.findById(NOT_EXISTING_RENTAL_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rentalService.getRentalById(
                EXISTING_ID_ANOTHER_USER,
                NOT_EXISTING_RENTAL_ID
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rental not found with id: " + NOT_EXISTING_RENTAL_ID);
        verify(userService, times(1)).isManager(EXISTING_ID_ANOTHER_USER);
        verify(rentalRepository, times(1)).findById(NOT_EXISTING_RENTAL_ID);
        verifyNoMoreInteractions(userService, rentalRepository);
        verifyNoInteractions(carService, rentalMapper);
    }

    @Test
    @DisplayName("Test getRentalById() method when CUSTOMER is looking for their rental.")
    public void getRentalById_GivenCustomerRental_ReturnsRentalDetailedDto() {
        // Given
        RentalDetailedDto expectedRentalDto = createTestRentalDetailedDto(EXISTING_USER_ID, null);
        Rental rental = createTestRental(expectedRentalDto);
        CarDto carDto = expectedRentalDto.getCarDto();

        when(userService.isManager(EXISTING_USER_ID)).thenReturn(Boolean.FALSE);
        when(rentalRepository.findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID))
                .thenReturn(Optional.of(rental));
        when(rentalMapper.toRentalDetailedDto(rental)).thenReturn(expectedRentalDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);

        // When
        RentalDetailedDto actualRentalDto = rentalService.getRentalById(
                EXISTING_USER_ID,
                rental.getId()
        );

        // Then
        assertObjectsAreEqualIgnoringFields(
                actualRentalDto,
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );
        verify(userService, times(1)).isManager(EXISTING_USER_ID);
        verify(rentalRepository, times(1))
                .findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID);
        verify(rentalMapper, times(1)).toRentalDetailedDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verifyNoMoreInteractions(userService, rentalRepository, carService, rentalMapper);
    }

    @Test
    @DisplayName("Verify that thrown an exception when CUSTOMER is looking for alien rental.")
    public void getRentalById_AlienRental_ThrowsException() {
        // Given
        when(userService.isManager(EXISTING_USER_ID)).thenReturn(Boolean.FALSE);
        when(rentalRepository.findRentalByIdAndUserId(
                EXISTING_RENTAL_ID_ANOTHER_USER,
                EXISTING_USER_ID
        ))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rentalService.getRentalById(
                EXISTING_USER_ID,
                EXISTING_RENTAL_ID_ANOTHER_USER
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Rental not found with id: " + EXISTING_RENTAL_ID_ANOTHER_USER);
        verify(userService, times(1)).isManager(EXISTING_USER_ID);
        verify(rentalRepository, times(1))
                .findRentalByIdAndUserId(EXISTING_RENTAL_ID_ANOTHER_USER, EXISTING_USER_ID);
        verifyNoMoreInteractions(userService, rentalRepository);
        verifyNoInteractions(carService, rentalMapper);
    }

    @Test
    @DisplayName("Test getRentalById() when actualReturnedDate is greater than returnedDate.")
    public void getRentalById_ActualReturnedDateGreaterReturnedDate_ReturnsRentalDetailedDto() {
        // Given
        RentalDetailedDto expectedRentalDto = createTestRentalDetailedDto(
                EXISTING_USER_ID,
                ACTUAL_RETURN_DATE_GREATER_THEN_RETURN_DATE
        );
        Rental rental = createTestRental(expectedRentalDto);
        CarDto carDto = expectedRentalDto.getCarDto();

        when(userService.isManager(EXISTING_USER_ID)).thenReturn(Boolean.FALSE);
        when(rentalRepository.findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID))
                .thenReturn(Optional.of(rental));
        when(rentalMapper.toRentalDetailedDto(rental)).thenReturn(expectedRentalDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);

        // When
        RentalDetailedDto actualRentalDto = rentalService.getRentalById(
                EXISTING_USER_ID,
                rental.getId()
        );

        // Then
        assertThat(actualRentalDto.getPenaltyAmount())
                .isEqualTo(expectedRentalDto.getPenaltyAmount());
        assertThat(actualRentalDto.getTotalCost()).isEqualTo(
                actualRentalDto.getBaseRentalCost().add(actualRentalDto.getPenaltyAmount())
        );
        assertThat(actualRentalDto.getAmountDue()).isEqualTo(
                actualRentalDto.getTotalCost().subtract(actualRentalDto.getAmountPaid())
        );
        assertObjectsAreEqualIgnoringFields(
                actualRentalDto,
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );

        verify(userService, times(1)).isManager(EXISTING_USER_ID);
        verify(rentalRepository, times(1))
                .findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID);
        verify(rentalMapper, times(1)).toRentalDetailedDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verifyNoMoreInteractions(userService, rentalRepository, carService, rentalMapper);
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
        when(userService.isManager(EXISTING_USER_ID)).thenReturn(Boolean.FALSE);
        when(rentalRepository.findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID))
                .thenReturn(Optional.of(rental));
        when(rentalMapper.toRentalDetailedDto(rental)).thenReturn(expectedRentalDto);
        when(carService.getById(rental.getCarId())).thenReturn(carDto);

        // When
        RentalDetailedDto actualRentalDto = rentalService.returnRental(
                EXISTING_USER_ID,
                rental.getId()
        );

        // Then
        assertThat(actualRentalDto.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(actualRentalDto.getActualReturnDate())
                .isEqualTo(String.valueOf(FIXED_DATE));
        assertObjectsAreEqualIgnoringFields(
                actualRentalDto,
                expectedRentalDto,
                RENTAL_DTO_IGNORING_FIELDS
        );

        verify(userService, times(1)).isManager(EXISTING_USER_ID);
        verify(rentalRepository, times(1))
                .findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID);
        verify(rentalRepository, times(1)).save(rental);
        verify(inventoryService, times(1))
                .adjustInventory(rental.getCarId(), 1, OperationType.INCREASE);
        verify(rentalMapper, times(1)).toRentalDetailedDto(rental);
        verify(carService, times(1)).getById(rental.getCarId());
        verifyNoMoreInteractions(userService, rentalRepository, carService);
        verifyNoMoreInteractions(rentalMapper, inventoryService);
    }

    @Test
    @DisplayName("Test returnRental() method when rental already returned.")
    public void returnRental_AlreadyReturned_ThrowsException() {
        // Given
        Rental rental = createTestRental(createTestRentalDetailedDto(
                EXISTING_USER_ID,
                ACTUAL_RETURN_DATE
        ));

        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(userService.isManager(EXISTING_USER_ID)).thenReturn(Boolean.FALSE);
        when(rentalRepository.findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID))
                .thenReturn(Optional.of(rental));

        // When & Then
        assertThatThrownBy(() -> rentalService.returnRental(
                EXISTING_USER_ID,
                rental.getId()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rental with id " + rental.getId()
                        + " is already returned on " + rental.getActualReturnDate());

        verify(userService, times(1)).isManager(EXISTING_USER_ID);
        verify(rentalRepository, times(1))
                .findRentalByIdAndUserId(rental.getId(), EXISTING_USER_ID);
        verify(rentalRepository, never()).save(rental);
        verify(inventoryService, never())
                .adjustInventory(rental.getCarId(), 1, OperationType.INCREASE);
        verifyNoMoreInteractions(userService, rentalRepository);
        verifyNoInteractions(carService, rentalMapper, inventoryService);
    }
}
