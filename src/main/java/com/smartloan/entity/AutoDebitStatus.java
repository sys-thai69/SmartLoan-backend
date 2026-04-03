package com.smartloan.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AutoDebitStatus {
    ACTIVE("ACTIVE"),
    PAUSED("PAUSED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    AutoDebitStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AutoDebitStatus fromValue(String value) {
        for (AutoDebitStatus status : AutoDebitStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid AutoDebitStatus: " + value);
    }
}
