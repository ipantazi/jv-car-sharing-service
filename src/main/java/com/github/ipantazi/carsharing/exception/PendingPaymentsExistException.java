package com.github.ipantazi.carsharing.exception;

public class PendingPaymentsExistException extends RuntimeException {
    public PendingPaymentsExistException(String message) {
        super(message);
    }
}
