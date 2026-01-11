package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Сущность для сборки ПК
 */
@Entity
@Table(name = "pc_builds")
@Data
public class PcBuild {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "build_name")
    private String buildName; // Название сборки

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Описание сборки

    // Компоненты сборки
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cpu_id")
    private PcComponent cpu;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gpu_id")
    private PcComponent gpu;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "motherboard_id")
    private PcComponent motherboard;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ram_id")
    private PcComponent ram;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_id")
    private PcComponent storage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "psu_id")
    private PcComponent psu;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "case_id")
    private PcComponent pcCase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cooler_id")
    private PcComponent cooler;

    // Статус сборки
    @Enumerated(EnumType.STRING)
    @Column(name = "build_status")
    private BuildStatus buildStatus = BuildStatus.DRAFT;

    @Column(name = "compatibility_checked")
    private Boolean compatibilityChecked = false;

    @Column(name = "compatibility_issues", columnDefinition = "TEXT")
    private String compatibilityIssues; // JSON или текст с проблемами совместимости

    @Column(name = "total_price")
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "is_public")
    private Boolean isPublic = false; // Публичная сборка для рекомендаций

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum BuildStatus {
        DRAFT("Черновик"),
        COMPLETE("Готово"),
        INCOMPATIBLE("Несовместимо"),
        OPTIMIZING("Оптимизация");

        private final String displayName;

        BuildStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Получает карту компонентов для удобного доступа
     */
    public Map<PcComponent.ComponentType, PcComponent> getComponentsMap() {
        Map<PcComponent.ComponentType, PcComponent> map = new HashMap<>();
        if (cpu != null) map.put(PcComponent.ComponentType.CPU, cpu);
        if (gpu != null) map.put(PcComponent.ComponentType.GPU, gpu);
        if (motherboard != null) map.put(PcComponent.ComponentType.MOTHERBOARD, motherboard);
        if (ram != null) map.put(PcComponent.ComponentType.RAM, ram);
        if (storage != null) map.put(PcComponent.ComponentType.STORAGE, storage);
        if (psu != null) map.put(PcComponent.ComponentType.PSU, psu);
        if (pcCase != null) map.put(PcComponent.ComponentType.CASE, pcCase);
        if (cooler != null) map.put(PcComponent.ComponentType.COOLER, cooler);
        return map;
    }

    /**
     * Вычисляет общую стоимость сборки
     */
    public void calculateTotalPrice() {
        BigDecimal total = BigDecimal.ZERO;
        if (cpu != null && cpu.getProduct() != null && cpu.getProduct().getCurrentPrice() != null) {
            total = total.add(cpu.getProduct().getCurrentPrice());
        }
        if (gpu != null && gpu.getProduct() != null && gpu.getProduct().getCurrentPrice() != null) {
            total = total.add(gpu.getProduct().getCurrentPrice());
        }
        if (motherboard != null && motherboard.getProduct() != null && motherboard.getProduct().getCurrentPrice() != null) {
            total = total.add(motherboard.getProduct().getCurrentPrice());
        }
        if (ram != null && ram.getProduct() != null && ram.getProduct().getCurrentPrice() != null) {
            total = total.add(ram.getProduct().getCurrentPrice());
        }
        if (storage != null && storage.getProduct() != null && storage.getProduct().getCurrentPrice() != null) {
            total = total.add(storage.getProduct().getCurrentPrice());
        }
        if (psu != null && psu.getProduct() != null && psu.getProduct().getCurrentPrice() != null) {
            total = total.add(psu.getProduct().getCurrentPrice());
        }
        if (pcCase != null && pcCase.getProduct() != null && pcCase.getProduct().getCurrentPrice() != null) {
            total = total.add(pcCase.getProduct().getCurrentPrice());
        }
        if (cooler != null && cooler.getProduct() != null && cooler.getProduct().getCurrentPrice() != null) {
            total = total.add(cooler.getProduct().getCurrentPrice());
        }
        this.totalPrice = total;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotalPrice();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateTotalPrice();
    }
}

