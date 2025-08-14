package com.example.service;

import com.example.entity.Product;
import com.example.entity.User;
import com.example.entity.UserBehavior;
import com.example.entity.UserPreference;
import com.example.repository.ProductRepository;
import com.example.repository.UserBehaviorRepository;
import com.example.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ProductRepository productRepository;

    /**
     * Анализирует предпочтения пользователя по времени суток
     */
    public Map<String, Double> analyzeTimeBasedPreferences(User user) {
        try {
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
            Map<String, Integer> timeSlotCounts = new HashMap<>();
            
            for (UserBehavior behavior : behaviors) {
                String timeSlot = getTimeSlot(behavior.getCreatedAt());
                timeSlotCounts.merge(timeSlot, 1, Integer::sum);
            }
            
            // Нормализуем до процентов
            int total = timeSlotCounts.values().stream().mapToInt(Integer::intValue).sum();
            Map<String, Double> preferences = new HashMap<>();
            
            for (Map.Entry<String, Integer> entry : timeSlotCounts.entrySet()) {
                preferences.put(entry.getKey(), (double) entry.getValue() / total);
            }
            
            return preferences;
            
        } catch (Exception e) {
            log.warn("Ошибка при анализе временных предпочтений пользователя {}", user.getEmail(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Анализирует предпочтения пользователя по дням недели
     */
    public Map<String, Double> analyzeDayOfWeekPreferences(User user) {
        try {
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
            Map<String, Integer> dayCounts = new HashMap<>();
            
            for (UserBehavior behavior : behaviors) {
                String dayOfWeek = behavior.getCreatedAt().getDayOfWeek().toString();
                dayCounts.merge(dayOfWeek, 1, Integer::sum);
            }
            
            // Нормализуем до процентов
            int total = dayCounts.values().stream().mapToInt(Integer::intValue).sum();
            Map<String, Double> preferences = new HashMap<>();
            
            for (Map.Entry<String, Integer> entry : dayCounts.entrySet()) {
                preferences.put(entry.getKey(), (double) entry.getValue() / total);
            }
            
            return preferences;
            
        } catch (Exception e) {
            log.warn("Ошибка при анализе предпочтений по дням недели для пользователя {}", user.getEmail(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Анализирует ценовые предпочтения пользователя
     */
    public Map<String, Double> analyzePricePreferences(User user) {
        try {
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
            List<BigDecimal> prices = new ArrayList<>();
            
            // Собираем цены товаров, с которыми взаимодействовал пользователь
            for (UserBehavior behavior : behaviors) {
                if (behavior.getProduct().getCurrentPrice() != null) {
                    prices.add(behavior.getProduct().getCurrentPrice());
                }
            }
            
            if (prices.isEmpty()) {
                return Collections.emptyMap();
            }
            
            // Вычисляем статистики
            BigDecimal minPrice = prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal avgPrice = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(prices.size()), 2, BigDecimal.ROUND_HALF_UP);
            
            Map<String, Double> preferences = new HashMap<>();
            preferences.put("minPrice", minPrice.doubleValue());
            preferences.put("maxPrice", maxPrice.doubleValue());
            preferences.put("avgPrice", avgPrice.doubleValue());
            preferences.put("priceRange", maxPrice.subtract(minPrice).doubleValue());
            
            return preferences;
            
        } catch (Exception e) {
            log.warn("Ошибка при анализе ценовых предпочтений пользователя {}", user.getEmail(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Анализирует предпочтения по источникам товаров
     */
    public Map<String, Double> analyzeSourcePreferences(User user) {
        try {
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
            Map<String, Integer> sourceCounts = new HashMap<>();
            
            for (UserBehavior behavior : behaviors) {
                String source = behavior.getProduct().getSource();
                if (source != null) {
                    sourceCounts.merge(source, 1, Integer::sum);
                }
            }
            
            // Нормализуем до процентов
            int total = sourceCounts.values().stream().mapToInt(Integer::intValue).sum();
            Map<String, Double> preferences = new HashMap<>();
            
            for (Map.Entry<String, Integer> entry : sourceCounts.entrySet()) {
                preferences.put(entry.getKey(), (double) entry.getValue() / total);
            }
            
            return preferences;
            
        } catch (Exception e) {
            log.warn("Ошибка при анализе предпочтений по источникам для пользователя {}", user.getEmail(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Анализирует активность пользователя
     */
    public Map<String, Double> analyzeUserActivity(User user) {
        try {
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
            
            if (behaviors.isEmpty()) {
                return Collections.emptyMap();
            }
            
            // Сортируем по времени
            behaviors.sort(Comparator.comparing(UserBehavior::getCreatedAt));
            
            LocalDateTime firstActivity = behaviors.get(0).getCreatedAt();
            LocalDateTime lastActivity = behaviors.get(behaviors.size() - 1).getCreatedAt();
            
            long daysActive = ChronoUnit.DAYS.between(firstActivity, lastActivity) + 1;
            double avgDailyActivity = (double) behaviors.size() / daysActive;
            
            // Анализируем активность по типам поведения
            Map<UserBehavior.BehaviorType, Long> behaviorTypeCounts = behaviors.stream()
                    .collect(Collectors.groupingBy(UserBehavior::getBehaviorType, Collectors.counting()));
            
            Map<String, Double> activity = new HashMap<>();
            activity.put("totalInteractions", (double) behaviors.size());
            activity.put("daysActive", (double) daysActive);
            activity.put("avgDailyActivity", avgDailyActivity);
            activity.put("views", (double) behaviorTypeCounts.getOrDefault(UserBehavior.BehaviorType.VIEW, 0L));
            activity.put("watchAdds", (double) behaviorTypeCounts.getOrDefault(UserBehavior.BehaviorType.WATCH_ADD, 0L));
            activity.put("notificationClicks", (double) behaviorTypeCounts.getOrDefault(UserBehavior.BehaviorType.NOTIFICATION_CLICK, 0L));
            
            return activity;
            
        } catch (Exception e) {
            log.warn("Ошибка при анализе активности пользователя {}", user.getEmail(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Вычисляет персонализированный скор для товара
     */
    public BigDecimal calculatePersonalizedScore(Product product, User user) {
        try {
            BigDecimal score = BigDecimal.ZERO;
            
            // Базовый скор по категории
            List<UserPreference> preferences = userPreferenceRepository.findByUserId(user.getId());
            for (UserPreference preference : preferences) {
                if (product.getCategory() != null && product.getCategory().equals(preference.getCategory())) {
                    score = score.add(preference.getWeight());
                }
            }
            
            // Бонус за источник
            Map<String, Double> sourcePrefs = analyzeSourcePreferences(user);
            if (product.getSource() != null && sourcePrefs.containsKey(product.getSource())) {
                score = score.add(BigDecimal.valueOf(sourcePrefs.get(product.getSource()) * 0.3));
            }
            
            // Бонус за цену
            Map<String, Double> pricePrefs = analyzePricePreferences(user);
            if (product.getCurrentPrice() != null && !pricePrefs.isEmpty()) {
                double avgPrice = pricePrefs.get("avgPrice");
                double priceRange = pricePrefs.get("priceRange");
                
                if (priceRange > 0) {
                    double priceDiff = Math.abs(product.getCurrentPrice().doubleValue() - avgPrice) / priceRange;
                    double priceBonus = Math.max(0, 0.2 - priceDiff * 0.2);
                    score = score.add(BigDecimal.valueOf(priceBonus));
                }
            }
            
            // Бонус за время (если сейчас активное время пользователя)
            Map<String, Double> timePrefs = analyzeTimeBasedPreferences(user);
            String currentTimeSlot = getTimeSlot(LocalDateTime.now());
            if (timePrefs.containsKey(currentTimeSlot)) {
                score = score.add(BigDecimal.valueOf(timePrefs.get(currentTimeSlot) * 0.1));
            }
            
            return score.max(BigDecimal.ZERO).min(BigDecimal.ONE);
            
        } catch (Exception e) {
            log.warn("Ошибка при вычислении персонализированного скора для товара {} и пользователя {}", 
                    product.getId(), user.getEmail(), e);
            return BigDecimal.valueOf(0.3);
        }
    }

    /**
     * Получает временной слот для даты
     */
    private String getTimeSlot(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        
        if (hour >= 6 && hour < 12) return "morning";
        else if (hour >= 12 && hour < 18) return "afternoon";
        else if (hour >= 18 && hour < 22) return "evening";
        else return "night";
    }

    /**
     * Обновляет профиль пользователя на основе его поведения
     */
    public void updateUserProfile(User user) {
        try {
            // Анализируем все аспекты поведения
            Map<String, Double> timePrefs = analyzeTimeBasedPreferences(user);
            Map<String, Double> dayPrefs = analyzeDayOfWeekPreferences(user);
            Map<String, Double> pricePrefs = analyzePricePreferences(user);
            Map<String, Double> sourcePrefs = analyzeSourcePreferences(user);
            Map<String, Double> activity = analyzeUserActivity(user);
            
            // Здесь можно сохранить профиль в отдельную таблицу или обновить существующие предпочтения
            log.info("Профиль пользователя {} обновлен", user.getEmail());
            
        } catch (Exception e) {
            log.error("Ошибка при обновлении профиля пользователя {}", user.getEmail(), e);
        }
    }
}
