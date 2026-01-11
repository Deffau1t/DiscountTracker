package com.example.controller;

import com.example.dto.PcBuildDto;
import com.example.dto.PcComponentDto;
import com.example.service.PcComponentService;
import com.example.entity.PcBuild;
import com.example.entity.PcComponent;
import com.example.entity.User;
import com.example.repository.PcComponentRepository;
import com.example.repository.UserRepository;
import com.example.repository.PriceHistoryRepository;
import com.example.service.PcBuildService;
import com.example.service.PcCompatibilityService;
import com.example.service.PcRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для модуля сборки ПК
 */
@Controller
@RequestMapping("/pc-build")
@RequiredArgsConstructor
@Slf4j
public class PcBuildController {

    private final PcBuildService pcBuildService;
    private final PcRecommendationService pcRecommendationService;
    private final PcCompatibilityService compatibilityService;
    private final PcComponentRepository pcComponentRepository;
    private final UserRepository userRepository;
    private final PcComponentService pcComponentService;
    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * Главная страница модуля сборки ПК
     */
    @GetMapping
    public String pcBuildHome(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<PcBuild> builds = pcBuildService.getUserBuilds(user);
        
        model.addAttribute("builds", builds);
        model.addAttribute("componentTypes", PcComponent.ComponentType.values());
        
        return "pc-build/home";
    }

    /**
     * Создание новой сборки
     */
    @PostMapping("/create")
    public String createBuild(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(required = false) String buildName,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        PcBuild build = pcBuildService.createBuild(user, buildName);
        
        redirectAttributes.addFlashAttribute("success", "Сборка создана");
        return "redirect:/pc-build/" + build.getId();
    }

    /**
     * Страница редактирования сборки
     */
    @GetMapping("/{buildId}")
    public String editBuild(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long buildId,
                           Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        Optional<PcBuild> buildOpt = pcBuildService.getBuild(buildId);
        
        if (buildOpt.isEmpty()) {
            return "redirect:/pc-build";
        }

        PcBuild build = buildOpt.get();
        
        // Проверяем доступ
        if (build.getUser() == null || !build.getUser().getId().equals(user.getId())) {
            return "redirect:/pc-build";
        }

        // Получаем рекомендации для сборки
        Map<PcComponent.ComponentType, List<PcComponent>> recommendations = 
                pcRecommendationService.recommendComponentsForBuild(build, user);

        // Проверяем совместимость
        PcCompatibilityService.CompatibilityResult compatibilityResult = 
                compatibilityService.checkCompatibility(build);

        // Получаем доступные компоненты по типам (с автоматическим созданием из отслеживаемых товаров)
        Map<PcComponent.ComponentType, List<PcComponent>> availableComponents = new HashMap<>();
        for (PcComponent.ComponentType type : PcComponent.ComponentType.values()) {
            List<PcComponent> components = pcComponentService.getComponentsByTypeWithAutoCreate(type, user);
            availableComponents.put(type, components);
        }

        PcBuildDto buildDto;
        try {
            buildDto = PcBuildDto.fromEntity(build);
        } catch (Exception e) {
            log.error("Ошибка создания DTO: {}", e.getMessage(), e);
            buildDto = new PcBuildDto();
            buildDto.setId(build.getId());
            buildDto.setBuildName(build.getBuildName());
            buildDto.setDescription(build.getDescription());
            buildDto.setBuildStatus(build.getBuildStatus() != null ? build.getBuildStatus().name() : null);
            buildDto.setCompatibilityChecked(build.getCompatibilityChecked());
            buildDto.setCompatibilityIssues(build.getCompatibilityIssues());
            buildDto.setTotalPrice(build.getTotalPrice());
            buildDto.setIsPublic(build.getIsPublic());
        }
        model.addAttribute("build", buildDto);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("availableComponents", availableComponents);
        model.addAttribute("compatibilityResult", compatibilityResult);
        model.addAttribute("componentTypes", PcComponent.ComponentType.values());

        return "pc-build/edit";
    }

