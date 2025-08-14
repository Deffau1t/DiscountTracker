package com.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationConfig {

    /**
     * Минимальный вес для рекомендаций
     */
    private Double minWeight = 0.5;

    /**
     * Минимальная схожесть для collaborative фильтрации
     */
    private Double minSimilarity = 0.3;

    /**
     * Лимит по умолчанию для рекомендаций
     */
    private Integer defaultLimit = 10;

    /**
     * Количество факторов для матричной факторизации
     */
    private Integer matrixFactors = 10;

    /**
     * Количество кластеров для кластеризации
     */
    private Integer clusterK = 5;

    /**
     * Веса для разных алгоритмов
     */
    private AlgorithmWeights algorithmWeights = new AlgorithmWeights();

    /**
     * Настройки для анализа трендов
     */
    private TrendSettings trendSettings = new TrendSettings();

    /**
     * Настройки персонализации
     */
    private PersonalizationSettings personalizationSettings = new PersonalizationSettings();

    @Data
    public static class AlgorithmWeights {
        private Double contentBased = 0.25;
        private Double collaborative = 0.2;
        private Double matrixFactorization = 0.15;
        private Double clustering = 0.1;
        private Double temporal = 0.1;
        private Double trendBased = 0.1;
        private Double personalized = 0.1;
    }

    @Data
    public static class TrendSettings {
        private Integer minPriceHistorySize = 5;
        private Double positiveTrendThreshold = 0.05;
        private Double popularityGrowthThreshold = 0.1;
        private Double seasonalityThreshold = 0.5;
    }

    @Data
    public static class PersonalizationSettings {
        private Double timeWeight = 0.1;
        private Double sourceWeight = 0.3;
        private Double priceWeight = 0.2;
        private Double categoryWeight = 0.4;
        private Integer minInteractionsForAnalysis = 5;
    }
}
