package com.smartloan.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LoanFrequency {
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String value;

    LoanFrequency(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LoanFrequency fromValue(String value) {
        for (LoanFrequency freq : LoanFrequency.values()) {
            if (freq.value.equalsIgnoreCase(value) || freq.name().equalsIgnoreCase(value)) {
                return freq;
            }
        }
        throw new IllegalArgumentException("Unknown frequency: " + value);
    }
}
