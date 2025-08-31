package com.example.controller;

import com.example.dto.RecommendationDto;
import com.example.dto.UserPreferenceDto;
import com.example.entity.User;
import com.example.entity.Product;
import com.example.repository.UserRepository;
import com.example.repository.ProductRepository;
import com.example.service.RecommendationService;
import com.example.service.UserBehaviorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserBehaviorTrackingService behaviorTrackingService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * Главная страница рекомендаций
     */
    @GetMapping
    public String showRecommendations(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return "redirect:/login";
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();

        try {
            // Получаем существующие рекомендации
            List<RecommendationDto> recommendations = recommendationService.getUserRecommendations(user, 20);
            
            // Получаем количество непросмотренных рекомендаций
            Long unviewedCount = recommendationService.getUnviewedRecommendationsCount(user);
            
            model.addAttribute("recommendations", recommendations);
            model.addAttribute("unviewedCount", unviewedCount);
            model.addAttribute("user", user);
            
            log.info("Показаны рекомендации для пользователя {}: {} шт.", user.getEmail(), recommendations.size());
            
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций для пользователя {}", user.getEmail(), e);
            model.addAttribute("error", "Не удалось загрузить рекомендации");
        }

        return "recommendations";
    }

    /**
     * REST API для получения рекомендаций
     */
    @GetMapping("/api")
    @ResponseBody
    public List<RecommendationDto> getRecommendationsApi(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "limit", required = false) Integer limit) {
        
        if (userDetails == null || userDetails.getUsername() == null) {
            return List.of();
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User user = userOpt.get();

        try {
            return recommendationService.getUserRecommendations(user, limit);
        } catch (Exception e) {
            log.error("Ошибка при получении рекомендаций через API для пользователя {}", user.getEmail(), e);
            return List.of();
        }
    }

    /**
     * REST API для генерации новых рекомендаций
     */
    @PostMapping("/api/generate")
    @ResponseBody
    public List<RecommendationDto> generateRecommendationsApi(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "limit", required = false) Integer limit) {
        
        if (userDetails == null || userDetails.getUsername() == null) {
            return List.of();
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User user = userOpt.get();

        try {
            log.info("Запрос на генерацию новых рекомендаций для пользователя {} (лимит: {})", 
                     user.getEmail(), limit);
            
            return recommendationService.generateRecommendations(user, limit);
        } catch (Exception e) {
            log.error("Ошибка при генерации рекомендаций для пользователя {}", user.getEmail(), e);
            return List.of();
        }
    }

    /**
     * REST API для обновления предпочтений пользователя
     */
    @PostMapping("/api/preferences/update")
    @ResponseBody
    public String updatePreferencesApi(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return "Unauthorized";
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "Unauthorized";
        }
        User user = userOpt.get();

        try {
            recommendationService.updateUserPreferences(user);
            log.info("Предпочтения пользователя {} обновлены", user.getEmail());
            return "Success";
        } catch (Exception e) {
            log.error("Ошибка при обновлении предпочтений пользователя {}", user.getEmail(), e);
            return "Error";
        }
    }

    /**
     * REST API для отметки рекомендации как просмотренной
     */
    @PostMapping("/api/{id}/view")
    @ResponseBody
    public String markAsViewedApi(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        if (userDetails == null || userDetails.getUsername() == null) {
            return "Unauthorized";
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "Unauthorized";
        }
        User user = userOpt.get();

        try {
            recommendationService.markAsViewed(id);
            log.debug("Рекомендация {} отмечена как просмотренная пользователем {}", id, user.getEmail());
            return "Success";
        } catch (Exception e) {
            log.error("Ошибка при отметке рекомендации {} как просмотренной", id, e);
            return "Error";
        }
    }

    /**
     * Страница настроек предпочтений
     */
    @GetMapping("/preferences")
    public String showPreferences(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return "redirect:/login";
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();

        try {
            // Здесь можно добавить логику для отображения текущих предпочтений
            model.addAttribute("user", user);
            model.addAttribute("message", "Настройки предпочтений");
            
        } catch (Exception e) {
            log.error("Ошибка при загрузке настроек предпочтений для пользователя {}", user.getEmail(), e);
            model.addAttribute("error", "Не удалось загрузить настройки");
        }

        return "preferences";
    }

    /**
     * Обновление предпочтений через веб-форму
     */
    @PostMapping("/preferences/update")
    public String updatePreferencesForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return "redirect:/login";
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();

        try {
            recommendationService.updateUserPreferences(user);
            model.addAttribute("message", "Предпочтения успешно обновлены");
            log.info("Предпочтения пользователя {} обновлены через веб-форму", user.getEmail());
            
        } catch (Exception e) {
            log.error("Ошибка при обновлении предпочтений пользователя {} через веб-форму", user.getEmail(), e);
            model.addAttribute("error", "Не удалось обновить предпочтения");
        }

        return "preferences";
    }

    /**
     * Фиксирует просмотр товара и редиректит на источник
     */
    @GetMapping("/view/{productId}")
    public String viewProduct(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Long productId) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return "redirect:/login";
        }

        java.util.Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();

        return productRepository.findById(productId)
                .map(product -> {
                    behaviorTrackingService.trackProductView(user, product);
                    String url = product.getUrl();
                    if (url == null || url.isBlank()) {
                        return "redirect:/recommendations";
                    }
                    return "redirect:" + url;
                })
                .orElse("redirect:/recommendations");
    }
}
