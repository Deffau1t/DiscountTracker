package com.example.controller;

import com.example.dto.ProductForm;
import com.example.entity.Notification;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.repository.NotificationRepository;
import com.example.repository.ProductRepository;
import com.example.repository.UserRepository;
import com.example.service.UserBehaviorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Log4j2
public class WatchController {

    private final ProductRepository productRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserBehaviorTrackingService userBehaviorTrackingService;

    @GetMapping("/watch")
    public String watchForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        return "watch";
    }

    @PostMapping("/watch")
    public String createWatch(@ModelAttribute ProductForm productForm,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        try {
            if (userDetails == null || userDetails.getUsername() == null) {
                model.addAttribute("error", "Пользователь не аутентифицирован");
                return "watch";
            }

            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                model.addAttribute("error", "Пользователь не найден");
                return "watch";
            }

            User user = userOpt.get();
            Optional<Product> existingProduct = productRepository.findByUrl(productForm.getUrl());
            Product product = existingProduct.orElseGet(() -> {
                Product p = new Product();
                p.setUrl(productForm.getUrl());
                p.setName(productForm.getName());
                p.setSource(productForm.getSource());
                p.setCategory(productForm.getCategory());
                return productRepository.save(p);
            });

            if (productForm.getThreshold() == null) {
                model.addAttribute("error", "Пороговое значение не может быть пустым");
                return "watch";
            }

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setProduct(product);
            notification.setThreshold(productForm.getThreshold());
            notificationRepository.save(notification);

            // Отслеживаем добавление в список наблюдения
            userBehaviorTrackingService.trackWatchAdd(user, product);

            return "redirect:/home";

        } catch (Exception e) {
            log.error("Ошибка при создании уведомления", e);
            model.addAttribute("error", "Произошла внутренняя ошибка сервера");
            return "watch";
        }
    }
}
