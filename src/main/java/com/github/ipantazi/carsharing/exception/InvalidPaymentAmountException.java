package com.github.ipantazi.carsharing.exception;

import java.math.BigDecimal;

public class InvalidPaymentAmountException extends RuntimeException {
    public InvalidPaymentAmountException(BigDecimal expected, BigDecimal actual) {
        super("Invalid amount paid for the rental. Expected: %s, Paid: %s"
                .formatted(expected, actual));
    }
}
