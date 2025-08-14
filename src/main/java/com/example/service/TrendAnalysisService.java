package com.example.service;

import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.entity.UserBehavior;
import com.example.repository.PriceHistoryRepository;
import com.example.repository.ProductRepository;
import com.example.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendAnalysisService {

    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final UserBehaviorRepository userBehaviorRepository;

    /**
     * Анализирует тренд цены товара
     */
    public BigDecimal analyzePriceTrend(Long productId) {
        try {
            List<PriceHistory> priceHistory = priceHistoryRepository.findByProductId(productId);
            if (priceHistory.size() < 2) {
                return BigDecimal.ZERO;
            }

            // Сортируем по дате
            priceHistory.sort(Comparator.comparing(PriceHistory::getCheckedAt));

            // Берем последние 10 записей для анализа тренда
            List<PriceHistory> recentHistory = priceHistory.subList(
                Math.max(0, priceHistory.size() - 10), priceHistory.size());

            if (recentHistory.size() < 2) {
                return BigDecimal.ZERO;
            }

            // Вычисляем среднее изменение цены
            BigDecimal totalChange = BigDecimal.ZERO;
            int changeCount = 0;

            for (int i = 1; i < recentHistory.size(); i++) {
                BigDecimal currentPrice = recentHistory.get(i).getPrice();
                BigDecimal previousPrice = recentHistory.get(i - 1).getPrice();
                
                if (previousPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal change = currentPrice.subtract(previousPrice)
                            .divide(previousPrice, 4, RoundingMode.HALF_UP);
                    totalChange = totalChange.add(change);
                    changeCount++;
                }
            }

            if (changeCount == 0) {
                return BigDecimal.ZERO;
            }

            return totalChange.divide(BigDecimal.valueOf(changeCount), 4, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.warn("Ошибка при анализе тренда цены для товара {}", productId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Анализирует популярность товара во времени
     */
    public BigDecimal analyzePopularityTrend(Long productId) {
        try {
            List<UserBehavior> behaviors = userBehaviorRepository.findByProductId(productId);
            if (behaviors.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Группируем по дням
            Map<String, Long> dailyInteractions = behaviors.stream()
                    .collect(Collectors.groupingBy(
                            behavior -> behavior.getCreatedAt().toLocalDate().toString(),
                            Collectors.counting()));

            if (dailyInteractions.size() < 2) {
                return BigDecimal.ZERO;
            }

            // Вычисляем тренд популярности
            List<Long> dailyCounts = new ArrayList<>(dailyInteractions.values());
            Collections.sort(dailyCounts);

            // Простой анализ: если последние дни более активны, тренд положительный
            int midPoint = dailyCounts.size() / 2;
            List<Long> earlyPeriod = dailyCounts.subList(0, midPoint);
            List<Long> latePeriod = dailyCounts.subList(midPoint, dailyCounts.size());

            double earlyAvg = earlyPeriod.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double lateAvg = latePeriod.stream().mapToLong(Long::longValue).average().orElse(0.0);

            if (earlyAvg == 0) {
                return BigDecimal.ZERO;
            }

            return BigDecimal.valueOf((lateAvg - earlyAvg) / earlyAvg);

        } catch (Exception e) {
            log.warn("Ошибка при анализе тренда популярности для товара {}", productId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Анализирует сезонность товара
     */
    public BigDecimal analyzeSeasonality(Long productId) {
        try {
            List<UserBehavior> behaviors = userBehaviorRepository.findByProductId(productId);
            if (behaviors.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // Группируем по месяцам
            Map<Integer, Long> monthlyInteractions = behaviors.stream()
                    .collect(Collectors.groupingBy(
                            behavior -> behavior.getCreatedAt().getMonthValue(),
                            Collectors.counting()));

            if (monthlyInteractions.size() < 3) {
                return BigDecimal.ZERO;
            }

            // Вычисляем сезонность (разброс между месяцами)
            double avg = monthlyInteractions.values().stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            if (avg == 0) {
                return BigDecimal.ZERO;
            }

            double variance = monthlyInteractions.values().stream()
                    .mapToDouble(count -> Math.pow(count - avg, 2))
                    .average()
                    .orElse(0.0);

            double coefficientOfVariation = Math.sqrt(variance) / avg;
            return BigDecimal.valueOf(Math.min(1.0, coefficientOfVariation));

        } catch (Exception e) {
            log.warn("Ошибка при анализе сезонности для товара {}", productId, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Получает товары с положительным трендом цены
     */
    public List<Product> getProductsWithPositivePriceTrend() {
        try {
            List<Product> allProducts = productRepository.findAll();
            return allProducts.stream()
                    .filter(product -> {
                        BigDecimal trend = analyzePriceTrend(product.getId());
                        return trend.compareTo(BigDecimal.valueOf(0.05)) > 0; // Тренд роста > 5%
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении товаров с положительным трендом", e);
            return Collections.emptyList();
        }
    }

    /**
     * Получает товары с растущей популярностью
     */
    public List<Product> getProductsWithGrowingPopularity() {
        try {
            List<Product> allProducts = productRepository.findAll();
            return allProducts.stream()
                    .filter(product -> {
                        BigDecimal trend = analyzePopularityTrend(product.getId());
                        return trend.compareTo(BigDecimal.valueOf(0.1)) > 0; // Рост популярности > 10%
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении товаров с растущей популярностью", e);
            return Collections.emptyList();
        }
    }

    /**
     * Получает товары с высокой сезонностью
     */
    public List<Product> getSeasonalProducts() {
        try {
            List<Product> allProducts = productRepository.findAll();
            return allProducts.stream()
                    .filter(product -> {
                        BigDecimal seasonality = analyzeSeasonality(product.getId());
                        return seasonality.compareTo(BigDecimal.valueOf(0.5)) > 0; // Высокая сезонность
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении сезонных товаров", e);
            return Collections.emptyList();
        }
    }

    /**
     * Вычисляет общий тренд-скор для товара
     */
    public BigDecimal calculateTrendScore(Long productId) {
        BigDecimal priceTrend = analyzePriceTrend(productId);
        BigDecimal popularityTrend = analyzePopularityTrend(productId);
        BigDecimal seasonality = analyzeSeasonality(productId);

        // Взвешенная сумма трендов
        BigDecimal score = priceTrend.multiply(BigDecimal.valueOf(0.4))
                .add(popularityTrend.multiply(BigDecimal.valueOf(0.4)))
                .add(seasonality.multiply(BigDecimal.valueOf(0.2)));

        return score.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
