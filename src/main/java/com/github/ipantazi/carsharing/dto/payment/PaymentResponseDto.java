package com.github.ipantazi.carsharing.dto.payment;

import com.github.ipantazi.carsharing.model.Payment;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDto {
    private Long id;
    private Long rentalId;
    private String sessionUrl;
    private String sessionId;
    private BigDecimal amountToPay;
    private Payment.Status status;
    private Payment.Type type;
}
