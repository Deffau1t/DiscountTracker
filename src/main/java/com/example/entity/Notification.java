package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Product product;

    private boolean notified = false;
    private BigDecimal threshold;
    private LocalDateTime lastNotified;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private NotificationFrequency frequency = NotificationFrequency.IMMEDIATELY;

    private LocalDateTime nextNotificationTime;

    public enum NotificationFrequency {
        IMMEDIATELY("Мгновенно"),
        HOURLY("Раз в час"),
        DAILY("Раз в день"),
        WEEKLY("Раз в неделю");

        private final String displayName;

        NotificationFrequency(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public void updateNextNotificationTime() {
        if (this.lastNotified == null) {
            this.nextNotificationTime = null;
            return;
        }

        switch (this.frequency) {
            case IMMEDIATELY:
                this.nextNotificationTime = this.lastNotified;
                break;
            case HOURLY:
                this.nextNotificationTime = this.lastNotified.plusHours(1);
                break;
            case DAILY:
                this.nextNotificationTime = this.lastNotified.plusDays(1);
                break;
            case WEEKLY:
                this.nextNotificationTime = this.lastNotified.plusWeeks(1);
                break;
        }
    }
}