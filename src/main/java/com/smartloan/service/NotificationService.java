package com.smartloan.service;

import com.smartloan.dto.NotificationDTO;
import com.smartloan.entity.Notification;
import com.smartloan.entity.NotificationType;
import com.smartloan.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Transactional
    public NotificationDTO createNotification(String userId, String title, String message, NotificationType type, String loanId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .loanId(loanId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notification = notificationRepository.save(notification);
        return toDTO(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public int markAsRead(String notificationId, String userId) {
        return notificationRepository.markAsRead(notificationId, userId);
    }

    @Transactional
    public int markAllAsRead(String userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Send a reminder notification for an upcoming due payment
     */
    @Transactional
    public NotificationDTO sendDueReminderNotification(String borrowerId, String lenderName, String loanId, double amount, String dueDate) {
        String title = "Payment Due Reminder";
        String message = String.format("Reminder: You have a payment of $%.2f due on %s from loan with %s", amount, dueDate, lenderName);
        return createNotification(borrowerId, title, message, NotificationType.PAYMENT_REMINDER, loanId);
    }

    /**
     * Send a notification for an overdue payment
     */
    @Transactional
    public NotificationDTO sendOverdueNotification(String borrowerId, String lenderName, String loanId, double amount, int daysOverdue) {
        String title = "Payment Overdue!";
        String message = String.format("Your payment of $%.2f to %s is %d days overdue. Please settle this immediately.", amount, lenderName, daysOverdue);
        return createNotification(borrowerId, title, message, NotificationType.OVERDUE_ALERT, loanId);
    }

    /**
     * Send a notification to lender that borrower is overdue
     */
    @Transactional
    public NotificationDTO sendLenderOverdueNotification(String lenderId, String borrowerName, String loanId, double amount, int daysOverdue) {
        String title = "Loan Payment Overdue";
        String message = String.format("Borrower %s owes $%.2f and is %d days overdue. Consider sending a reminder.", borrowerName, amount, daysOverdue);
        return createNotification(lenderId, title, message, NotificationType.OVERDUE_ALERT, loanId);
    }

    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .loanId(notification.getLoanId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt().format(DATE_FORMATTER))
                .build();
    }
}
