package com.github.ipantazi.carsharing.dto.car;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CarDto {
    private Long id;
    private String model;
    private String brand;
    private String type;
    private int inventory;
    private BigDecimal dailyFee;
}
