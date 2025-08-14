package com.example.repository;

import com.example.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    List<UserPreference> findByUserId(Long userId);
    
    Optional<UserPreference> findByUserIdAndCategory(Long userId, String category);
    
    @Query("SELECT up.category, AVG(up.weight) as avgWeight FROM UserPreference up " +
           "GROUP BY up.category ORDER BY avgWeight DESC")
    List<Object[]> findPopularCategories();
    
    @Query("SELECT up.category FROM UserPreference up WHERE up.user.id = :userId " +
           "AND up.weight >= :minWeight ORDER BY up.weight DESC")
    List<String> findUserCategoriesByMinWeight(@Param("userId") Long userId, @Param("minWeight") Double minWeight);
}
