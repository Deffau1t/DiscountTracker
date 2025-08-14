package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_recommendations")
@Data
public class ProductRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlgorithmType algorithm;

    @Column(name = "is_viewed")
    private Boolean isViewed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum AlgorithmType {
        CONTENT_BASED,    // Content-based фильтрация
        COLLABORATIVE,    // Collaborative фильтрация
        HYBRID,          // Гибридный подход
        MATRIX_FACTORIZATION, // Матричная факторизация
        CLUSTERING,      // Кластеризация пользователей
        TEMPORAL,        // Временные паттерны
        TREND_BASED,     // Рекомендации на основе трендов
        PERSONALIZED     // Персонализированные рекомендации
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
