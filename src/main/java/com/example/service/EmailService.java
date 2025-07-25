package com.example.service;

import com.example.entity.User;
import com.example.entity.Product;
import java.math.BigDecimal;

public interface EmailService {
    void sendNotification(User user, Product product, BigDecimal price);
    void sendPriceNotification(String productName, BigDecimal price, String userEmail);
}