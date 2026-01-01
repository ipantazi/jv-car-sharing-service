package com.github.ipantazi.carsharing.exception;

public class PaymentAlreadyPaidException extends RuntimeException {
    public PaymentAlreadyPaidException(Long rentalId, String type) {
        super("Payment for rental: %d and type: %s already paid".formatted(rentalId, type));
    }
}
