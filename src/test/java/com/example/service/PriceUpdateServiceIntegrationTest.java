package com.example.service;

import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.repository.PriceHistoryRepository;
import com.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PriceUpdateServiceIntegrationTest {

    @Autowired
    private PriceUpdateService priceUpdateService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private ParserService parserService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        priceHistoryRepository.deleteAll();
        productRepository.deleteAll();

        // Создаем тестовый продукт
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setUrl("https://www.citilink.ru/product/test");
        testProduct.setSource("citilink");
        testProduct.setCategory("video");
        testProduct = productRepository.save(testProduct);
    }

    @Test
    void whenPriceChanges_shouldSavePriceHistory() {
        // Arrange
        BigDecimal initialPrice = new BigDecimal("1000.00");
        BigDecimal newPrice = new BigDecimal("900.00");

        // Act
        priceUpdateService.updatePrices();

        // Assert
        List<PriceHistory> priceHistory = priceHistoryRepository.findByProductId(testProduct.getId());
        assertFalse(priceHistory.isEmpty());
        assertNotNull(priceHistory.get(0).getPrice());
    }

    @Test
    void whenMultiplePriceUpdates_shouldSaveAllHistory() {
        // Arrange
        int numberOfUpdates = 3;

        // Act
        for (int i = 0; i < numberOfUpdates; i++) {
            priceUpdateService.updatePrices();
        }

        // Assert
        List<PriceHistory> priceHistory = priceHistoryRepository.findByProductId(testProduct.getId());
        assertEquals(numberOfUpdates, priceHistory.size());
    }

    @Test
    void whenProductNotFound_shouldNotThrowException() {
        // Arrange
        productRepository.deleteAll();

        // Act & Assert
        assertDoesNotThrow(() -> priceUpdateService.updatePrices());
    }
} 