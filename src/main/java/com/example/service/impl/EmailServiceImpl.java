package com.example.service.impl;

import com.example.entity.Product;
import com.example.entity.User;
import com.example.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    
    @Autowired
    private JavaMailSender emailSender;

    @Override
    public void sendNotification(User user, Product product, BigDecimal price) {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("Не удалось отправить уведомление: email пользователя не указан");
            return;
        }

        try {
            log.info("Начинаю отправку уведомления на email: {} для товара: {}", user.getEmail(), product.getName());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("qdaqda9@gmail.com"); // Добавляем отправителя
            message.setTo(user.getEmail());
            message.setSubject("Изменение цены на товар: " + product.getName());
            message.setText(String.format(
                "Здравствуйте!\n\n" +
                "Цена на товар %s изменилась и теперь составляет %s руб.\n\n" +
                "Ссылка на товар: %s\n\n" +
                "С уважением,\nDiscount Tracker", 
                product.getName(), 
                price.toString(),
                product.getUrl()
            ));
            
            log.info("Подготовка письма завершена, отправляю...");
            emailSender.send(message);
            log.info("✅ Уведомление успешно отправлено на email {} о товаре {}", user.getEmail(), product.getName());
        } catch (Exception e) {
            log.error("❌ Ошибка при отправке уведомления на email {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Не удалось отправить уведомление: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPriceNotification(String productName, BigDecimal price, String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("Не удалось отправить уведомление: email пользователя не указан");
            return;
        }

        try {
            log.info("Начинаю отправку уведомления о цене на email: {} для товара: {}", userEmail, productName);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("qdaqda9@gmail.com"); // Добавляем отправителя
            message.setTo(userEmail);
            message.setSubject("Изменение цены на товар: " + productName);
            message.setText(String.format(
                "Здравствуйте!\n\n" +
                "Цена на товар %s изменилась и теперь составляет %s руб.\n\n" +
                "С уважением,\nDiscount Tracker", 
                productName, 
                price.toString()
            ));
            
            log.info("Подготовка письма завершена, отправляю...");
            emailSender.send(message);
            log.info("✅ Уведомление успешно отправлено на email {} о товаре {}", userEmail, productName);
        } catch (Exception e) {
            log.error("❌ Ошибка при отправке уведомления на email {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Не удалось отправить уведомление: " + e.getMessage(), e);
        }
    }
}