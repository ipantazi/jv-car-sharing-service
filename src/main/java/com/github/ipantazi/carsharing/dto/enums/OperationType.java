package com.github.ipantazi.carsharing.dto.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OperationType {
    INCREASE,
    DECREASE,
    SET;

    @JsonCreator
    public static OperationType from(String value) {
        try {
            return value == null ? null : OperationType.valueOf(value.toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }
}
