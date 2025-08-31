package com.example.service;

import com.example.dto.RecommendationDto;
import com.example.entity.*;
import com.example.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final ProductRecommendationRepository productRecommendationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final TrendAnalysisService trendAnalysisService;
    private final PersonalizationService personalizationService;
    private final UserBehaviorTrackingService userBehaviorTrackingService;

    @Value("${recommendation.min.weight:0.5}")
    private Double minWeight;

    @Value("${recommendation.min.similarity:0.3}")
    private Double minSimilarity;

    @Value("${recommendation.default.limit:10}")
    private Integer defaultLimit;

    @Value("${recommendation.matrix.factors:10}")
    private Integer matrixFactors;

    @Value("${recommendation.cluster.k:5}")
    private Integer clusterK;

    /**
     * Генерирует рекомендации для пользователя с использованием всех доступных алгоритмов
     */
    @Transactional
    public List<RecommendationDto> generateRecommendations(User user, Integer limit) {
        if (limit == null) limit = defaultLimit;
        
        log.info("Генерация рекомендаций для пользователя {} (лимит: {})", user.getEmail(), limit);
        
        try {
            // Получаем предпочтения пользователя
            List<UserPreference> preferences = userPreferenceRepository.findByUserId(user.getId());
            if (preferences.isEmpty()) {
                preferences = getDefaultPreferences(user);
            }
            
                         // Генерируем рекомендации разными алгоритмами
             List<ProductRecommendation> contentBased = generateContentBasedRecommendations(user, limit);
             List<ProductRecommendation> collaborative = generateCollaborativeRecommendations(user, limit);
             List<ProductRecommendation> matrixFactorization = generateMatrixFactorizationRecommendations(user, limit);
             List<ProductRecommendation> clustering = generateClusteringRecommendations(user, limit);
             List<ProductRecommendation> temporal = generateTemporalRecommendations(user, limit);
             List<ProductRecommendation> trendBased = generateTrendBasedRecommendations(user, limit);
             List<ProductRecommendation> personalized = generatePersonalizedRecommendations(user, limit);
            
                         // Объединяем все рекомендации
             List<ProductRecommendation> combined = combineAllRecommendations(
                 contentBased, collaborative, matrixFactorization, clustering, temporal, trendBased, personalized, limit);
            
            // Фолбэк: если ничего не набралось, подставляем базовые рекомендации
            if (combined.isEmpty()) {
                log.warn("Алгоритмы вернули пустой список. Используем фолбэк-рекомендации");
                combined = generateFallbackRecommendations(user, limit);
            }

            // Усиливаем фокус по доминирующим категориям пользователя
            combined = applyCategoryFocus(user, combined, limit);
            
            // Сохраняем рекомендации
            List<ProductRecommendation> saved = saveRecommendations(user, combined);
            
            log.info("Сгенерировано {} рекомендаций для пользователя {}", saved.size(), user.getEmail());
            return saved.stream().map(this::convertToDto).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Ошибка при генерации рекомендаций для пользователя {}", user.getEmail(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Content-based фильтрация на основе предпочтений пользователя (улучшенная версия)
     */
    private List<ProductRecommendation> generateContentBasedRecommendations(User user, Integer limit) {
        log.debug("Генерация content-based рекомендаций для пользователя {}", user.getEmail());
        
        List<UserPreference> preferences = userPreferenceRepository.findByUserId(user.getId());
        if (preferences.isEmpty()) {
            // Используем дефолтные предпочтения, если своих нет
            preferences = getDefaultPreferences(user);
        }
        
        // Берём только товары с активностью (WATCH_ADD, VIEW, NOTIFICATION_CLICK) или из наблюдений
        Set<Long> activeProductIds = userBehaviorRepository.findAll().stream()
                .filter(b -> b.getBehaviorType() == UserBehavior.BehaviorType.WATCH_ADD
                        || b.getBehaviorType() == UserBehavior.BehaviorType.VIEW
                        || b.getBehaviorType() == UserBehavior.BehaviorType.NOTIFICATION_CLICK)
                .map(b -> b.getProduct().getId())
                .collect(Collectors.toSet());

        if (activeProductIds.isEmpty()) {
            // Фолбэк на уведомления (список наблюдения)
            List<Notification> allNotifications = notificationRepository.findAll();
            activeProductIds = allNotifications.stream()
                    .map(n -> n.getProduct().getId())
                    .collect(Collectors.toSet());
        }

        List<Product> products = activeProductIds.isEmpty()
                ? Collections.emptyList()
                : productRepository.findAllById(activeProductIds);
        List<ProductRecommendation> recommendations = new ArrayList<>();
        
        for (Product product : products) {
            BigDecimal score = calculateEnhancedContentBasedScore(product, preferences, user);
            if (score.compareTo(BigDecimal.valueOf(minWeight)) >= 0) {
                ProductRecommendation rec = new ProductRecommendation();
                rec.setUser(user);
                rec.setProduct(product);
                rec.setScore(score);
                rec.setAlgorithm(ProductRecommendation.AlgorithmType.CONTENT_BASED);
                recommendations.add(rec);
            }
        }
        
        // Сортируем по скору и ограничиваем количество
        return recommendations.stream()
                .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает топ доминирующих категорий пользователя (по поведению и наблюдениям)
     */
    private List<String> getUserDominantCategories(User user, int maxCategories) {
        Map<String, Long> categoryCounts = new HashMap<>();

        // По поведению
        List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
        behaviors.stream()
                .filter(b -> b.getBehaviorType() == UserBehavior.BehaviorType.WATCH_ADD
                        || b.getBehaviorType() == UserBehavior.BehaviorType.VIEW
                        || b.getBehaviorType() == UserBehavior.BehaviorType.NOTIFICATION_CLICK)
                .map(b -> b.getProduct().getCategory())
                .filter(Objects::nonNull)
                .forEach(cat -> categoryCounts.merge(cat, 1L, Long::sum));

        // Фолбэк: по уведомлениям (watch-list)
        if (categoryCounts.isEmpty()) {
            List<Notification> notifications = notificationRepository.findByUserId(user.getId());
            notifications.stream()
                    .map(n -> n.getProduct().getCategory())
                    .filter(Objects::nonNull)
                    .forEach(cat -> categoryCounts.merge(cat, 1L, Long::sum));
        }

        return categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(Math.max(1, maxCategories))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Усиливает фокус рекомендаций на доминирующих категориях пользователя
     */
    private List<ProductRecommendation> applyCategoryFocus(User user, List<ProductRecommendation> recs, int limit) {
        if (recs.isEmpty()) return recs;
        List<String> topCategories = getUserDominantCategories(user, 1);
        if (topCategories.isEmpty()) {
            return recs.stream()
                    .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        String top = topCategories.get(0);
        // Жёсткая фильтрация: оставляем только топ-категорию
        return recs.stream()
                .filter(r -> r.getProduct() != null
                        && r.getProduct().getCategory() != null
                        && r.getProduct().getCategory().equals(top))
                .peek(r -> r.setScore(r.getScore().multiply(BigDecimal.valueOf(1.5))))
                .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Фолбэк-рекомендации на случай отсутствия данных/совпадений
     */
    private List<ProductRecommendation> generateFallbackRecommendations(User user, Integer limit) {
        // Получаем статистику по просмотрам, добавлениям и кликам на уведомления
        Map<Product, Long> popularityMap = userBehaviorRepository.findAll().stream()
                .filter(b -> b.getBehaviorType() == UserBehavior.BehaviorType.WATCH_ADD
                          || b.getBehaviorType() == UserBehavior.BehaviorType.VIEW
                          || b.getBehaviorType() == UserBehavior.BehaviorType.NOTIFICATION_CLICK)
                .collect(Collectors.groupingBy(UserBehavior::getProduct, Collectors.counting()));

        // Если активностей нет вообще, используем популярность по количеству наблюдений (notifications)
        if (popularityMap.isEmpty()) {
            List<Notification> notifications = notificationRepository.findAll();
            if (notifications.isEmpty()) {
                return Collections.emptyList();
            }
            popularityMap = notifications.stream()
                    .collect(Collectors.groupingBy(Notification::getProduct, Collectors.counting()));
        }

        if (popularityMap.isEmpty()) {
            return Collections.emptyList();
        }

        // Сортируем по убыванию популярности
        List<Map.Entry<Product, Long>> sortedByPopularity = popularityMap.entrySet().stream()
                .sorted(Map.Entry.<Product, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Максимальное значение популярности для нормализации score
        long maxPopularity = sortedByPopularity.get(0).getValue();

        return sortedByPopularity.stream()
                .map(entry -> {
                    ProductRecommendation rec = new ProductRecommendation();
                    rec.setUser(user);
                    rec.setProduct(entry.getKey());
                    // Score нормализуем от 0.5 до 1.0
                    double score = 0.5 + (0.5 * entry.getValue() / (double) maxPopularity);
                    rec.setScore(BigDecimal.valueOf(score));
                    rec.setAlgorithm(ProductRecommendation.AlgorithmType.TREND_BASED);
                    return rec;
                })
                .collect(Collectors.toList());
    }
    

    /**
     * Collaborative фильтрация на основе поведения похожих пользователей (улучшенная версия)
     */
    private List<ProductRecommendation> generateCollaborativeRecommendations(User user, Integer limit) {
        log.debug("Генерация collaborative рекомендаций для пользователя {}", user.getEmail());
        
        // Находим похожих пользователей с улучшенным алгоритмом
        List<User> similarUsers = findSimilarUsersEnhanced(user);
        if (similarUsers.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Анализируем поведение похожих пользователей
        Map<Long, BigDecimal> productScores = new HashMap<>();
        for (User similarUser : similarUsers) {
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(similarUser.getId());
            for (UserBehavior behavior : behaviors) {
                if (behavior.getBehaviorType() == UserBehavior.BehaviorType.WATCH_ADD ||
                    behavior.getBehaviorType() == UserBehavior.BehaviorType.VIEW ||
                    behavior.getBehaviorType() == UserBehavior.BehaviorType.NOTIFICATION_CLICK) {
                    
                    Long productId = behavior.getProduct().getId();
                    BigDecimal currentScore = productScores.getOrDefault(productId, BigDecimal.ZERO);
                    BigDecimal behaviorScore = getEnhancedBehaviorScore(behavior.getBehaviorType(), behavior.getCreatedAt());
                    productScores.put(productId, currentScore.add(behaviorScore));
                }
            }
        }
        
        // Создаем рекомендации
        List<ProductRecommendation> recommendations = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : productScores.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal score = entry.getValue();
            
            // Проверяем, что товар не в списке наблюдения пользователя
            if (!isProductInUserWatchList(user, productId)) {
                Optional<Product> productOpt = productRepository.findById(productId);
                if (productOpt.isPresent()) {
                    ProductRecommendation rec = new ProductRecommendation();
                    rec.setUser(user);
                    rec.setProduct(productOpt.get());
                    rec.setScore(score);
                    rec.setAlgorithm(ProductRecommendation.AlgorithmType.COLLABORATIVE);
                    recommendations.add(rec);
                }
            }
        }
        
        // Сортируем по скору и ограничиваем количество
        return recommendations.stream()
                .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Матричная факторизация для рекомендаций
     */
    private List<ProductRecommendation> generateMatrixFactorizationRecommendations(User user, Integer limit) {
        log.debug("Генерация матричной факторизации для пользователя {}", user.getEmail());
        
        try {
            // Получаем матрицу взаимодействий пользователь-товар
            Map<Long, Map<Long, BigDecimal>> userProductMatrix = buildUserProductMatrix();
            
            // Простая реализация SVD (Singular Value Decomposition)
            Map<Long, BigDecimal> userFactors = decomposeUserFactors(user.getId(), userProductMatrix);
            Map<Long, BigDecimal> productFactors = decomposeProductFactors(userProductMatrix);
            
            // Вычисляем скоры для всех товаров
            List<Product> products = productRepository.findAll();
            List<ProductRecommendation> recommendations = new ArrayList<>();
            
            for (Product product : products) {
                BigDecimal score = calculateMatrixFactorizationScore(userFactors, productFactors, product.getId());
                if (score.compareTo(BigDecimal.valueOf(0.4)) >= 0) {
                    ProductRecommendation rec = new ProductRecommendation();
                    rec.setUser(user);
                    rec.setProduct(product);
                    rec.setScore(score);
                    rec.setAlgorithm(ProductRecommendation.AlgorithmType.MATRIX_FACTORIZATION);
                    recommendations.add(rec);
                }
            }
            
            return recommendations.stream()
                    .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Ошибка при генерации матричной факторизации для пользователя {}", user.getEmail(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Кластеризация пользователей для рекомендаций
     */
    private List<ProductRecommendation> generateClusteringRecommendations(User user, Integer limit) {
        log.debug("Генерация кластерных рекомендаций для пользователя {}", user.getEmail());
        
        try {
            // Находим кластер пользователя
            Integer userCluster = findUserCluster(user);
            if (userCluster == null) {
                return Collections.emptyList();
            }
            
            // Получаем товары, популярные в этом кластере
            List<Long> clusterProductIds = getClusterPopularProducts(userCluster);
            
            List<ProductRecommendation> recommendations = new ArrayList<>();
            for (Long productId : clusterProductIds) {
                if (!isProductInUserWatchList(user, productId)) {
                    Optional<Product> productOpt = productRepository.findById(productId);
                    if (productOpt.isPresent()) {
                        ProductRecommendation rec = new ProductRecommendation();
                        rec.setUser(user);
                        rec.setProduct(productOpt.get());
                        rec.setScore(BigDecimal.valueOf(0.6)); // Базовый скор для кластера
                        rec.setAlgorithm(ProductRecommendation.AlgorithmType.CLUSTERING);
                        recommendations.add(rec);
                    }
                }
            }
            
            return recommendations.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Ошибка при генерации кластерных рекомендаций для пользователя {}", user.getEmail(), e);
            return Collections.emptyList();
        }
    }

         /**
      * Рекомендации на основе трендов
      */
     private List<ProductRecommendation> generateTrendBasedRecommendations(User user, Integer limit) {
         log.debug("Генерация трендовых рекомендаций для пользователя {}", user.getEmail());
         
         try {
             // Получаем товары с положительным трендом
             List<Product> trendingProducts = trendAnalysisService.getProductsWithPositivePriceTrend();
             List<Product> growingPopularityProducts = trendAnalysisService.getProductsWithGrowingPopularity();
             
             // Объединяем списки
             Set<Product> allTrendingProducts = new HashSet<>(trendingProducts);
             allTrendingProducts.addAll(growingPopularityProducts);
             
             List<ProductRecommendation> recommendations = new ArrayList<>();
             for (Product product : allTrendingProducts) {
                 if (!isProductInUserWatchList(user, product.getId())) {
                     BigDecimal trendScore = trendAnalysisService.calculateTrendScore(product.getId());
                     
                     ProductRecommendation rec = new ProductRecommendation();
                     rec.setUser(user);
                     rec.setProduct(product);
                     rec.setScore(trendScore);
                     rec.setAlgorithm(ProductRecommendation.AlgorithmType.TREND_BASED);
                     recommendations.add(rec);
                 }
             }
             
             return recommendations.stream()
                     .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                     .limit(limit)
                     .collect(Collectors.toList());
                     
         } catch (Exception e) {
             log.error("Ошибка при генерации трендовых рекомендаций для пользователя {}", user.getEmail(), e);
             return Collections.emptyList();
         }
     }
     
     /**
      * Персонализированные рекомендации
      */
     private List<ProductRecommendation> generatePersonalizedRecommendations(User user, Integer limit) {
         log.debug("Генерация персонализированных рекомендаций для пользователя {}", user.getEmail());
         
         try {
             List<Product> products = productRepository.findAll();
             List<ProductRecommendation> recommendations = new ArrayList<>();
             
             for (Product product : products) {
                 if (!isProductInUserWatchList(user, product.getId())) {
                     BigDecimal personalizedScore = personalizationService.calculatePersonalizedScore(product, user);
                     
                     if (personalizedScore.compareTo(BigDecimal.valueOf(0.4)) >= 0) {
                         ProductRecommendation rec = new ProductRecommendation();
                         rec.setUser(user);
                         rec.setProduct(product);
                         rec.setScore(personalizedScore);
                         rec.setAlgorithm(ProductRecommendation.AlgorithmType.PERSONALIZED);
                         recommendations.add(rec);
                     }
                 }
             }
             
             return recommendations.stream()
                     .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                     .limit(limit)
                     .collect(Collectors.toList());
                     
         } catch (Exception e) {
             log.error("Ошибка при генерации персонализированных рекомендаций для пользователя {}", user.getEmail(), e);
             return Collections.emptyList();
         }
     }
     
     /**
      * Временные рекомендации на основе паттернов
      */
     private List<ProductRecommendation> generateTemporalRecommendations(User user, Integer limit) {
        log.debug("Генерация временных рекомендаций для пользователя {}", user.getEmail());
        
        try {
            // Анализируем временные паттерны пользователя
            Map<String, Integer> timePatterns = analyzeTimePatterns(user);
            
            // Получаем товары, соответствующие текущему времени
            String currentTimePattern = getCurrentTimePattern();
            List<Product> temporalProducts = getProductsForTimePattern(currentTimePattern, timePatterns);
            
            List<ProductRecommendation> recommendations = new ArrayList<>();
            for (Product product : temporalProducts) {
                if (!isProductInUserWatchList(user, product.getId())) {
                    ProductRecommendation rec = new ProductRecommendation();
                    rec.setUser(user);
                    rec.setProduct(product);
                    rec.setScore(BigDecimal.valueOf(0.5)); // Базовый скор для временных
                    rec.setAlgorithm(ProductRecommendation.AlgorithmType.TEMPORAL);
                    recommendations.add(rec);
                }
            }
            
            return recommendations.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Ошибка при генерации временных рекомендаций для пользователя {}", user.getEmail(), e);
            return Collections.emptyList();
        }
    }

         /**
      * Объединяет рекомендации от всех алгоритмов с весами
      */
     private List<ProductRecommendation> combineAllRecommendations(
             List<ProductRecommendation> contentBased,
             List<ProductRecommendation> collaborative,
             List<ProductRecommendation> matrixFactorization,
             List<ProductRecommendation> clustering,
             List<ProductRecommendation> temporal,
             List<ProductRecommendation> trendBased,
             List<ProductRecommendation> personalized,
             Integer limit) {
        
        log.debug("Объединение рекомендаций от всех алгоритмов");
        
        Map<Long, ProductRecommendation> combinedMap = new HashMap<>();
        
                 // Веса для разных алгоритмов
         double contentWeight = 0.25;
         double collaborativeWeight = 0.2;
         double matrixWeight = 0.15;
         double clusterWeight = 0.1;
         double temporalWeight = 0.1;
         double trendWeight = 0.1;
         double personalizedWeight = 0.1;
         
         // Добавляем рекомендации с весами
         addRecommendationsWithWeight(combinedMap, contentBased, contentWeight);
         addRecommendationsWithWeight(combinedMap, collaborative, collaborativeWeight);
         addRecommendationsWithWeight(combinedMap, matrixFactorization, matrixWeight);
         addRecommendationsWithWeight(combinedMap, clustering, clusterWeight);
         addRecommendationsWithWeight(combinedMap, temporal, temporalWeight);
         addRecommendationsWithWeight(combinedMap, trendBased, trendWeight);
         addRecommendationsWithWeight(combinedMap, personalized, personalizedWeight);
        
        // Сортируем по скору и ограничиваем количество
        return combinedMap.values().stream()
                .sorted((r1, r2) -> r2.getScore().compareTo(r1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Добавляет рекомендации с весами в общую карту
     */
    private void addRecommendationsWithWeight(Map<Long, ProductRecommendation> combinedMap, 
                                           List<ProductRecommendation> recommendations, 
                                           double weight) {
        for (ProductRecommendation rec : recommendations) {
            Long productId = rec.getProduct().getId();
            if (combinedMap.containsKey(productId)) {
                // Объединяем скоры с весами
                ProductRecommendation existing = combinedMap.get(productId);
                BigDecimal weightedScore = rec.getScore().multiply(BigDecimal.valueOf(weight));
                BigDecimal combinedScore = existing.getScore().add(weightedScore);
                existing.setScore(combinedScore);
                existing.setAlgorithm(ProductRecommendation.AlgorithmType.HYBRID);
            } else {
                // Создаем новую с весом
                ProductRecommendation weightedRec = new ProductRecommendation();
                weightedRec.setUser(rec.getUser());
                weightedRec.setProduct(rec.getProduct());
                weightedRec.setScore(rec.getScore().multiply(BigDecimal.valueOf(weight)));
                weightedRec.setAlgorithm(rec.getAlgorithm());
                combinedMap.put(productId, weightedRec);
            }
        }
    }

    /**
     * Находит похожих пользователей на основе предпочтений
     */
    private List<User> findSimilarUsers(User user) {
        List<UserPreference> userPrefs = userPreferenceRepository.findByUserId(user.getId());
        if (userPrefs.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<User> allUsers = userRepository.findAll();
        List<User> similarUsers = new ArrayList<>();
        
        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(user.getId())) {
                continue; // Пропускаем самого пользователя
            }
            
            List<UserPreference> otherPrefs = userPreferenceRepository.findByUserId(otherUser.getId());
            if (!otherPrefs.isEmpty()) {
                double similarity = calculateUserSimilarity(userPrefs, otherPrefs);
                if (similarity >= minSimilarity) {
                    similarUsers.add(otherUser);
                }
            }
        }
        
        // Сортируем по схожести и ограничиваем количество
        return similarUsers.stream()
                .sorted((u1, u2) -> {
                    List<UserPreference> prefs1 = userPreferenceRepository.findByUserId(u1.getId());
                    List<UserPreference> prefs2 = userPreferenceRepository.findByUserId(u2.getId());
                    double sim1 = calculateUserSimilarity(userPrefs, prefs1);
                    double sim2 = calculateUserSimilarity(userPrefs, prefs2);
                    return Double.compare(sim2, sim1);
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Вычисляет схожесть пользователей на основе предпочтений
     */
    private double calculateUserSimilarity(List<UserPreference> prefs1, List<UserPreference> prefs2) {
        if (prefs1.isEmpty() || prefs2.isEmpty()) {
            return 0.0;
        }
        
        // Используем Jaccard similarity для категорий
        Set<String> categories1 = prefs1.stream()
                .map(UserPreference::getCategory)
                .collect(Collectors.toSet());
        
        Set<String> categories2 = prefs2.stream()
                .map(UserPreference::getCategory)
                .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(categories1);
        intersection.retainAll(categories2);
        
        Set<String> union = new HashSet<>(categories1);
        union.addAll(categories2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Улучшенный расчет content-based скора
     */
    private BigDecimal calculateEnhancedContentBasedScore(Product product, List<UserPreference> preferences, User user) {
        if (product.getCategory() == null || preferences.isEmpty()) {
            return BigDecimal.valueOf(0.3);
        }
        
        BigDecimal totalScore = BigDecimal.ZERO;
        int matchedPreferences = 0;
        
        // Базовый скор по категории
        for (UserPreference preference : preferences) {
            if (product.getCategory().equals(preference.getCategory())) {
                totalScore = totalScore.add(preference.getWeight());
                matchedPreferences++;
            }
        }
        
        // Дополнительные факторы
        BigDecimal sourceBonus = calculateSourceBonus(product.getSource(), user);
        BigDecimal priceBonus = calculatePriceBonus(product, user);
        BigDecimal popularityBonus = calculatePopularityBonus(product);
        
        totalScore = totalScore.add(sourceBonus).add(priceBonus).add(popularityBonus);
        
        if (matchedPreferences == 0) {
            return totalScore.max(BigDecimal.valueOf(0.3));
        }
        
        return totalScore.divide(BigDecimal.valueOf(matchedPreferences), 4, RoundingMode.HALF_UP);
    }

    /**
     * Вычисляет бонус за источник товара
     */
    private BigDecimal calculateSourceBonus(String source, User user) {
        if (source == null) return BigDecimal.ZERO;
        
        // Анализируем предпочтения пользователя по источникам
        List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
        Map<String, Integer> sourceCounts = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            String behaviorSource = behavior.getProduct().getSource();
            if (behaviorSource != null) {
                sourceCounts.merge(behaviorSource, 1, Integer::sum);
            }
        }
        
        Integer sourceCount = sourceCounts.get(source);
        if (sourceCount != null && sourceCount > 0) {
            return BigDecimal.valueOf(Math.min(0.2, sourceCount * 0.05));
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Вычисляет бонус за цену товара
     */
    private BigDecimal calculatePriceBonus(Product product, User user) {
        try {
            // Получаем текущую цену товара
            List<PriceHistory> priceHistory = priceHistoryRepository.findByProductId(product.getId());
            if (priceHistory.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Сортируем по дате и берем последнюю цену
            priceHistory.sort((p1, p2) -> p2.getCheckedAt().compareTo(p1.getCheckedAt()));
            BigDecimal currentPrice = priceHistory.get(0).getPrice();
            
            // Анализируем ценовые предпочтения пользователя
            List<UserBehavior> userBehaviors = userBehaviorRepository.findByUserId(user.getId());
            List<BigDecimal> userPrices = new ArrayList<>();
            
            for (UserBehavior behavior : userBehaviors) {
                List<PriceHistory> productPrices = priceHistoryRepository.findByProductId(behavior.getProduct().getId());
                if (!productPrices.isEmpty()) {
                    // Сортируем по дате и берем последнюю цену
                    productPrices.sort((p1, p2) -> p2.getCheckedAt().compareTo(p1.getCheckedAt()));
                    userPrices.add(productPrices.get(0).getPrice());
                }
            }
            
            if (userPrices.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Вычисляем среднюю цену и стандартное отклонение
            BigDecimal avgPrice = userPrices.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(userPrices.size()), 2, RoundingMode.HALF_UP);
            
            // Если цена товара близка к средней цене пользователя, даем бонус
            BigDecimal priceDiff = currentPrice.subtract(avgPrice).abs();
            BigDecimal avgPricePercent = avgPrice.multiply(BigDecimal.valueOf(0.2)); // 20% от средней цены
            
            if (priceDiff.compareTo(avgPricePercent) <= 0) {
                return BigDecimal.valueOf(0.15); // Бонус за подходящую цену
            } else if (currentPrice.compareTo(avgPrice) < 0) {
                return BigDecimal.valueOf(0.1); // Небольшой бонус за более низкую цену
            }
            
            return BigDecimal.ZERO;
            
        } catch (Exception e) {
            log.warn("Ошибка при вычислении ценового бонуса для товара {}", product.getId(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Вычисляет бонус за популярность товара
     */
    private BigDecimal calculatePopularityBonus(Product product) {
        try {
            // Подсчитываем количество взаимодействий с товаром
            List<UserBehavior> productBehaviors = userBehaviorRepository.findByProductId(product.getId());
            
            if (productBehaviors.isEmpty()) {
                return BigDecimal.ZERO;
            }
            
            // Подсчитываем положительные взаимодействия (просмотры, добавления в список)
            long positiveInteractions = productBehaviors.stream()
                    .filter(b -> b.getBehaviorType() == UserBehavior.BehaviorType.VIEW || 
                                b.getBehaviorType() == UserBehavior.BehaviorType.WATCH_ADD)
                    .count();
            
            // Нормализуем популярность (0.0 - 0.2)
            double popularityScore = Math.min(0.2, positiveInteractions * 0.01);
            
            return BigDecimal.valueOf(popularityScore);
            
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Улучшенный поиск похожих пользователей
     */
    private List<User> findSimilarUsersEnhanced(User user) {
        List<UserPreference> userPrefs = userPreferenceRepository.findByUserId(user.getId());
        if (userPrefs.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<User> allUsers = userRepository.findAll();
        List<User> similarUsers = new ArrayList<>();
        
        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(user.getId())) {
                continue;
            }
            
            List<UserPreference> otherPrefs = userPreferenceRepository.findByUserId(otherUser.getId());
            if (!otherPrefs.isEmpty()) {
                double similarity = calculateEnhancedUserSimilarity(userPrefs, otherPrefs, user, otherUser);
                if (similarity >= minSimilarity) {
                    similarUsers.add(otherUser);
                }
            }
        }
        
        return similarUsers.stream()
                .sorted((u1, u2) -> {
                    List<UserPreference> prefs1 = userPreferenceRepository.findByUserId(u1.getId());
                    List<UserPreference> prefs2 = userPreferenceRepository.findByUserId(u2.getId());
                    double sim1 = calculateEnhancedUserSimilarity(userPrefs, prefs1, user, u1);
                    double sim2 = calculateEnhancedUserSimilarity(userPrefs, prefs2, user, u2);
                    return Double.compare(sim2, sim1);
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Улучшенный расчет схожести пользователей
     */
    private double calculateEnhancedUserSimilarity(List<UserPreference> prefs1, List<UserPreference> prefs2, 
                                                 User user1, User user2) {
        if (prefs1.isEmpty() || prefs2.isEmpty()) {
            return 0.0;
        }
        
        // Jaccard similarity для категорий
        Set<String> categories1 = prefs1.stream()
                .map(UserPreference::getCategory)
                .collect(Collectors.toSet());
        
        Set<String> categories2 = prefs2.stream()
                .map(UserPreference::getCategory)
                .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(categories1);
        intersection.retainAll(categories2);
        
        Set<String> union = new HashSet<>(categories1);
        union.addAll(categories2);
        
        double categorySimilarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        
        // Дополнительная схожесть по поведению
        double behaviorSimilarity = calculateBehaviorSimilarity(user1, user2);
        
        // Взвешенная схожесть
        return categorySimilarity * 0.7 + behaviorSimilarity * 0.3;
    }

    /**
     * Вычисляет схожесть поведения пользователей
     */
    private double calculateBehaviorSimilarity(User user1, User user2) {
        List<UserBehavior> behaviors1 = userBehaviorRepository.findByUserId(user1.getId());
        List<UserBehavior> behaviors2 = userBehaviorRepository.findByUserId(user2.getId());
        
        if (behaviors1.isEmpty() || behaviors2.isEmpty()) {
            return 0.0;
        }
        
        // Сравниваем типы поведения
        Set<UserBehavior.BehaviorType> types1 = behaviors1.stream()
                .map(UserBehavior::getBehaviorType)
                .collect(Collectors.toSet());
        
        Set<UserBehavior.BehaviorType> types2 = behaviors2.stream()
                .map(UserBehavior::getBehaviorType)
                .collect(Collectors.toSet());
        
        Set<UserBehavior.BehaviorType> intersection = new HashSet<>(types1);
        intersection.retainAll(types2);
        
        Set<UserBehavior.BehaviorType> union = new HashSet<>(types1);
        union.addAll(types2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Улучшенный скор для типа поведения с учетом времени
     */
    private BigDecimal getEnhancedBehaviorScore(UserBehavior.BehaviorType behaviorType, LocalDateTime createdAt) {
        BigDecimal baseScore = switch (behaviorType) {
            case WATCH_ADD -> BigDecimal.valueOf(0.8);
            case VIEW -> BigDecimal.valueOf(0.5);
            case NOTIFICATION_CLICK -> BigDecimal.valueOf(0.6);
            default -> BigDecimal.valueOf(0.3);
        };
        
        // Временной фактор (более свежие действия имеют больший вес)
        if (createdAt != null) {
            long daysSince = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
            double timeFactor = Math.max(0.5, 1.0 - (daysSince * 0.1));
            baseScore = baseScore.multiply(BigDecimal.valueOf(timeFactor));
        }
        
        return baseScore;
    }

    /**
     * Возвращает скор для типа поведения
     */
    private BigDecimal getBehaviorScore(UserBehavior.BehaviorType behaviorType) {
        return switch (behaviorType) {
            case WATCH_ADD -> BigDecimal.valueOf(0.8);
            case VIEW -> BigDecimal.valueOf(0.5);
            case NOTIFICATION_CLICK -> BigDecimal.valueOf(0.6);
            default -> BigDecimal.valueOf(0.3);
        };
    }

    /**
     * Проверяет, находится ли товар в списке наблюдения пользователя
     */
    private boolean isProductInUserWatchList(User user, Long productId) {
        try {
            // Проверяем через NotificationRepository - если есть уведомление для этого товара,
            // значит товар в списке наблюдения пользователя
            List<Notification> userNotifications = notificationRepository.findByUserId(user.getId());
            return userNotifications.stream()
                    .anyMatch(notification -> notification.getProduct().getId().equals(productId));
        } catch (Exception e) {
            log.warn("Ошибка при проверке списка наблюдения для пользователя {} и товара {}", 
                     user.getEmail(), productId, e);
            return false;
        }
    }

    /**
     * Создает предпочтения по умолчанию для пользователя
     */
    private List<UserPreference> getDefaultPreferences(User user) {
        List<UserPreference> defaultPrefs = new ArrayList<>();
        
        // Получаем популярные категории
        List<String> popularCategories = Arrays.asList("electronics", "clothing", "books", "home");
        
        for (String category : popularCategories) {
            UserPreference pref = new UserPreference();
            pref.setUser(user);
            pref.setCategory(category);
            pref.setWeight(BigDecimal.valueOf(0.5));
            defaultPrefs.add(pref);
        }
        
        return defaultPrefs;
    }

    /**
     * Конвертирует ProductRecommendation в RecommendationDto
     */
    private RecommendationDto convertToDto(ProductRecommendation recommendation) {
        RecommendationDto dto = new RecommendationDto();
        dto.setId(recommendation.getId());
        dto.setProductId(recommendation.getProduct().getId());
        dto.setProductName(recommendation.getProduct().getName());
        dto.setProductUrl(recommendation.getProduct().getUrl());
        dto.setProductCategory(recommendation.getProduct().getCategory());
        dto.setProductSource(recommendation.getProduct().getSource());
        dto.setScore(recommendation.getScore());
        dto.setAlgorithm(recommendation.getAlgorithm().name());
        dto.setIsViewed(recommendation.getIsViewed());
        return dto;
    }

    /**
     * Обновляет предпочтения пользователя на основе его поведения
     */
    @Transactional
    public void updateUserPreferences(User user) {
        log.info("Обновление предпочтений пользователя {}", user.getEmail());
        
        try {
            // Анализируем поведение пользователя
            List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
            Map<String, Integer> categoryCounts = new HashMap<>();
            
            for (UserBehavior behavior : behaviors) {
                String category = behavior.getProduct().getCategory();
                if (category != null) {
                    categoryCounts.merge(category, 1, Integer::sum);
                }
            }
            
            // Обновляем веса предпочтений
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                String category = entry.getKey();
                int count = entry.getValue();
                
                // Вычисляем новый вес на основе частоты взаимодействий
                double newWeight = Math.min(1.0, 0.5 + (count * 0.1));
                
                UserPreference preference = userPreferenceRepository.findByUserIdAndCategory(user.getId(), category)
                        .orElse(new UserPreference());
                
                preference.setUser(user);
                preference.setCategory(category);
                preference.setWeight(BigDecimal.valueOf(newWeight));
                
                userPreferenceRepository.save(preference);
            }
            
            log.info("Предпочтения пользователя {} обновлены", user.getEmail());
            
        } catch (Exception e) {
            log.error("Ошибка при обновлении предпочтений пользователя {}", user.getEmail(), e);
        }
    }

    /**
     * Отмечает рекомендацию как просмотренную
     */
    @Transactional
    public void markAsViewed(Long recommendationId) {
        log.info("Отметка рекомендации {} как просмотренной", recommendationId);
        
        try {
            Optional<ProductRecommendation> opt = productRecommendationRepository.findById(recommendationId);
            if (opt.isPresent()) {
                ProductRecommendation recommendation = opt.get();
                recommendation.setIsViewed(true);
                recommendation.setUpdatedAt(LocalDateTime.now());
                productRecommendationRepository.save(recommendation);
                // Трекинг просмотра товара
                try {
                    User user = recommendation.getUser();
                    Product product = recommendation.getProduct();
                    if (user != null && product != null) {
                        userBehaviorTrackingService.trackProductView(user, product);
                    }
                } catch (Exception ignored) {}
                log.info("Рекомендация {} отмечена как просмотренная", recommendationId);
            }
        } catch (Exception e) {
            log.error("Ошибка при отметке рекомендации {} как просмотренной", recommendationId, e);
        }
    }

    /**
     * Получает рекомендации пользователя
     */
    public List<RecommendationDto> getUserRecommendations(User user, Integer limit) {
        if (limit == null) limit = defaultLimit;
        
        log.debug("Получение рекомендаций для пользователя {} (лимит: {})", user.getEmail(), limit);
        
        List<ProductRecommendation> recommendations = productRecommendationRepository
                .findByUserIdOrderByScoreDesc(user.getId());
        
        // Ограничиваем количество результатов
        if (recommendations.size() > limit) {
            recommendations = recommendations.subList(0, limit);
        }
        
        return recommendations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает количество непросмотренных рекомендаций
     */
    public Long getUnviewedRecommendationsCount(User user) {
        log.debug("Получение количества непросмотренных рекомендаций для пользователя {}", user.getEmail());
        
        return productRecommendationRepository.countUnviewedRecommendations(user.getId());
    }

    /**
     * Строит матрицу взаимодействий пользователь-товар
     */
    private Map<Long, Map<Long, BigDecimal>> buildUserProductMatrix() {
        Map<Long, Map<Long, BigDecimal>> matrix = new HashMap<>();
        
        List<UserBehavior> allBehaviors = userBehaviorRepository.findAll();
        for (UserBehavior behavior : allBehaviors) {
            Long userId = behavior.getUser().getId();
            Long productId = behavior.getProduct().getId();
            BigDecimal score = getBehaviorScore(behavior.getBehaviorType());
            
            matrix.computeIfAbsent(userId, k -> new HashMap<>());
            matrix.get(userId).merge(productId, score, BigDecimal::add);
        }
        
        return matrix;
    }

    /**
     * Разлагает пользовательские факторы
     */
    private Map<Long, BigDecimal> decomposeUserFactors(Long userId, Map<Long, Map<Long, BigDecimal>> matrix) {
        // Упрощенная реализация SVD
        Map<Long, BigDecimal> factors = new HashMap<>();
        Map<Long, BigDecimal> userInteractions = matrix.get(userId);
        
        if (userInteractions != null) {
            for (Map.Entry<Long, BigDecimal> entry : userInteractions.entrySet()) {
                factors.put(entry.getKey(), entry.getValue());
            }
        }
        
        return factors;
    }

    /**
     * Разлагает товарные факторы
     */
    private Map<Long, BigDecimal> decomposeProductFactors(Map<Long, Map<Long, BigDecimal>> matrix) {
        // Упрощенная реализация
        Map<Long, BigDecimal> factors = new HashMap<>();
        
        for (Map<Long, BigDecimal> userInteractions : matrix.values()) {
            for (Map.Entry<Long, BigDecimal> entry : userInteractions.entrySet()) {
                factors.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
            }
        }
        
        return factors;
    }

    /**
     * Вычисляет скор матричной факторизации
     */
    private BigDecimal calculateMatrixFactorizationScore(Map<Long, BigDecimal> userFactors, 
                                                       Map<Long, BigDecimal> productFactors, 
                                                       Long productId) {
        BigDecimal userFactor = userFactors.getOrDefault(productId, BigDecimal.ZERO);
        BigDecimal productFactor = productFactors.getOrDefault(productId, BigDecimal.ZERO);
        
        return userFactor.multiply(productFactor).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /**
     * Находит кластер пользователя
     */
    private Integer findUserCluster(User user) {
        // Упрощенная кластеризация по категориям предпочтений
        List<UserPreference> preferences = userPreferenceRepository.findByUserId(user.getId());
        if (preferences.isEmpty()) {
            return 0; // Дефолтный кластер
        }
        
        // Простая кластеризация по количеству предпочтений
        int clusterSize = preferences.size();
        if (clusterSize <= 2) return 0;
        else if (clusterSize <= 4) return 1;
        else if (clusterSize <= 6) return 2;
        else if (clusterSize <= 8) return 3;
        else return 4;
    }

    /**
     * Получает популярные товары кластера
     */
    private List<Long> getClusterPopularProducts(Integer clusterId) {
        // TODO: Реализовать получение популярных товаров кластера
        return new ArrayList<>();
    }

    /**
     * Анализирует временные паттерны пользователя
     */
    private Map<String, Integer> analyzeTimePatterns(User user) {
        Map<String, Integer> patterns = new HashMap<>();
        List<UserBehavior> behaviors = userBehaviorRepository.findByUserId(user.getId());
        
        for (UserBehavior behavior : behaviors) {
            String timePattern = getTimePattern(behavior.getCreatedAt());
            patterns.merge(timePattern, 1, Integer::sum);
        }
        
        return patterns;
    }

    /**
     * Получает текущий временной паттерн
     */
    private String getCurrentTimePattern() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        if (hour >= 6 && hour < 12) return "morning";
        else if (hour >= 12 && hour < 18) return "afternoon";
        else if (hour >= 18 && hour < 22) return "evening";
        else return "night";
    }

    /**
     * Получает временной паттерн для даты
     */
    private String getTimePattern(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        
        if (hour >= 6 && hour < 12) return "morning";
        else if (hour >= 12 && hour < 18) return "afternoon";
        else if (hour >= 18 && hour < 22) return "evening";
        else return "night";
    }

    /**
     * Получает товары для временного паттерна
     */
    private List<Product> getProductsForTimePattern(String timePattern, Map<String, Integer> patterns) {
        // TODO: Реализовать получение товаров для временного паттерна
        return new ArrayList<>();
    }

    /**
     * Сохраняет рекомендации в базу данных
     */
    private List<ProductRecommendation> saveRecommendations(User user, List<ProductRecommendation> recommendations) {
        List<ProductRecommendation> saved = new ArrayList<>();
        
        for (ProductRecommendation rec : recommendations) {
            // Проверяем, не существует ли уже такая рекомендация
            Optional<ProductRecommendation> existing = productRecommendationRepository
                    .findByUserIdAndProductId(user.getId(), rec.getProduct().getId());
            
            if (existing.isPresent()) {
                // Обновляем существующую
                ProductRecommendation existingRec = existing.get();
                existingRec.setScore(rec.getScore());
                existingRec.setAlgorithm(rec.getAlgorithm());
                existingRec.setUpdatedAt(LocalDateTime.now());
                saved.add(productRecommendationRepository.save(existingRec));
            } else {
                // Создаем новую
                saved.add(productRecommendationRepository.save(rec));
            }
        }
        
        return saved;
    }
}
