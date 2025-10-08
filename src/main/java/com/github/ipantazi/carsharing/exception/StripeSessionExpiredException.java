package com.github.ipantazi.carsharing.exception;

public class StripeSessionExpiredException extends RuntimeException {
    public StripeSessionExpiredException(String message) {
        super(message);
    }
}
