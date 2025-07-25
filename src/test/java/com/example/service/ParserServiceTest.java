package com.example.service;

import com.example.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParserServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ParserService parserService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setUrl("https://www.citilink.ru/product/test");
        testProduct.setSource("citilink");
        testProduct.setCategory("video");
    }

    @Test
    void whenValidCitilinkUrl_shouldParsePrice() {
        // Arrange
        String mockResponse = """
            {
                "price": {
                    "value": 9999.99
                }
            }
            """;
        when(restTemplate.getForObject(anyString(), any())).thenReturn(mockResponse);

        // Act
        BigDecimal price = parserService.parsePrice(testProduct);

        // Assert
        assertNotNull(price);
        assertEquals(new BigDecimal("9999.99"), price);
    }

    @Test
    void whenInvalidUrl_shouldReturnNull() {
        // Arrange
        testProduct.setUrl("invalid-url");

        // Act
        BigDecimal price = parserService.parsePrice(testProduct);

        // Assert
        assertNull(price);
    }

    @Test
    void whenApiError_shouldReturnNull() {
        // Arrange
        when(restTemplate.getForObject(anyString(), any())).thenThrow(new RuntimeException("API Error"));

        // Act
        BigDecimal price = parserService.parsePrice(testProduct);

        // Assert
        assertNull(price);
    }
} 