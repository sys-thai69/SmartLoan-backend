package com.smartloan.dto;

import lombok.Data;

@Data
public class SendReminderRequest {
    private String scheduleId; // The specific payment due
    private String customMessage; // Optional custom message
}
