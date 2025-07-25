package com.example.service;

import com.example.entity.Notification;
import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.repository.NotificationRepository;
import com.example.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PriceScraperService priceScraperService;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private User testUser;
    private Product testProduct;
    private Notification testNotification;
    private PriceHistory testPriceHistory;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        testUser.setCreatedAt(LocalDateTime.now());

        // Создаем тестовый продукт
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setUrl("https://www.citilink.ru/product/test");
        testProduct.setSource("citilink");
        testProduct.setCategory("video");
        testProduct.setCreatedAt(LocalDateTime.now());

        // Создаем тестовое уведомление
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUser(testUser);
        testNotification.setProduct(testProduct);
        testNotification.setThreshold(new BigDecimal("1000.00"));
        testNotification.setNotified(false);
        testNotification.setLastNotified(null);
        testNotification.setFrequency(Notification.NotificationFrequency.IMMEDIATELY);

        // Создаем тестовую историю цен
        testPriceHistory = new PriceHistory();
        testPriceHistory.setId(1L);
        testPriceHistory.setProduct(testProduct);
        testPriceHistory.setPrice(new BigDecimal("900.00"));
        testPriceHistory.setCheckedAt(LocalDateTime.now());
    }

    @Test
    void whenPriceBelowThreshold_shouldSendNotification() throws IOException {
        // Arrange
        when(notificationRepository.findAll()).thenReturn(List.of(testNotification));
        when(priceHistoryRepository.findByProductId(testProduct.getId()))
            .thenReturn(List.of(testPriceHistory));
        when(priceScraperService.scrapePrice(anyString(), anyString()))
            .thenReturn(new BigDecimal("900.00"));

        // Act
        notificationService.checkAndSendNotifications();

        // Assert
        verify(emailService, times(1)).sendNotification(
            eq(testUser),
            eq(testProduct),
            eq(new BigDecimal("900.00"))
        );
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        
        Notification savedNotification = notificationCaptor.getValue();
        assertTrue(savedNotification.isNotified());
        assertNotNull(savedNotification.getLastNotified());
    }

    @Test
    void whenPriceAboveThreshold_shouldNotSendNotification() throws IOException {
        // Arrange
        testPriceHistory.setPrice(new BigDecimal("1100.00"));
        when(notificationRepository.findAll()).thenReturn(List.of(testNotification));
        when(priceHistoryRepository.findByProductId(testProduct.getId()))
            .thenReturn(List.of(testPriceHistory));
        when(priceScraperService.scrapePrice(anyString(), anyString()))
            .thenReturn(new BigDecimal("1100.00"));

        // Act
        notificationService.checkAndSendNotifications();

        // Assert
        verify(emailService, never()).sendNotification(any(), any(), any());
    }

    @Test
    void whenMultiplePriceChanges_shouldSendNotificationOnlyOnce() throws IOException {
        // Arrange
        PriceHistory oldPrice = new PriceHistory();
        oldPrice.setProduct(testProduct);
        oldPrice.setPrice(new BigDecimal("1100.00"));
        oldPrice.setCheckedAt(LocalDateTime.now().minusHours(1));

        when(notificationRepository.findAll()).thenReturn(List.of(testNotification));
        when(priceHistoryRepository.findByProductId(testProduct.getId()))
            .thenReturn(Arrays.asList(oldPrice, testPriceHistory));
        when(priceScraperService.scrapePrice(anyString(), anyString()))
            .thenReturn(new BigDecimal("900.00"));

        // Act
        notificationService.checkAndSendNotifications();

        // Assert
        verify(emailService, times(1)).sendNotification(
            eq(testUser),
            eq(testProduct),
            eq(new BigDecimal("900.00"))
        );
    }
} 