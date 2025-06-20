package com.github.ipantazi.carsharing.controller;

import com.github.ipantazi.carsharing.dto.rental.RentalDetailedDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestDto;
import com.github.ipantazi.carsharing.dto.rental.RentalRequestFilterDto;
import com.github.ipantazi.carsharing.dto.rental.RentalResponseDto;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.security.CustomUserDetails;
import com.github.ipantazi.carsharing.service.rental.RentalService;
import com.github.ipantazi.carsharing.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rental management", description = "Operations related to car rentals")
@RestController
@RequestMapping("/rentals")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;
    private final UserService userService;

    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new rental",
            description = "Creates a new rental and decreases car inventory by 1"
    )
    public RentalResponseDto createRental(
            Authentication authentication,
            @RequestBody @Valid RentalRequestDto rentalRequestDto
    ) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return rentalService.createRental(userId, rentalRequestDto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(
            summary = "Get rentals by filter",
            description = "Returns rentals filtered by user and/or active status"
    )
    public Page<RentalResponseDto> getRentals(Authentication authentication,
                                              @Valid RentalRequestFilterDto filter,
                                              @ParameterObject Pageable pageable) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        boolean isManager = userDetails.getRole().equals(User.Role.MANAGER);

        Long actualUserId;
        if (isManager && filter.user_id() != null) {
            userService.validateUserExistsOrThrow(filter.user_id());
            actualUserId = filter.user_id();
        } else if (!isManager) {
            actualUserId = userDetails.getId();
        } else {
            actualUserId = null;
        }

        return rentalService.getRentalsByFilter(actualUserId, filter.is_active(), pageable);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Operation(
            summary = "Retrieve rental by ID",
            description = "Returns detailed information about the specified rental"
    )

    public RentalDetailedDto getRentalById(Authentication authentication,
                                           @PathVariable Long id) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return rentalService.getRentalById(userDetails.getId(), id);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/return")
    @Operation(
            summary = "Return rental by id",
            description = "Sets actual return date and increases car inventory by 1"
    )
    public RentalDetailedDto returnRental(Authentication authentication,
                                          @PathVariable Long id) {
        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        return rentalService.returnRental(userId, id);
    }
}
