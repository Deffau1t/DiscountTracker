package com.example.controller;

import com.example.dto.RecommendationDto;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/demo")
@RequiredArgsConstructor
@Slf4j
public class DemoController {

    private final RecommendationService recommendationService;

    /**
     * Демонстрационная страница рекомендаций
     */
    @GetMapping("/recommendations")
    public String showDemoRecommendations(Model model) {
        log.info("Показана демонстрационная страница рекомендаций");
        
        // Создаем демо-пользователя
        User demoUser = createDemoUser();
        
        try {
            // Получаем рекомендации для демо-пользователя
            List<RecommendationDto> recommendations = recommendationService.generateRecommendations(demoUser, 10);
            model.addAttribute("recommendations", recommendations);
            model.addAttribute("user", demoUser);
            model.addAttribute("unviewedCount", recommendations.size());
            
        } catch (Exception e) {
            log.error("Ошибка при генерации демо-рекомендаций", e);
            // Создаем демо-рекомендации вручную
            List<RecommendationDto> demoRecommendations = createDemoRecommendations();
            model.addAttribute("recommendations", demoRecommendations);
            model.addAttribute("user", demoUser);
            model.addAttribute("unviewedCount", demoRecommendations.size());
        }

        return "demo-recommendations";
    }

    /**
     * REST API для генерации демо-рекомендаций
     */
    @PostMapping("/api/generate")
    @ResponseBody
    public List<RecommendationDto> generateDemoRecommendations() {
        log.info("Запрос на генерацию демо-рекомендаций");
        
        User demoUser = createDemoUser();
        
        try {
            return recommendationService.generateRecommendations(demoUser, 10);
        } catch (Exception e) {
            log.error("Ошибка при генерации демо-рекомендаций", e);
            return createDemoRecommendations();
        }
    }

    /**
     * Создает демо-пользователя
     */
    private User createDemoUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("demo@example.com");
        user.setRole("USER");
        return user;
    }

    /**
     * Создает демо-рекомендации вручную
     */
    private List<RecommendationDto> createDemoRecommendations() {
        List<RecommendationDto> recommendations = new ArrayList<>();
        
        // Демо-рекомендации для разных алгоритмов
        recommendations.add(createDemoRecommendation(1L, "MacBook Pro 16", "electronics", "apple.com", 0.95, "CONTENT_BASED"));
        recommendations.add(createDemoRecommendation(2L, "iPhone 15 Pro", "electronics", "apple.com", 0.88, "COLLABORATIVE"));
        recommendations.add(createDemoRecommendation(3L, "Gaming Mouse", "electronics", "gaming.com", 0.82, "MATRIX_FACTORIZATION"));
        recommendations.add(createDemoRecommendation(4L, "Mechanical Keyboard", "electronics", "gaming.com", 0.78, "CLUSTERING"));
        recommendations.add(createDemoRecommendation(5L, "4K Monitor", "electronics", "tech.com", 0.75, "TEMPORAL"));
        recommendations.add(createDemoRecommendation(6L, "Designer T-Shirt", "clothing", "fashion.com", 0.72, "CONTENT_BASED"));
        recommendations.add(createDemoRecommendation(7L, "Leather Jacket", "clothing", "fashion.com", 0.68, "COLLABORATIVE"));
        recommendations.add(createDemoRecommendation(8L, "Java Programming Book", "books", "books.com", 0.65, "HYBRID"));
        
        return recommendations;
    }

    /**
     * Создает одну демо-рекомендацию
     */
    private RecommendationDto createDemoRecommendation(Long id, String name, String category, String source, double score, String algorithm) {
        RecommendationDto dto = new RecommendationDto();
        dto.setId(id);
        dto.setProductId(id);
        dto.setProductName(name);
        dto.setProductUrl("https://example.com/product/" + id);
        dto.setProductCategory(category);
        dto.setProductSource(source);
        dto.setScore(java.math.BigDecimal.valueOf(score));
        dto.setAlgorithm(algorithm);
        dto.setIsViewed(false);
        return dto;
    }
}
