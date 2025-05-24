package com.github.ipantazi.carsharing.dto.car;

import com.github.ipantazi.carsharing.dto.enums.OperationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequestDto {
    @Positive(message = "Invalid inventory. Inventory should be positive.")
    private int inventory;

    @NotNull(message = "Invalid operation. Operation shouldn't be null.")
    private OperationType operation;
}
