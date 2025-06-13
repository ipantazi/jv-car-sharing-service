package com.github.ipantazi.carsharing.service.rental;

import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RentalService {
    RentalResponseDto createRental(Long userId, RentalRequestDto rentalRequestDto);

    Page<RentalResponseDto> getRentalsByFilter(Long userId, Boolean isActive, Pageable pageable);

    RentalDetailedDto getRentalById(Long userId, Long rentalId);

    RentalDetailedDto returnRental(Long userId, Long rentalId);
}
