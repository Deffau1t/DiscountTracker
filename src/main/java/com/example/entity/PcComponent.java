package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность для комплектующих ПК
 * Расширяет Product с техническими характеристиками
 */
@Entity
@Table(name = "pc_components")
@Data
public class PcComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComponentType componentType;

    // Общие характеристики
    @Column(name = "manufacturer")
    private String manufacturer; // Производитель (Intel, AMD, ASUS, etc.)

    @Column(name = "model")
    private String model; // Модель

    // CPU характеристики
    @Column(name = "socket")
    private String socket; // Сокет (LGA1700, AM4, AM5)

    @Column(name = "cores")
    private Integer cores; // Количество ядер

    @Column(name = "threads")
    private Integer threads; // Количество потоков

    @Column(name = "base_clock")
    private BigDecimal baseClock; // Базовая частота (GHz)

    @Column(name = "boost_clock")
    private BigDecimal boostClock; // Турбо частота (GHz)

    @Column(name = "tdp")
    private Integer tdp; // Тепловыделение (W)

    // GPU характеристики
    @Column(name = "vram")
    private Integer vram; // Объем видеопамяти (GB)

    @Column(name = "vram_type")
    private String vramType; // Тип видеопамяти (GDDR6, GDDR6X)

    @Column(name = "memory_bandwidth")
    private Integer memoryBandwidth; // Пропускная способность памяти

    // Motherboard характеристики
    @Column(name = "form_factor")
    private String formFactor; // Форм-фактор (ATX, mATX, ITX)

    @Column(name = "chipset")
    private String chipset; // Чипсет (B650, Z790, etc.)

    @Column(name = "memory_slots")
    private Integer memorySlots; // Количество слотов памяти

    @Column(name = "max_memory")
    private Integer maxMemory; // Максимальный объем памяти (GB)

    @Column(name = "memory_type")
    private String memoryType; // Тип памяти (DDR4, DDR5)

    @Column(name = "pcie_slots")
    private Integer pcieSlots; // Количество PCIe слотов

    // RAM характеристики
    @Column(name = "capacity")
    private Integer capacity; // Объем (GB)

    @Column(name = "speed")
    private Integer speed; // Частота (MHz)

    @Column(name = "latency")
    private String latency; // Тайминги (например, CL16)

    // Storage характеристики
    @Column(name = "capacity_gb")
    private Integer capacityGb; // Объем (GB)

    @Column(name = "storage_type")
    private String storageType; // Тип (SSD, HDD, NVMe)

    @Column(name = "read_speed")
    private Integer readSpeed; // Скорость чтения (MB/s)

    @Column(name = "write_speed")
    private Integer writeSpeed; // Скорость записи (MB/s)

    @Column(name = "interface_type")
    private String interfaceType; // Интерфейс (SATA, NVMe, PCIe)

    // PSU характеристики
    @Column(name = "wattage")
    private Integer wattage; // Мощность (W)

    @Column(name = "efficiency_rating")
    private String efficiencyRating; // Рейтинг эффективности (80+ Bronze, Gold, etc.)

    @Column(name = "modular")
    private Boolean modular; // Модульный блок питания

    // Case характеристики
    @Column(name = "case_form_factor")
    private String caseFormFactor; // Поддерживаемые форм-факторы

    @Column(name = "max_gpu_length")
    private Integer maxGpuLength; // Максимальная длина видеокарты (mm)

    @Column(name = "max_cpu_cooler_height")
    private Integer maxCpuCoolerHeight; // Максимальная высота кулера (mm)

    @Column(name = "fan_slots")
    private Integer fanSlots; // Количество слотов для вентиляторов

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ComponentType {
        CPU("Процессор"),
        GPU("Видеокарта"),
        MOTHERBOARD("Материнская плата"),
        RAM("Оперативная память"),
        STORAGE("Накопитель"),
        PSU("Блок питания"),
        CASE("Корпус"),
        COOLER("Кулер процессора");

        private final String displayName;

        ComponentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

