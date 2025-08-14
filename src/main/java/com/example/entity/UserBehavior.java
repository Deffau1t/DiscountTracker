package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_behaviors")
@Data
public class UserBehavior {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "behavior_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private BehaviorType behaviorType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum BehaviorType {
        VIEW,           // Просмотр товара
        WATCH_ADD,      // Добавление в список наблюдения
        WATCH_REMOVE,   // Удаление из списка наблюдения
        NOTIFICATION_CLICK // Клик по уведомлению
    }
}
