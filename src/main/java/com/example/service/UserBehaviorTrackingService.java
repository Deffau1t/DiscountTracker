package com.example.service;

import com.example.entity.Product;
import com.example.entity.User;
import com.example.entity.UserBehavior;
import com.example.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorTrackingService {

    private final UserBehaviorRepository userBehaviorRepository;

    /**
     * Отслеживает просмотр товара пользователем
     */
    @Transactional
    public void trackProductView(User user, Product product) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUser(user);
        behavior.setProduct(product);
        behavior.setBehaviorType(UserBehavior.BehaviorType.VIEW);
        behavior.setCreatedAt(LocalDateTime.now());
        
        userBehaviorRepository.save(behavior);
        log.debug("Отслежено просмотр товара {} пользователем {}", product.getName(), user.getEmail());
    }

    /**
     * Отслеживает добавление товара в список наблюдения
     */
    @Transactional
    public void trackWatchAdd(User user, Product product) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUser(user);
        behavior.setProduct(product);
        behavior.setBehaviorType(UserBehavior.BehaviorType.WATCH_ADD);
        behavior.setCreatedAt(LocalDateTime.now());
        
        userBehaviorRepository.save(behavior);
        log.debug("Отслежено добавление товара {} в список наблюдения пользователем {}", 
                 product.getName(), user.getEmail());
    }

    /**
     * Отслеживает удаление товара из списка наблюдения
     */
    @Transactional
    public void trackWatchRemove(User user, Product product) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUser(user);
        behavior.setProduct(product);
        behavior.setBehaviorType(UserBehavior.BehaviorType.WATCH_REMOVE);
        behavior.setCreatedAt(LocalDateTime.now());
        
        userBehaviorRepository.save(behavior);
        log.debug("Отслежено удаление товара {} из списка наблюдения пользователем {}", 
                 product.getName(), user.getEmail());
    }

    /**
     * Отслеживает клик по уведомлению
     */
    @Transactional
    public void trackNotificationClick(User user, Product product) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUser(user);
        behavior.setProduct(product);
        behavior.setBehaviorType(UserBehavior.BehaviorType.NOTIFICATION_CLICK);
        behavior.setCreatedAt(LocalDateTime.now());
        
        userBehaviorRepository.save(behavior);
        log.debug("Отслежено клик по уведомлению о товаре {} пользователем {}", 
                 product.getName(), user.getEmail());
    }

    /**
     * Получает количество действий определенного типа для пользователя
     */
    public Long getBehaviorCount(User user, UserBehavior.BehaviorType behaviorType) {
        return userBehaviorRepository.countByUserAndType(user.getId(), behaviorType);
    }

    /**
     * Получает количество действий определенного типа для конкретного товара
     */
    public Long getBehaviorCount(User user, Product product, UserBehavior.BehaviorType behaviorType) {
        return userBehaviorRepository.countByUserAndProductAndType(user.getId(), product.getId(), behaviorType);
    }
}
