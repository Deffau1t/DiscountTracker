package com.example.service;

import com.example.dto.RecommendationDto;
import com.example.entity.*;
import com.example.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;
    
    @Mock
    private UserBehaviorRepository userBehaviorRepository;
    
    @Mock
    private ProductRecommendationRepository recommendationRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private User testUser;
    private Product testProduct;
    private UserPreference testPreference;
    private PriceHistory testPriceHistory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setCategory("Electronics");
        testProduct.setSource("Test Store");

        testPreference = new UserPreference();
        testPreference.setId(1L);
        testPreference.setUser(testUser);
        testPreference.setCategory("Electronics");
        testPreference.setWeight(0.8);

        testPriceHistory = new PriceHistory();
        testPriceHistory.setId(1L);
        testPriceHistory.setProduct(testProduct);
        testPriceHistory.setPrice(new BigDecimal("1000.00"));
        testPriceHistory.setCheckedAt(LocalDateTime.now());
    }

    @Test
    void testGenerateRecommendations_WithUserPreferences() {
        // Arrange
        when(userPreferenceRepository.findByUser(testUser))
            .thenReturn(Arrays.asList(testPreference));
        when(productRepository.findByCategory("Electronics"))
            .thenReturn(Arrays.asList(testProduct));
        when(priceHistoryRepository.findByProductOrderByCheckedAtDesc(testProduct))
            .thenReturn(Arrays.asList(testPriceHistory));
        when(recommendationRepository.save(any(ProductRecommendation.class)))
            .thenReturn(new ProductRecommendation());

        // Act
        List<RecommendationDto> result = recommendationService.generateRecommendations(testUser, 5);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        
        RecommendationDto recommendation = result.get(0);
        assertEquals(testProduct.getId(), recommendation.getProductId());
        assertEquals(testProduct.getName(), recommendation.getProductName());
        assertEquals(testProduct.getCategory(), recommendation.getCategory());
        assertEquals(testPriceHistory.getPrice(), recommendation.getCurrentPrice());
        assertTrue(recommendation.getScore() > 0);
        assertNotNull(recommendation.getReason());
    }

    @Test
    void testGenerateRecommendations_WithoutUserPreferences() {
        // Arrange
        when(userPreferenceRepository.findByUser(testUser))
            .thenReturn(Collections.emptyList());
        when(userPreferenceRepository.findPopularCategories())
            .thenReturn(Arrays.asList(new Object[]{"Electronics", 0.7}));
        when(productRepository.findByCategory("Electronics"))
            .thenReturn(Arrays.asList(testProduct));
        when(priceHistoryRepository.findByProductOrderByCheckedAtDesc(testProduct))
            .thenReturn(Arrays.asList(testPriceHistory));
        when(recommendationRepository.save(any(ProductRecommendation.class)))
            .thenReturn(new ProductRecommendation());

        // Act
        List<RecommendationDto> result = recommendationService.generateRecommendations(testUser, 5);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        
        RecommendationDto recommendation = result.get(0);
        assertEquals(testProduct.getId(), recommendation.getProductId());
        assertTrue(recommendation.getReason().contains("Популярная категория"));
    }

    @Test
    void testGetUserRecommendations() {
        // Arrange
        ProductRecommendation existingRecommendation = new ProductRecommendation();
        existingRecommendation.setId(1L);
        existingRecommendation.setUser(testUser);
        existingRecommendation.setProduct(testProduct);
        existingRecommendation.setScore(0.8);
        existingRecommendation.setReason("Test reason");
        existingRecommendation.setGeneratedAt(LocalDateTime.now());

        when(recommendationRepository.findByUserOrderByScoreDesc(testUser))
            .thenReturn(Arrays.asList(existingRecommendation));
        when(priceHistoryRepository.findByProductOrderByCheckedAtDesc(testProduct))
            .thenReturn(Arrays.asList(testPriceHistory));

        // Act
        List<RecommendationDto> result = recommendationService.getUserRecommendations(testUser, 5);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        
        RecommendationDto recommendation = result.get(0);
        assertEquals(existingRecommendation.getScore(), recommendation.getScore());
        assertEquals(existingRecommendation.getReason(), recommendation.getReason());
    }

    @Test
    void testUpdateUserPreferences() {
        // Arrange
        UserBehavior behavior = new UserBehavior();
        behavior.setUser(testUser);
        behavior.setProduct(testProduct);
        behavior.setBehaviorType(UserBehavior.BehaviorType.ADD_TO_WATCH);
        behavior.setTimestamp(LocalDateTime.now());

        when(userBehaviorRepository.findByUserAndTimestampBetween(any(), any(), any()))
            .thenReturn(Arrays.asList(behavior));
        when(userPreferenceRepository.findByUserAndCategory(testUser, "Electronics"))
            .thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any(UserPreference.class)))
            .thenReturn(new UserPreference());

        // Act
        recommendationService.updateUserPreferences(testUser);

        // Assert
        verify(userPreferenceRepository, times(1)).save(any(UserPreference.class));
    }

    @Test
    void testMarkRecommendationAsViewed() {
        // Arrange
        ProductRecommendation recommendation = new ProductRecommendation();
        recommendation.setId(1L);
        recommendation.setUser(testUser);
        recommendation.setProduct(testProduct);

        when(recommendationRepository.findById(1L))
            .thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(ProductRecommendation.class)))
            .thenReturn(recommendation);

        // Act
        recommendationService.markRecommendationAsViewed(1L);

        // Assert
        verify(recommendationRepository, times(1)).save(recommendation);
        assertNotNull(recommendation.getViewedAt());
    }

    @Test
    void testMarkRecommendationAsViewed_NotFound() {
        // Arrange
        when(recommendationRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act
        recommendationService.markRecommendationAsViewed(999L);

        // Assert
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    void testCreateRecommendationDto() {
        // Arrange
        when(priceHistoryRepository.findByProductOrderByCheckedAtDesc(testProduct))
            .thenReturn(Arrays.asList(testPriceHistory));

        // Act
        List<RecommendationDto> result = recommendationService.generateRecommendations(testUser, 1);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        RecommendationDto dto = result.get(0);
        assertEquals(testProduct.getId(), dto.getProductId());
        assertEquals(testProduct.getName(), dto.getProductName());
        assertEquals(testProduct.getUrl(), dto.getProductUrl());
        assertEquals(testProduct.getCategory(), dto.getCategory());
        assertEquals(testProduct.getSource(), dto.getSource());
        assertEquals(testPriceHistory.getPrice(), dto.getCurrentPrice());
        assertNotNull(dto.getGeneratedAt());
        assertFalse(dto.isViewed());
    }
}
