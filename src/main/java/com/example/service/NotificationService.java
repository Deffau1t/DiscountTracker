package com.example.service;

import com.example.entity.Notification;
import com.example.entity.PriceHistory;
import com.example.repository.NotificationRepository;
import com.example.repository.PriceHistoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final EmailService emailService;
    private final PriceScraperService priceScraperService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkAndSendNotifications() {
        List<Notification> notifications = notificationRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        log.info("Начинаю проверку уведомлений. Всего уведомлений: {}", notifications.size());

        for (Notification notification : notifications) {
            try {
                log.info("Проверяю уведомление для товара: {} (ID: {})", 
                    notification.getProduct().getName(), 
                    notification.getProduct().getId());

                // Обновляем цену для внешних источников
                if (!notification.getProduct().getSource().equalsIgnoreCase("my")) {
                    try {
                        BigDecimal newPrice = priceScraperService.scrapePrice(
                            notification.getProduct().getUrl(),
                            notification.getUser().getEmail()
                        );
                        
                        if (newPrice != null) {
                            if (!newPrice.equals(notification.getProduct().getCurrentPrice())) {
                                log.info("Обнаружено изменение цены для {}: {} -> {}",
                                    notification.getProduct().getName(),
                                    notification.getProduct().getCurrentPrice(),
                                    newPrice);
                                    
                                PriceHistory priceHistory = new PriceHistory();
                                priceHistory.setProduct(notification.getProduct());
                                priceHistory.setPrice(newPrice);
                                priceHistory.setCheckedAt(now);
                                priceHistoryRepository.save(priceHistory);
                                
                                notification.getProduct().setCurrentPrice(newPrice);
                            }
                        }
                    } catch (Exception e) {
                        log.error("❌ Ошибка при получении цены для {}: {}", 
                            notification.getProduct().getUrl(), 
                            e.getMessage());
                        continue;
                    }
                }

                List<PriceHistory> history = priceHistoryRepository.findByProductId(notification.getProduct().getId());
                if (history.isEmpty()) {
                    log.info("История цен пуста для товара: {}", notification.getProduct().getName());
                    continue;
                }

                PriceHistory latestPrice = history.stream()
                    .max(Comparator.comparing(PriceHistory::getCheckedAt))
                    .orElse(null);

                if (latestPrice == null) {
                    log.info("Не найдена последняя цена для товара: {}", notification.getProduct().getName());
                    continue;
                }

                BigDecimal currentPrice = latestPrice.getPrice();
                BigDecimal threshold = notification.getThreshold();

                log.info("Текущая цена: {}, Порог: {}, Статус уведомления: {}, Последнее уведомление: {}", 
                    currentPrice, threshold, notification.isNotified(), notification.getLastNotified());

                // Проверяем, изменилась ли цена с момента последнего уведомления
                boolean priceChanged = notification.getLastNotified() == null || 
                    history.stream()
                        .filter(ph -> ph.getCheckedAt().isAfter(notification.getLastNotified()))
                        .anyMatch(ph -> !ph.getPrice().equals(currentPrice));

                // Проверяем, прошло ли достаточно времени с последнего уведомления
                boolean timeToNotify = notification.getNextNotificationTime() == null || 
                                     now.isAfter(notification.getNextNotificationTime());

                boolean shouldNotify = currentPrice.compareTo(threshold) <= 0 && 
                                     (!notification.isNotified() || timeToNotify) &&
                                     priceChanged;

                if (shouldNotify) {
                    log.info("Отправляю уведомление для товара {} пользователю {} (цена: {}, порог: {}, цена изменилась: {})", 
                        notification.getProduct().getName(), 
                        notification.getUser().getEmail(),
                        currentPrice,
                        threshold,
                        priceChanged);
                        
                    emailService.sendNotification(notification.getUser(), notification.getProduct(), currentPrice);
                    notification.setNotified(true);
                    notification.setLastNotified(now);
                    notification.updateNextNotificationTime();
                    notificationRepository.save(notification);
                    log.info("✅ Уведомление успешно отправлено и сохранено");
                } else if (currentPrice.compareTo(threshold) > 0 && notification.isNotified()) {
                    notification.setNotified(false);
                    notification.setNextNotificationTime(null);
                    notificationRepository.save(notification);
                    log.info("Сброс статуса уведомления для товара {} (цена поднялась выше порога)", 
                        notification.getProduct().getName());
                } else {
                    log.info("Уведомление не требуется для товара {} (цена: {}, порог: {}, статус: {}, цена изменилась: {}, время пришло: {})", 
                        notification.getProduct().getName(),
                        currentPrice,
                        threshold,
                        notification.isNotified(),
                        priceChanged,
                        timeToNotify);
                }
            } catch (Exception e) {
                log.error("Ошибка при обработке уведомления для товара {}: {}", 
                    notification.getProduct().getName(), 
                    e.getMessage(), 
                    e);
            }
        }
    }
}