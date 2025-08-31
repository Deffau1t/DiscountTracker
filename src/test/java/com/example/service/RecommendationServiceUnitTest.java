package com.example.service;

import com.example.dto.RecommendationDto;
import com.example.entity.Notification;
import com.example.entity.Product;
import com.example.entity.ProductRecommendation;
import com.example.entity.User;
import com.example.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceUnitTest {

	@Mock
	private UserPreferenceRepository userPreferenceRepository;

	@Mock
	private UserBehaviorRepository userBehaviorRepository;

	@Mock
	private ProductRecommendationRepository productRecommendationRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PriceHistoryRepository priceHistoryRepository;

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private TrendAnalysisService trendAnalysisService;

	@Mock
	private PersonalizationService personalizationService;

	@Mock
	private UserBehaviorTrackingService userBehaviorTrackingService;

	@InjectMocks
	private RecommendationService recommendationService;

	private User user;
	private Product productElectronics;
	private Product productBooks;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(1L);
		user.setEmail("user@example.com");

		productElectronics = new Product();
		productElectronics.setId(10L);
		productElectronics.setName("Phone");
		productElectronics.setUrl("http://example.com/p1");
		productElectronics.setSource("store");
		productElectronics.setCategory("electronics");

		productBooks = new Product();
		productBooks.setId(20L);
		productBooks.setName("Book");
		productBooks.setUrl("http://example.com/p2");
		productBooks.setSource("store");
		productBooks.setCategory("books");
	}

	@Test
	void generateRecommendations_usesFallbackFromNotifications_andAppliesCategoryFocus() {
		// Все алгоритмы вернут пусто: нет поведений, нет трендов, нет товаров для персонализации
		when(userPreferenceRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());
		when(userBehaviorRepository.findAll()).thenReturn(Collections.emptyList());
		when(userBehaviorRepository.findByUserId(user.getId())).thenReturn(Collections.emptyList());
		when(trendAnalysisService.getProductsWithPositivePriceTrend()).thenReturn(Collections.emptyList());
		when(trendAnalysisService.getProductsWithGrowingPopularity()).thenReturn(Collections.emptyList());
		when(productRepository.findAll()).thenReturn(Collections.emptyList());
		when(productRepository.findAllById(any())).thenReturn(Collections.emptyList());
		when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

		// Фолбэк по уведомлениям: 2 уведомления по electronics и 1 по books
		Notification n1 = new Notification();
		n1.setUser(user);
		n1.setProduct(productElectronics);
		Notification n2 = new Notification();
		n2.setUser(user);
		n2.setProduct(productElectronics);
		Notification n3 = new Notification();
		n3.setUser(user);
		n3.setProduct(productBooks);

		when(notificationRepository.findAll()).thenReturn(Arrays.asList(n1, n2, n3));
		when(notificationRepository.findByUserId(user.getId())).thenReturn(Arrays.asList(n1, n2, n3));

		// Сохранение рекомендаций
		when(productRecommendationRepository.findByUserIdAndProductId(eq(user.getId()), anyLong()))
				.thenReturn(Optional.empty());
		when(productRecommendationRepository.save(any(ProductRecommendation.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		List<RecommendationDto> result = recommendationService.generateRecommendations(user, 10);

		// Ожидаем, что останется только топ-категория (electronics) и скор будет умножен на 1.5
		assertNotNull(result);
		assertEquals(1, result.size());
		RecommendationDto dto = result.get(0);
		assertEquals(productElectronics.getId(), dto.getProductId());
		assertEquals("electronics", dto.getProductCategory());
		assertEquals("TREND_BASED", dto.getAlgorithm()); // алгоритм фолбэка
		assertEquals(0, dto.getScore().compareTo(BigDecimal.valueOf(1.5))); // 1.0 * 1.5
	}

	@Test
	void getUserRecommendations_appliesLimit_andMapsDtoFields() {
		Product p1 = productElectronics;
		Product p2 = productBooks;

		ProductRecommendation r1 = new ProductRecommendation();
		r1.setId(101L);
		r1.setUser(user);
		r1.setProduct(p1);
		r1.setScore(BigDecimal.valueOf(0.9));
		r1.setAlgorithm(ProductRecommendation.AlgorithmType.CONTENT_BASED);

		ProductRecommendation r2 = new ProductRecommendation();
		r2.setId(102L);
		r2.setUser(user);
		r2.setProduct(p2);
		r2.setScore(BigDecimal.valueOf(0.8));
		r2.setAlgorithm(ProductRecommendation.AlgorithmType.COLLABORATIVE);

		ProductRecommendation r3 = new ProductRecommendation();
		r3.setId(103L);
		r3.setUser(user);
		r3.setProduct(p2);
		r3.setScore(BigDecimal.valueOf(0.7));
		r3.setAlgorithm(ProductRecommendation.AlgorithmType.TEMPORAL);

		when(productRecommendationRepository.findByUserIdOrderByScoreDesc(user.getId()))
				.thenReturn(Arrays.asList(r1, r2, r3));

		List<RecommendationDto> dtos = recommendationService.getUserRecommendations(user, 2);

		assertNotNull(dtos);
		assertEquals(2, dtos.size());
		assertEquals(p1.getId(), dtos.get(0).getProductId());
		assertEquals(p1.getName(), dtos.get(0).getProductName());
		assertEquals(p1.getUrl(), dtos.get(0).getProductUrl());
		assertEquals(p1.getCategory(), dtos.get(0).getProductCategory());
		assertEquals("CONTENT_BASED", dtos.get(0).getAlgorithm());
		assertEquals(0, dtos.get(0).getScore().compareTo(BigDecimal.valueOf(0.9)));
	}
}


