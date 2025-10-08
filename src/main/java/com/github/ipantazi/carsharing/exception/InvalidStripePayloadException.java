package com.github.ipantazi.carsharing.exception;

public class InvalidStripePayloadException extends RuntimeException {
    public InvalidStripePayloadException(String message) {
        super(message);
    }

    public InvalidStripePayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
