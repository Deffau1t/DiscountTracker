package com.example.repository;

import com.example.entity.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {
    
    List<UserBehavior> findByUserId(Long userId);
    
    List<UserBehavior> findByUserIdAndBehaviorType(Long userId, UserBehavior.BehaviorType behaviorType);
    
    @Query("SELECT ub.product.category, COUNT(ub) as count FROM UserBehavior ub " +
           "WHERE ub.user.id = :userId AND ub.behaviorType = :behaviorType " +
           "GROUP BY ub.product.category ORDER BY count DESC")
    List<Object[]> findUserCategoryPreferences(@Param("userId") Long userId, 
                                             @Param("behaviorType") UserBehavior.BehaviorType behaviorType);
    
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.user.id = :userId " +
           "AND ub.createdAt >= :since ORDER BY ub.createdAt DESC")
    List<UserBehavior> findRecentBehaviors(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(ub) FROM UserBehavior ub WHERE ub.user.id = :userId " +
           "AND ub.behaviorType = :behaviorType")
    Long countByUserAndType(@Param("userId") Long userId, 
                            @Param("behaviorType") UserBehavior.BehaviorType behaviorType);
    
    @Query("SELECT COUNT(ub) FROM UserBehavior ub WHERE ub.user.id = :userId " +
           "AND ub.product.id = :productId AND ub.behaviorType = :behaviorType")
    Long countByUserAndProductAndType(@Param("userId") Long userId, 
                                     @Param("productId") Long productId, 
                                     @Param("behaviorType") UserBehavior.BehaviorType behaviorType);
    
    List<UserBehavior> findByProductId(Long productId);
}
