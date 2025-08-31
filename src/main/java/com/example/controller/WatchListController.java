package com.example.controller;

import com.example.entity.Notification;
import com.example.entity.PriceHistory;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.repository.NotificationRepository;
import com.example.repository.PriceHistoryRepository;
import com.example.repository.ProductRepository;
import com.example.service.UserBehaviorTrackingService;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/watch-list")
public class WatchListController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final UserBehaviorTrackingService userBehaviorTrackingService;

    @GetMapping
    public String getWatchList(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<Notification> notifications = notificationRepository.findByUserId(user.getId());

        Map<Long, BigDecimal> latestPrices = priceHistoryRepository.findLatestPrices().stream()
                .collect(Collectors.toMap(ph -> ph.getProduct().getId(), PriceHistory::getPrice));

        notifications.forEach(notification -> {
            BigDecimal price = latestPrices.get(notification.getProduct().getId());
            if (price != null) {
                notification.getProduct().setCurrentPrice(price);
            }
        });

        // Применяем поиск
        if (search != null && !search.isEmpty()) {
            notifications = notifications.stream()
                    .filter(n -> n.getProduct().getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Применяем фильтр по категории
        if (category != null && !category.isEmpty()) {
            notifications = notifications.stream()
                    .filter(n -> n.getProduct().getCategory() != null && 
                            n.getProduct().getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        // Применяем сортировку
        if (sortBy != null && !sortBy.isEmpty()) {
            Comparator<Notification> comparator = switch (sortBy) {
                case "name" -> Comparator.comparing(n -> n.getProduct().getName());
                case "price" -> Comparator.comparing(n -> n.getProduct().getCurrentPrice());
                case "date" -> Comparator.comparing(Notification::getCreatedAt);
                case "status" -> Comparator.comparing(Notification::isNotified);
                default -> (n1, n2) -> 0;
            };

            if ("desc".equalsIgnoreCase(direction)) {
                comparator = comparator.reversed();
            }

            notifications.sort(comparator);
        }

        model.addAttribute("notifications", notifications);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        return "watch-list";
    }

    @PostMapping("/edit")
    public String editThreshold(@RequestParam Long id, @RequestParam BigDecimal threshold) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setThreshold(threshold);
            notificationRepository.save(notification);
        }
        return "redirect:/watch-list";
    }

    @PostMapping("/delete")
    public String deleteWatch(@RequestParam Long id) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            Product product = notification.getProduct();
            User user = notification.getUser();
            
            // Удаляем уведомление
            notificationRepository.deleteById(id);
            
            // Проверяем, есть ли другие уведомления для этого товара
            List<Notification> remainingNotifications = notificationRepository.findByProductId(product.getId());
            if (remainingNotifications.isEmpty() && product.getSource().equalsIgnoreCase("my")) {
                // Если это последнее уведомление и источник "my", удаляем товар
                productRepository.delete(product);
            }

            // Трекинг удаления из списка наблюдения
            if (user != null) {
                userBehaviorTrackingService.trackWatchRemove(user, product);
            }
        }
        return "redirect:/watch-list";
    }

    @PostMapping("/frequency")
    public String updateFrequency(@RequestParam Long id, @RequestParam Notification.NotificationFrequency frequency) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setFrequency(frequency);
            notification.updateNextNotificationTime();
            notificationRepository.save(notification);
        }
        return "redirect:/watch-list";
    }
}