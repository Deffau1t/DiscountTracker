package com.example.controller;

import com.example.entity.Notification;
import com.example.repository.NotificationRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@AllArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;

    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(@PathVariable Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    @PostMapping
    public Notification createNotification(@RequestBody Notification notification) {
        return notificationRepository.save(notification);
    }

    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable Long id) {
        notificationRepository.deleteById(id);
    }
}