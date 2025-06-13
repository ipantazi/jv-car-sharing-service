package com.github.ipantazi.carsharing.dto.rental;

import com.github.ipantazi.carsharing.dto.car.CarDto;
import com.github.ipantazi.carsharing.dto.enums.RentalStatus;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDetailedDto {
    private Long id;
    private Long userId;
    private String rentalDate;
    private String returnDate;
    private String actualReturnDate;
    private CarDto carDto;
    private BigDecimal baseRentalCost;
    private BigDecimal penaltyAmount;
    private BigDecimal totalCost;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private RentalStatus status;
}
