package com.smartloan.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LoanStatus {
    PENDING_ACCEPTANCE("pending_acceptance"),
    ACTIVE("active"),
    OVERDUE("overdue"),
    COMPLETED("completed"),
    DECLINED("declined"),
    CANCELLED("cancelled");

    private final String value;

    LoanStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LoanStatus fromValue(String value) {
        for (LoanStatus status : LoanStatus.values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
