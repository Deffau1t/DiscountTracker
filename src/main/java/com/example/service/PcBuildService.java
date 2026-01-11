package com.example.service;

import com.example.entity.PcBuild;
import com.example.entity.PcComponent;
import com.example.entity.User;
import com.example.repository.PcBuildRepository;
import com.example.repository.PcComponentRepository;
import com.example.repository.ProductRepository;
import com.example.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления сборками ПК
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PcBuildService {

    private final PcBuildRepository pcBuildRepository;
    private final PcComponentRepository pcComponentRepository;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PcCompatibilityService compatibilityService;

    /**
     * Создает новую сборку для пользователя
     */
    @Transactional
    public PcBuild createBuild(User user, String buildName) {
        PcBuild build = new PcBuild();
        build.setUser(user);
        build.setBuildName(buildName != null ? buildName : "Новая сборка");
        build.setBuildStatus(PcBuild.BuildStatus.DRAFT);
        
        return pcBuildRepository.save(build);
    }

    /**
     * Сохраняет или обновляет сборку
     */
    @Transactional
    public PcBuild saveBuild(PcBuild build) {
        // Обновляем цены компонентов
        updateComponentPrices(build);
        
        // Проверяем совместимость
        var compatibilityResult = compatibilityService.checkCompatibility(build);
        build.setCompatibilityChecked(true);
        
        if (compatibilityResult.isCompatible()) {
            build.setBuildStatus(PcBuild.BuildStatus.COMPLETE);
            build.setCompatibilityIssues(null);
        } else {
            build.setBuildStatus(PcBuild.BuildStatus.INCOMPATIBLE);
            build.setCompatibilityIssues(String.join("\n", compatibilityResult.getIssues()));
        }
        
        build.calculateTotalPrice();
        return pcBuildRepository.save(build);
    }

    /**
     * Добавляет компонент в сборку
     */
    @Transactional
    public PcBuild addComponent(Long buildId, Long componentId, PcComponent.ComponentType componentType) {
        Optional<PcBuild> buildOpt = pcBuildRepository.findById(buildId);
        if (buildOpt.isEmpty()) {
            throw new IllegalArgumentException("Сборка не найдена");
        }

        Optional<PcComponent> componentOpt = pcComponentRepository.findById(componentId);
        if (componentOpt.isEmpty()) {
            throw new IllegalArgumentException("Компонент не найден");
        }

        PcBuild build = buildOpt.get();
        PcComponent component = componentOpt.get();

        // Проверяем тип компонента
        if (component.getComponentType() != componentType) {
            throw new IllegalArgumentException("Тип компонента не совпадает");
        }

        // Устанавливаем компонент в соответствующее поле
        switch (componentType) {
            case CPU -> build.setCpu(component);
            case GPU -> build.setGpu(component);
            case MOTHERBOARD -> build.setMotherboard(component);
            case RAM -> build.setRam(component);
            case STORAGE -> build.setStorage(component);
            case PSU -> build.setPsu(component);
            case CASE -> build.setPcCase(component);
            case COOLER -> build.setCooler(component);
        }

        return saveBuild(build);
    }

    /**
     * Удаляет компонент из сборки
     */
    @Transactional
    public PcBuild removeComponent(Long buildId, PcComponent.ComponentType componentType) {
        Optional<PcBuild> buildOpt = pcBuildRepository.findById(buildId);
        if (buildOpt.isEmpty()) {
            throw new IllegalArgumentException("Сборка не найдена");
        }

        PcBuild build = buildOpt.get();

        switch (componentType) {
            case CPU -> build.setCpu(null);
            case GPU -> build.setGpu(null);
            case MOTHERBOARD -> build.setMotherboard(null);
            case RAM -> build.setRam(null);
            case STORAGE -> build.setStorage(null);
            case PSU -> build.setPsu(null);
            case CASE -> build.setPcCase(null);
            case COOLER -> build.setCooler(null);
        }

        return saveBuild(build);
    }

    /**
     * Получает сборку по ID
     */
    public Optional<PcBuild> getBuild(Long buildId) {
        return pcBuildRepository.findById(buildId);
    }

    /**
     * Получает все сборки пользователя
     */
    public List<PcBuild> getUserBuilds(User user) {
        return pcBuildRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
    }

    /**
     * Получает публичные сборки
     */
    public List<PcBuild> getPublicBuilds() {
        return pcBuildRepository.findPublicBuildsOrderByPrice();
    }

    /**
     * Удаляет сборку
     */
    @Transactional
    public void deleteBuild(Long buildId, User user) {
        Optional<PcBuild> buildOpt = pcBuildRepository.findByIdAndUserId(buildId, user.getId());
        if (buildOpt.isPresent()) {
            pcBuildRepository.delete(buildOpt.get());
            log.info("Сборка {} удалена пользователем {}", buildId, user.getEmail());
        } else {
            throw new IllegalArgumentException("Сборка не найдена или доступ запрещен");
        }
    }

    /**
     * Обновляет статус публичности сборки
     */
    @Transactional
    public PcBuild setBuildPublic(Long buildId, User user, boolean isPublic) {
        Optional<PcBuild> buildOpt = pcBuildRepository.findByIdAndUserId(buildId, user.getId());
        if (buildOpt.isEmpty()) {
            throw new IllegalArgumentException("Сборка не найдена или доступ запрещен");
        }

        PcBuild build = buildOpt.get();
        build.setIsPublic(isPublic);
        return pcBuildRepository.save(build);
    }

    /**
     * Обновляет цены компонентов из истории цен
     */
    private void updateComponentPrices(PcBuild build) {
        var components = build.getComponentsMap();
        
        for (PcComponent component : components.values()) {
            if (component != null && component.getProduct() != null) {
                var latestPrices = priceHistoryRepository.findLatestPriceByProductId(component.getProduct().getId());
                if (latestPrices != null && !latestPrices.isEmpty()) {
                    component.getProduct().setCurrentPrice(latestPrices.get(0));
                }
            }
        }
    }

    /**
     * Проверяет совместимость сборки
     */
    public PcCompatibilityService.CompatibilityResult checkBuildCompatibility(PcBuild build) {
        updateComponentPrices(build);
        return compatibilityService.checkCompatibility(build);
    }
}

