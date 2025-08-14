package com.example.controller;

import com.example.entity.Notification;
import com.example.entity.User;
import com.example.repository.NotificationRepository;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Log4j2
public class HomeController {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/")
    public String rootPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        return homePage(userDetails, model);
    }

    @GetMapping("/home")
    public String homePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null || userDetails.getUsername() == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<Notification> notifications = notificationRepository.findByUserId(user.getId());
        model.addAttribute("notifications", notifications);
        model.addAttribute("username", user.getEmail());

        model.addAttribute("watchListUrl", "/watch-list");
        model.addAttribute("addWatchUrl", "/watch");
        model.addAttribute("logoutUrl", "/logout");
        model.addAttribute("recommendationsUrl", "/recommendations");

        return "home";
    }
}
