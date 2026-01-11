package com.example.service;

import com.example.entity.*;
import com.example.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с компонентами ПК
 * Автоматически создает компоненты из товаров на основе категорий
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PcComponentService {

    private final PcComponentRepository pcComponentRepository;
    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;
    private final ProductRecommendationRepository productRecommendationRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * Получает или создает компонент из товара
     */
    @Transactional
    public PcComponent getOrCreateComponentFromProduct(Product product, PcComponent.ComponentType componentType) {
        // Проверяем, существует ли уже компонент для этого товара
        Optional<PcComponent> existing = pcComponentRepository.findByProductId(product.getId());
        if (existing.isPresent()) {
            PcComponent existingComponent = existing.get();
            // Если тип компонента не совпадает, обновляем его
            if (existingComponent.getComponentType() != componentType) {
                existingComponent.setComponentType(componentType);
                existingComponent = pcComponentRepository.save(existingComponent);
                if (componentType == PcComponent.ComponentType.MOTHERBOARD) {
                    log.info("MOTHERBOARD: Обновлен тип компонента ID {} для товара {}", 
                            existingComponent.getId(), product.getId());
                }
            }
            return existingComponent;
        }

        // Создаем новый компонент
        PcComponent component = new PcComponent();
        component.setProduct(product);
        component.setComponentType(componentType);
        
        // Пытаемся определить производителя и модель из названия
        String name = product.getName();
        if (name != null) {
            // Простая логика определения производителя
            String manufacturer = extractManufacturer(name);
            component.setManufacturer(manufacturer);
            component.setModel(name);
        }

        // Получаем последнюю цену
        List<BigDecimal> prices = priceHistoryRepository.findLatestPriceByProductId(product.getId());
        if (!prices.isEmpty()) {
            product.setCurrentPrice(prices.get(0));
        }

        PcComponent saved = pcComponentRepository.save(component);
        
        // Логирование только для MOTHERBOARD
        if (componentType == PcComponent.ComponentType.MOTHERBOARD) {
            log.info("MOTHERBOARD: Создан компонент ID {} для товара {} (категория: {})", 
                    saved.getId(), product.getId(), product.getCategory());
        }
        
        return saved;
    }

    /**
     * Получает компоненты из отслеживаемых товаров пользователя
     */
    public List<PcComponent> getComponentsFromUserTrackedProducts(User user) {
        List<Notification> notifications = notificationRepository.findByUserId(user.getId());
        
        List<Product> trackedProducts = notifications.stream()
                .map(Notification::getProduct)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<PcComponent> components = new ArrayList<>();
        for (Product product : trackedProducts) {
            PcComponent.ComponentType type = determineComponentType(product);
            
            if (type != null) {
                try {
                    PcComponent component = getOrCreateComponentFromProduct(product, type);
                    components.add(component);
                } catch (Exception e) {
                    log.error("Не удалось создать компонент из товара {}: {}", product.getId(), e.getMessage());
                }
            }
        }

        return components;
    }

    /**
     * Получает компоненты из рекомендаций пользователя
     */
    public List<PcComponent> getComponentsFromUserRecommendations(User user) {
        List<ProductRecommendation> recommendations = productRecommendationRepository
                .findByUserIdOrderByScoreDesc(user.getId());

        List<PcComponent> components = new ArrayList<>();
        for (ProductRecommendation rec : recommendations) {
            Product product = rec.getProduct();
            if (product != null) {
                PcComponent.ComponentType type = determineComponentType(product);
                if (type != null) {
                    try {
                        PcComponent component = getOrCreateComponentFromProduct(product, type);
                        components.add(component);
                    } catch (Exception e) {
                        log.warn("Не удалось создать компонент из рекомендации {}: {}", product.getId(), e.getMessage());
                    }
                }
            }
        }

        return components;
    }

    /**
     * Получает компоненты по типу, включая созданные из отслеживаемых товаров и рекомендаций
     */
    public List<PcComponent> getComponentsByTypeWithAutoCreate(PcComponent.ComponentType type, User user) {
            // Получаем существующие компоненты из БД
            List<PcComponent> components = pcComponentRepository.findByComponentType(type);
            
            // Логирование только для MOTHERBOARD для отладки
            if (type == PcComponent.ComponentType.MOTHERBOARD) {
                log.info("MOTHERBOARD: Найдено {} компонентов в БД", components.size());
            }

            // Если есть пользователь, добавляем из отслеживаемых товаров
            if (user != null) {
                List<PcComponent> fromTracked = getComponentsFromUserTrackedProducts(user);
                List<PcComponent> fromRecs = getComponentsFromUserRecommendations(user);
                
                if (type == PcComponent.ComponentType.MOTHERBOARD) {
                    log.info("MOTHERBOARD: Получено {} компонентов из отслеживаемых, {} из рекомендаций", 
                            fromTracked.size(), fromRecs.size());
                }

                Set<Long> existingIds = components.stream()
                        .map(PcComponent::getId)
                        .collect(Collectors.toSet());

                // Фильтруем по типу и добавляем
                int addedFromTracked = 0;
                for (PcComponent component : fromTracked) {
                    if (component.getComponentType() == type && !existingIds.contains(component.getId())) {
                        components.add(component);
                        existingIds.add(component.getId());
                        addedFromTracked++;
                    }
                }
                
                int addedFromRecs = 0;
                for (PcComponent component : fromRecs) {
                    if (component.getComponentType() == type && !existingIds.contains(component.getId())) {
                        components.add(component);
                        existingIds.add(component.getId());
                        addedFromRecs++;
                    }
                }
                
                if (type == PcComponent.ComponentType.MOTHERBOARD) {
                    log.info("MOTHERBOARD: Добавлено {} из отслеживаемых, {} из рекомендаций", 
                            addedFromTracked, addedFromRecs);
                }
            }

            // Логирование только для MOTHERBOARD для отладки
            if (type == PcComponent.ComponentType.MOTHERBOARD) {
                log.info("MOTHERBOARD: Итого получено {} компонентов для пользователя {}", 
                        components.size(), user != null ? user.getEmail() : "anonymous");
            }

            return components;
        }

        /**
         * Определяет тип компонента по категории товара
         */
        private PcComponent.ComponentType determineComponentType(Product product) {
        if (product == null) {
            return null;
        }

        // Делаем безопасные строки, даже если category/name = null
        String category = product.getCategory() != null
                ? product.getCategory().toLowerCase()
                : "";
        String name = product.getName() != null
                ? product.getName().toLowerCase()
                : "";

        // 1) Жёсткое соответствие значениям из select'а (watch.html)
        switch (category) {
            case "cpu":
                return PcComponent.ComponentType.CPU;
            case "video":
            case "gpu":
                return PcComponent.ComponentType.GPU;
            case "motherboard":
                return PcComponent.ComponentType.MOTHERBOARD;
            case "ram":
                return PcComponent.ComponentType.RAM;
            case "storage":
                return PcComponent.ComponentType.STORAGE;
            case "psu":
                return PcComponent.ComponentType.PSU;
            case "case":
                return PcComponent.ComponentType.CASE;
            case "cooler":
                return PcComponent.ComponentType.COOLER;
            default:
                // идём в эвристику
                break;
        }

        // 2) Эвристики по category/name — работают даже если category пустая

        // Материнские платы
        if (category.contains("материн") || name.contains("материн")
                || category.contains("motherboard") || name.contains("motherboard")
                || name.startsWith("mb ") || name.contains(" mb ")) {
            return PcComponent.ComponentType.MOTHERBOARD;
        }

        // CPU
        if (category.contains("cpu") || category.contains("процессор")
                || name.contains("ryzen") || name.contains("processor")
                || name.contains("процессор")) {
            return PcComponent.ComponentType.CPU;
        }

        // GPU
        if (category.contains("gpu") || category.contains("видеокарт") || category.equals("video")
                || name.contains("rtx") || name.contains("gtx") || name.contains("radeon")
                || name.contains("graphics") || name.contains("video card")
                || name.contains("видеокарт")) {
            return PcComponent.ComponentType.GPU;
        }

        // RAM
        if (category.contains("ram") || category.contains("память") || category.contains("memory")
                || name.contains("ddr3") || name.contains("ddr4") || name.contains("ddr5")
                || name.contains("ram") || name.contains("memory")
                || name.contains("оперативн")) {
            return PcComponent.ComponentType.RAM;
        }

        // STORAGE
        if (category.contains("storage") || category.contains("накопител")
                || category.contains("ssd") || category.contains("hdd")
                || name.contains("ssd") || name.contains("hdd")
                || name.contains("nvme") || name.contains("storage")
                || name.contains("жесткий диск")) {
            return PcComponent.ComponentType.STORAGE;
        }

        // Корпус — проверяем ДО блока питания
        if (category.contains("case") || category.contains("корпус") || category.contains("chassis")
                || name.contains("корпус") || name.contains("case") || name.contains("tower")) {
            return PcComponent.ComponentType.CASE;
        }

        // PSU
        if (category.contains("psu")
                || category.contains("блок пит") || category.contains("блоки пит")
                || name.contains("psu") || name.contains("power supply")
                || (name.contains("блок") && name.contains("питан"))) {
            return PcComponent.ComponentType.PSU;
        }

        // Кулер
        if (category.contains("cooler") || category.contains("кулер") || category.contains("cooling")
                || name.contains("cooler") || name.contains("fan")
                || name.contains("вентилятор")) {
            return PcComponent.ComponentType.COOLER;
        }

        return null;
    }


    /**
     * Извлекает производителя из названия товара
     */
    private String extractManufacturer(String name) {
        if (name == null) return null;

        String[] manufacturers = {
            "Intel", "AMD", "NVIDIA", "ASUS", "MSI", "Gigabyte", "ASRock",
            "Corsair", "G.Skill", "Kingston", "Samsung", "Western Digital",
            "Seagate", "Crucial", "EVGA", "Zotac", "PowerColor", "Sapphire",
            "Cooler Master", "be quiet!", "Noctua", "Thermaltake", "Fractal Design",
            "NZXT", "Lian Li", "Phanteks", "Silverstone"
        };

        for (String manufacturer : manufacturers) {
            if (name.contains(manufacturer)) {
                return manufacturer;
            }
        }

        // Если не нашли известного производителя, берем первое слово
        String[] words = name.split("\\s+");
        return words.length > 0 ? words[0] : null;
    }

    /**
     * Создает компоненты из всех отслеживаемых товаров пользователя
     */
    @Transactional
    public void createComponentsFromTrackedProducts(User user) {
        List<Notification> notifications = notificationRepository.findByUserId(user.getId());
        int created = 0;

        for (Notification notification : notifications) {
            Product product = notification.getProduct();
            if (product != null) {
                PcComponent.ComponentType type = determineComponentType(product);
                if (type != null) {
                    try {
                        getOrCreateComponentFromProduct(product, type);
                        created++;
                    } catch (Exception e) {
                        log.warn("Не удалось создать компонент из товара {}: {}", product.getId(), e.getMessage());
                    }
                }
            }
        }

        log.info("Создано {} компонентов из отслеживаемых товаров пользователя {}", created, user.getEmail());
    }
}

