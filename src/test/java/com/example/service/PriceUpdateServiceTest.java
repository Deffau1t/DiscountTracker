package com.example.service;

import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.repository.PriceHistoryRepository;
import com.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceUpdateServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private ParserService parserService;

    @InjectMocks
    private PriceUpdateService priceUpdateService;

    private Product testProduct;
    private BigDecimal initialPrice;
    private BigDecimal newPrice;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setUrl("https://www.citilink.ru/product/test");
        testProduct.setSource("citilink");
        testProduct.setCategory("video");
        initialPrice = new BigDecimal("1000.00");
        newPrice = new BigDecimal("900.00");
    }

    @Test
    void whenPriceDecreases_shouldSavePriceHistory() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(testProduct));
        when(parserService.parsePrice(testProduct)).thenReturn(newPrice);

        // Act
        priceUpdateService.updatePrices();

        // Assert
        verify(priceHistoryRepository, times(1)).save(any(PriceHistory.class));
    }

    @Test
    void whenPriceIncreases_shouldSavePriceHistory() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(testProduct));
        when(parserService.parsePrice(testProduct)).thenReturn(new BigDecimal("1100.00"));

        // Act
        priceUpdateService.updatePrices();

        // Assert
        verify(priceHistoryRepository, times(1)).save(any(PriceHistory.class));
    }

    @Test
    void whenMultipleProducts_shouldUpdateAllPrices() {
        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setUrl("https://www.citilink.ru/product/1");
        product1.setSource("citilink");
        
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setUrl("https://www.citilink.ru/product/2");
        product2.setSource("citilink");

        when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));
        when(parserService.parsePrice(any(Product.class))).thenReturn(newPrice);

        // Act
        priceUpdateService.updatePrices();

        // Assert
        verify(priceHistoryRepository, times(2)).save(any(PriceHistory.class));
    }

    @Test
    void whenParserReturnsNull_shouldNotSavePriceHistory() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(testProduct));
        when(parserService.parsePrice(testProduct)).thenReturn(null);

        // Act
        priceUpdateService.updatePrices();

        // Assert
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
    }
} 