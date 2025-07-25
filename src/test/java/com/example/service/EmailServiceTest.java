package com.example.service;

import com.example.entity.Product;
import com.example.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private User testUser;
    private Product testProduct;
    private BigDecimal testPrice;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setUrl("https://www.citilink.ru/product/test");

        testPrice = new BigDecimal("900.00");
    }

    @Test
    void whenSendingNotification_shouldSendEmail() {
        // Act
        emailService.sendNotification(testUser, testProduct, testPrice);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
} 