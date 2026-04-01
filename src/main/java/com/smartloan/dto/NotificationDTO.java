package com.smartloan.dto;

import com.smartloan.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String userId;
    private String title;
    private String message;
    private NotificationType type;
    private String loanId;
    private Boolean isRead;
    private String createdAt;
}
