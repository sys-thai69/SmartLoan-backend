package com.smartloan.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
    INFO("info"),
    WARNING("warning"),
    ALERT("alert"),
    PAYMENT_REMINDER("payment_reminder"),
    LOAN_REQUEST("loan_request"),
    LOAN_ACCEPTED("loan_accepted"),
    LOAN_DECLINED("loan_declined"),
    PAYMENT_RECEIVED("payment_received"),
    OVERDUE_ALERT("overdue_alert");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
