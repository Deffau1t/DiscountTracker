package com.example.controller;

import com.example.dto.RecommendationDto;
import com.example.entity.User;
import com.example.service.RecommendationService;
import com.example.service.PersonalizationService;
import com.example.service.TrendAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationManagementController {

    private final RecommendationService recommendationService;
    private final PersonalizationService personalizationService;
    private final TrendAnalysisService trendAnalysisService;

    /**
     * Генерирует новые рекомендации для пользователя
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        try {
            log.info("Запрос на генерацию рекомендаций для пользователя {}", user.getEmail());
            
            List<RecommendationDto> recommendations = recommendationService.generateRecommendations(user, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recommendations", recommendations);
            response.put("count", recommendations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при генерации рекомендаций для пользователя {}", user.getEmail(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка при генерации рекомендаций");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Получает текущие рекомендации пользователя
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        try {
            List<RecommendationDto> recommendations = recommendationService.getUserRecommendations(user, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recommendations", recommendations);
            response.put("count", recommendations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций для пользователя {}", user.getEmail(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка при получении рекомендаций");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Отмечает рекомендацию как просмотренную
     */
    @PostMapping("/{recommendationId}/view")
    public ResponseEntity<Map<String, Object>> markAsViewed(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal User user) {
        
        try {
            recommendationService.markAsViewed(recommendationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Рекомендация отмечена как просмотренная");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при отметке рекомендации {} как просмотренной", recommendationId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка при отметке рекомендации");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Получает статистику рекомендаций
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRecommendationStats(@AuthenticationPrincipal User user) {
        try {
            Long unviewedCount = recommendationService.getUnviewedRecommendationsCount(user);
            List<RecommendationDto> recentRecommendations = recommendationService.getUserRecommendations(user, 5);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unviewedCount", unviewedCount);
            response.put("recentRecommendations", recentRecommendations);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при получении статистики рекомендаций для пользователя {}", user.getEmail(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка при получении статистики");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Обновляет предпочтения пользователя
     */
    @PostMapping("/preferences/update")
    public ResponseEntity<Map<String, Object>> updatePreferences(@AuthenticationPrincipal User user) {
        try {
            recommendationService.updateUserPreferences(user);
            personalizationService.updateUserProfile(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Предпочтения обновлены");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при обновлении предпочтений пользователя {}", user.getEmail(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка при обновлении предпочтений");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Получает трендовые товары
     */
    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrendingProducts() {
        try {
            List<com.example.entity.Product> trendingProducts = trendAnalysisService.getProductsWithPositivePriceTrend();
            List<com.example.entity.Product> growingPopularityProducts = trendAnalysisService.getProductsWithGrowingPopularity();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("trendingProducts", trendingProducts);
            response.put("growingPopularityProducts", growingPopularityProducts);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при получении трендовых товаров", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка при получении трендовых товаров");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Получает профиль пользователя
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@AuthenticationPrincipal User user) {
        try {
            Map<String, Double> timePrefs = personalizationService.analyzeTimeBasedPreferences(user);
            Map<String, Double> dayPrefs = personalizationService.analyzeDayOfWeekPreferences(user);
            Map<String, Double> pricePrefs = personalizationService.analyzePricePreferences(user);
            Map<String, Double> sourcePrefs = personalizationService.analyzeSourcePreferences(user);
            Map<String, Double> activity = personalizationService.analyzeUserActivity(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timePreferences", timePrefs);
            response.put("dayPreferences", dayPrefs);
            response.put("pricePreferences", pricePrefs);
            response.put("sourcePreferences", sourcePrefs);
            response.put("activity", activity);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при получении профиля пользователя {}", user.getEmail(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Ошибка при получении профиля");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
