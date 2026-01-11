package com.example.service;

import com.example.entity.*;
import com.example.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для рекомендаций комплектующих для сборки ПК
 * Интегрируется с существующей системой рекомендаций
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PcRecommendationService {

    private final PcComponentRepository pcComponentRepository;
    private final PcBuildRepository pcBuildRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final NotificationRepository notificationRepository;
    private final ProductRecommendationRepository productRecommendationRepository;
    private final ProductRepository productRepository;
    private final PcComponentService pcComponentService;

    /**
     * Рекомендует комплектующие на основе существующей сборки
     */
    public Map<PcComponent.ComponentType, List<PcComponent>> recommendComponentsForBuild(
            PcBuild currentBuild, User user) {
        
        try {
            Map<PcComponent.ComponentType, List<PcComponent>> recommendations = new HashMap<>();
            
            // Получаем рекомендации на основе текущей сборки
            PcComponent motherboard = currentBuild != null ? currentBuild.getMotherboard() : null;
            PcComponent cpu = currentBuild != null ? currentBuild.getCpu() : null;
            
            // Рекомендации CPU на основе материнской платы
            if (motherboard != null && motherboard.getSocket() != null) {
                List<PcComponent> cpuRecommendations = recommendCPU(motherboard, user);
                recommendations.put(PcComponent.ComponentType.CPU, cpuRecommendations);
            }
            
            // Рекомендации материнской платы на основе CPU
            if (cpu != null && cpu.getSocket() != null) {
                List<PcComponent> mbRecommendations = recommendMotherboard(cpu, user);
                recommendations.put(PcComponent.ComponentType.MOTHERBOARD, mbRecommendations);
            }
            
            // Рекомендации RAM на основе материнской платы
            if (motherboard != null && motherboard.getMemoryType() != null) {
                List<PcComponent> ramRecommendations = recommendRAM(motherboard, user);
                recommendations.put(PcComponent.ComponentType.RAM, ramRecommendations);
            }
            
            // Рекомендации GPU
            List<PcComponent> gpuRecommendations = recommendGPU(user, currentBuild);
            recommendations.put(PcComponent.ComponentType.GPU, gpuRecommendations);
            
            // Рекомендации Storage
            List<PcComponent> storageRecommendations = recommendStorage(user);
            recommendations.put(PcComponent.ComponentType.STORAGE, storageRecommendations);
            
            // Рекомендации PSU на основе потребляемой мощности
            List<PcComponent> psuRecommendations = recommendPSU(currentBuild, user);
            recommendations.put(PcComponent.ComponentType.PSU, psuRecommendations);
            
            // Рекомендации Case на основе материнской платы
            if (motherboard != null && motherboard.getFormFactor() != null) {
                List<PcComponent> caseRecommendations = recommendCase(motherboard, user);
                recommendations.put(PcComponent.ComponentType.CASE, caseRecommendations);
            }
            
            // Если сборка пустая, добавляем общие рекомендации для всех компонентов
            if (currentBuild == null || (motherboard == null && cpu == null)) {
                // Рекомендации для пустой сборки - все доступные компоненты
                for (PcComponent.ComponentType type : PcComponent.ComponentType.values()) {
                    if (!recommendations.containsKey(type)) {
                        List<PcComponent> components = pcComponentRepository.findByComponentType(type);
                        if (!components.isEmpty()) {
                            recommendations.put(type, components.stream().limit(5).collect(Collectors.toList()));
                        }
                    }
                }
            }
            
            return recommendations;
        } catch (Exception e) {
            log.error("Ошибка при генерации рекомендаций для сборки", e);
            return new HashMap<>();
        }
    }

    /**
     * Рекомендует CPU на основе материнской платы
     */
    private List<PcComponent> recommendCPU(PcComponent motherboard, User user) {
        // Находим совместимые CPU
        List<PcComponent> compatibleCPUs = pcComponentRepository
                .findCompatibleCPUs(PcComponent.ComponentType.CPU, motherboard.getSocket());
        
        // Сортируем по рекомендациям пользователя
        return sortByUserPreferences(compatibleCPUs, user, PcComponent.ComponentType.CPU);
    }

    /**
     * Рекомендует материнскую плату на основе CPU
     */
    private List<PcComponent> recommendMotherboard(PcComponent cpu, User user) {
        List<PcComponent> compatibleMBs = pcComponentRepository
                .findCompatibleMotherboards(PcComponent.ComponentType.MOTHERBOARD, cpu.getSocket());
        
        return sortByUserPreferences(compatibleMBs, user, PcComponent.ComponentType.MOTHERBOARD);
    }

    /**
     * Рекомендует RAM на основе материнской платы
     */
    private List<PcComponent> recommendRAM(PcComponent motherboard, User user) {
        List<PcComponent> compatibleRAM = pcComponentRepository
                .findCompatibleRAM(PcComponent.ComponentType.RAM, motherboard.getMemoryType());
        
        return sortByUserPreferences(compatibleRAM, user, PcComponent.ComponentType.RAM);
    }

    /**
     * Рекомендует GPU на основе пользовательских предпочтений
     */
    private List<PcComponent> recommendGPU(User user, PcBuild currentBuild) {
        // Получаем GPU из отслеживаемых товаров пользователя
        List<PcComponent> userTrackedGPUs = getTrackedComponents(user, PcComponent.ComponentType.GPU);
        
        // Получаем GPU из рекомендаций
        List<PcComponent> recommendedGPUs = getRecommendedComponents(user, PcComponent.ComponentType.GPU);
        
        // Объединяем и сортируем
        Set<PcComponent> allGPUs = new HashSet<>(userTrackedGPUs);
        allGPUs.addAll(recommendedGPUs);
        
        // Если нет пользовательских, получаем популярные
        if (allGPUs.isEmpty()) {
            List<PcComponent> components = pcComponentRepository.findByComponentType(PcComponent.ComponentType.GPU);
            // Сортируем в сервисе после загрузки цен
            allGPUs.addAll(components.stream().limit(10).collect(Collectors.toSet()));
        }
        
        return new ArrayList<>(allGPUs).stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Рекомендует Storage
     */
    private List<PcComponent> recommendStorage(User user) {
        List<PcComponent> userTracked = getTrackedComponents(user, PcComponent.ComponentType.STORAGE);
        List<PcComponent> recommended = getRecommendedComponents(user, PcComponent.ComponentType.STORAGE);
        
        Set<PcComponent> all = new HashSet<>(userTracked);
        all.addAll(recommended);
        
        if (all.isEmpty()) {
            List<PcComponent> components = pcComponentRepository.findByComponentType(PcComponent.ComponentType.STORAGE);
            // Сортируем в сервисе после загрузки цен
            all.addAll(components.stream().limit(10).collect(Collectors.toSet()));
        }
        
        return new ArrayList<>(all).stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Рекомендует PSU на основе потребляемой мощности сборки
     */
    private List<PcComponent> recommendPSU(PcBuild currentBuild, User user) {
        // Вычисляем требуемую мощность
        int requiredWattage = calculateRequiredWattage(currentBuild);
        int recommendedWattage = (int) (requiredWattage * 1.25); // 25% запас
        
        List<PcComponent> compatiblePSUs = pcComponentRepository
                .findCompatiblePSUs(PcComponent.ComponentType.PSU, recommendedWattage);
        
        return sortByUserPreferences(compatiblePSUs, user, PcComponent.ComponentType.PSU);
    }

    /**
     * Рекомендует Case на основе форм-фактора материнской платы
     */
    private List<PcComponent> recommendCase(PcComponent motherboard, User user) {
        List<PcComponent> compatibleCases = pcComponentRepository
                .findCompatibleCases(PcComponent.ComponentType.CASE, motherboard.getFormFactor());
        
        return sortByUserPreferences(compatibleCases, user, PcComponent.ComponentType.CASE);
    }

    /**
     * Получает комплектующие из отслеживаемых товаров пользователя
     */
    private List<PcComponent> getTrackedComponents(User user, PcComponent.ComponentType componentType) {
        // Используем сервис для автоматического создания компонентов из отслеживаемых товаров
        List<PcComponent> allTracked = pcComponentService.getComponentsFromUserTrackedProducts(user);
        return allTracked.stream()
                .filter(c -> c.getComponentType() == componentType)
                .collect(Collectors.toList());
    }

    /**
     * Получает комплектующие из рекомендаций пользователя
     */
    private List<PcComponent> getRecommendedComponents(User user, PcComponent.ComponentType componentType) {
        // Используем сервис для автоматического создания компонентов из рекомендаций
        List<PcComponent> allRecommended = pcComponentService.getComponentsFromUserRecommendations(user);
        return allRecommended.stream()
                .filter(c -> c.getComponentType() == componentType)
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Сортирует компоненты по пользовательским предпочтениям
     */
    private List<PcComponent> sortByUserPreferences(List<PcComponent> components, User user, 
                                                    PcComponent.ComponentType type) {
        // Получаем рекомендации для этих компонентов
        Map<Long, BigDecimal> scores = new HashMap<>();
        
        for (PcComponent component : components) {
            if (component.getProduct() != null) {
                productRecommendationRepository
                        .findByUserIdAndProductId(user.getId(), component.getProduct().getId())
                        .ifPresent(rec -> scores.put(component.getId(), rec.getScore()));
            }
        }
        
        // Сортируем по скору рекомендаций, затем по цене
        return components.stream()
                .sorted((c1, c2) -> {
                    BigDecimal score1 = scores.getOrDefault(c1.getId(), BigDecimal.ZERO);
                    BigDecimal score2 = scores.getOrDefault(c2.getId(), BigDecimal.ZERO);
                    
                    int scoreCompare = score2.compareTo(score1);
                    if (scoreCompare != 0) return scoreCompare;
                    
                    // Если скоры равны, сортируем по цене
                    BigDecimal price1 = c1.getProduct() != null && c1.getProduct().getCurrentPrice() != null
                            ? c1.getProduct().getCurrentPrice() : BigDecimal.valueOf(Long.MAX_VALUE);
                    BigDecimal price2 = c2.getProduct() != null && c2.getProduct().getCurrentPrice() != null
                            ? c2.getProduct().getCurrentPrice() : BigDecimal.valueOf(Long.MAX_VALUE);
                    
                    return price1.compareTo(price2);
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Вычисляет требуемую мощность для сборки
     */
    private int calculateRequiredWattage(PcBuild build) {
        int wattage = 0;
        
        if (build.getCpu() != null && build.getCpu().getTdp() != null) {
            wattage += build.getCpu().getTdp();
        }
        
        if (build.getGpu() != null && build.getGpu().getTdp() != null) {
            wattage += build.getGpu().getTdp();
        }
        
        // Запас для остальных компонентов
        wattage += 100;
        
        return wattage;
    }

    /**
     * Генерирует готовую сборку на основе предпочтений пользователя
     */
    public PcBuild generateBuildFromPreferences(User user, String buildName) {
        try {
            PcBuild build = new PcBuild();
            build.setUser(user);
            build.setBuildName(buildName != null ? buildName : "Авто-сборка");
            
            // Получаем рекомендации для каждой категории компонентов
            Map<PcComponent.ComponentType, List<PcComponent>> recommendations = 
                    recommendComponentsForBuild(build, user);
            
            if (recommendations == null || recommendations.isEmpty()) {
                log.warn("Нет рекомендаций для генерации сборки для пользователя {}", user.getEmail());
                return build; // Возвращаем пустую сборку
            }
            
            // Выбираем лучшие компоненты
            List<PcComponent> cpuList = recommendations.getOrDefault(PcComponent.ComponentType.CPU, Collections.emptyList());
            if (!cpuList.isEmpty() && cpuList.get(0) != null) {
                build.setCpu(cpuList.get(0));
            }
            
            List<PcComponent> mbList = recommendations.getOrDefault(PcComponent.ComponentType.MOTHERBOARD, Collections.emptyList());
            if (!mbList.isEmpty() && mbList.get(0) != null) {
                build.setMotherboard(mbList.get(0));
            }
            
            List<PcComponent> ramList = recommendations.getOrDefault(PcComponent.ComponentType.RAM, Collections.emptyList());
            if (!ramList.isEmpty() && ramList.get(0) != null) {
                build.setRam(ramList.get(0));
            }
            
            List<PcComponent> gpuList = recommendations.getOrDefault(PcComponent.ComponentType.GPU, Collections.emptyList());
            if (!gpuList.isEmpty() && gpuList.get(0) != null) {
                build.setGpu(gpuList.get(0));
            }
            
            List<PcComponent> storageList = recommendations.getOrDefault(PcComponent.ComponentType.STORAGE, Collections.emptyList());
            if (!storageList.isEmpty() && storageList.get(0) != null) {
                build.setStorage(storageList.get(0));
            }
            
            List<PcComponent> psuList = recommendations.getOrDefault(PcComponent.ComponentType.PSU, Collections.emptyList());
            if (!psuList.isEmpty() && psuList.get(0) != null) {
                build.setPsu(psuList.get(0));
            }
            
            List<PcComponent> caseList = recommendations.getOrDefault(PcComponent.ComponentType.CASE, Collections.emptyList());
            if (!caseList.isEmpty() && caseList.get(0) != null) {
                build.setPcCase(caseList.get(0));
            }
            
            return build;
        } catch (Exception e) {
            log.error("Ошибка при генерации сборки из предпочтений для пользователя {}", user.getEmail(), e);
            throw e;
        }
    }
}