    /**
     * Добавление компонента в сборку
     */
    @PostMapping("/{buildId}/add-component")
    @ResponseBody
    public Map<String, Object> addComponent(@AuthenticationPrincipal UserDetails userDetails,
                                            @PathVariable Long buildId,
                                            @RequestParam Long componentId,
                                            @RequestParam String componentType) {
        Map<String, Object> response = new HashMap<>();
        
        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Не авторизован");
            return response;
        }

        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Пользователь не найден");
                return response;
            }

            User user = userOpt.get();
            Optional<PcBuild> buildOpt = pcBuildService.getBuild(buildId);
            
            if (buildOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Сборка не найдена");
                return response;
            }

            PcBuild build = buildOpt.get();
            if (!build.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return response;
            }

            PcComponent.ComponentType type = PcComponent.ComponentType.valueOf(componentType);
            pcBuildService.addComponent(buildId, componentId, type);
            
            response.put("success", true);
            response.put("message", "Компонент добавлен");
        } catch (Exception e) {
            log.error("Ошибка при добавлении компонента", e);
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }

        return response;
    }

    /**
     * Удаление компонента из сборки
     */
    @PostMapping("/{buildId}/remove-component")
    @ResponseBody
    public Map<String, Object> removeComponent(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable Long buildId,
                                              @RequestParam String componentType) {
        Map<String, Object> response = new HashMap<>();
        
        if (userDetails == null) {
            response.put("success", false);
            response.put("message", "Не авторизован");
            return response;
        }

        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Пользователь не найден");
                return response;
            }

            User user = userOpt.get();
            Optional<PcBuild> buildOpt = pcBuildService.getBuild(buildId);
            
            if (buildOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Сборка не найдена");
                return response;
            }

            PcBuild build = buildOpt.get();
            if (!build.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Доступ запрещен");
                return response;
            }

            PcComponent.ComponentType type = PcComponent.ComponentType.valueOf(componentType);
            pcBuildService.removeComponent(buildId, type);
            
            response.put("success", true);
            response.put("message", "Компонент удален");
        } catch (Exception e) {
            log.error("Ошибка при удалении компонента", e);
            response.put("success", false);
            response.put("message", "Ошибка: " + e.getMessage());
        }

        return response;
    }

    /**
     * Генерация готовой сборки
     */
    @PostMapping("/generate")
    public String generateBuild(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(required = false) String buildName,
                               RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        try {
            User user = userOpt.get();
            
            // Создаем компоненты из отслеживаемых товаров пользователя перед генерацией сборки
            pcComponentService.createComponentsFromTrackedProducts(user);
            
            String name = buildName != null && !buildName.trim().isEmpty() 
                    ? buildName.trim() 
                    : "Авто-сборка " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            
            PcBuild build = pcRecommendationService.generateBuildFromPreferences(user, name);
            
            if (build == null) {
                redirectAttributes.addFlashAttribute("error", "Не удалось сгенерировать сборку. Возможно, недостаточно данных для рекомендаций.");
                return "redirect:/pc-build";
            }
            
            PcBuild saved = pcBuildService.saveBuild(build);
            
            redirectAttributes.addFlashAttribute("success", "Сборка сгенерирована");
            return "redirect:/pc-build/" + saved.getId();
        } catch (Exception e) {
            log.error("Ошибка при генерации сборки", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при генерации сборки: " + e.getMessage());
            return "redirect:/pc-build";
        }
    }

    /**
     * Удаление сборки
     */
    @PostMapping("/{buildId}/delete")
    public String deleteBuild(@AuthenticationPrincipal UserDetails userDetails,
                             @PathVariable Long buildId,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        pcBuildService.deleteBuild(buildId, user);
        
        redirectAttributes.addFlashAttribute("success", "Сборка удалена");
        return "redirect:/pc-build";
    }

    /**
     * REST API для получения компонентов по типу
     */
    @GetMapping("/api/components/{type}")
    @ResponseBody
    public List<PcComponentDto> getComponentsByType(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable String type) {
        try {
            PcComponent.ComponentType componentType = PcComponent.ComponentType.valueOf(type.toUpperCase());
            
            User user = null;
            if (userDetails != null) {
                Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
                user = userOpt.orElse(null);
            }
            
            List<PcComponent> components = pcComponentService.getComponentsByTypeWithAutoCreate(componentType, user);
            
            // Для каждого компонента загружаем цену, если не загружена
            for (PcComponent component : components) {
                if (component.getProduct() != null && component.getProduct().getCurrentPrice() == null) {
                    List<java.math.BigDecimal> prices = priceHistoryRepository.findLatestPriceByProductId(component.getProduct().getId());
                    if (!prices.isEmpty()) {
                        component.getProduct().setCurrentPrice(prices.get(0));
                    }
                }
            }
            
            // Преобразуем в DTO
            List<PcComponentDto> dtos = components.stream()
                    .map(PcComponentDto::fromEntity)
                    .collect(Collectors.toList());
            
            // Логирование только для MOTHERBOARD для отладки
            if (componentType == PcComponent.ComponentType.MOTHERBOARD) {
                log.info("MOTHERBOARD API: Запрос типа {}, получено {} компонентов, возвращено {} DTO", 
                        type, components.size(), dtos.size());
            }
            
            return dtos;
        } catch (IllegalArgumentException e) {
            log.error("Неверный тип компонента: {}", type, e);
            return List.of();
        } catch (Exception e) {
            log.error("Ошибка при получении компонентов типа {}: {}", type, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * REST API для получения рекомендаций для сборки
     */
    @GetMapping("/api/{buildId}/recommendations")
    @ResponseBody
    public Map<String, List<PcComponent>> getRecommendations(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long buildId) {
        if (userDetails == null) {
            return Map.of();
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return Map.of();
        }

        Optional<PcBuild> buildOpt = pcBuildService.getBuild(buildId);
        if (buildOpt.isEmpty()) {
            return Map.of();
        }

        User user = userOpt.get();
        PcBuild build = buildOpt.get();
        
        Map<PcComponent.ComponentType, List<PcComponent>> recommendations = 
                pcRecommendationService.recommendComponentsForBuild(build, user);
        
        return recommendations.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));
    }
}

