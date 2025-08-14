package com.example.repository;

import com.example.entity.ProductRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRecommendationRepository extends JpaRepository<ProductRecommendation, Long> {
    
    List<ProductRecommendation> findByUserIdAndIsViewedOrderByScoreDesc(Long userId, Boolean isViewed);
    
    @Query("SELECT pr FROM ProductRecommendation pr WHERE pr.user.id = :userId " +
           "ORDER BY pr.score DESC")
    List<ProductRecommendation> findByUserIdOrderByScoreDesc(@Param("userId") Long userId);
    
    @Query("SELECT pr FROM ProductRecommendation pr WHERE pr.user.id = :userId " +
           "AND pr.algorithm = :algorithm ORDER BY pr.score DESC")
    List<ProductRecommendation> findByUserIdAndAlgorithmOrderByScoreDesc(@Param("userId") Long userId, 
                                                                       @Param("algorithm") ProductRecommendation.AlgorithmType algorithm);
    
    @Query("SELECT COUNT(pr) FROM ProductRecommendation pr WHERE pr.user.id = :userId " +
           "AND pr.isViewed = false")
    Long countUnviewedRecommendations(@Param("userId") Long userId);
    
    Optional<ProductRecommendation> findByUserIdAndProductId(Long userId, Long productId);
    
    @Query("SELECT pr FROM ProductRecommendation pr WHERE pr.user.id = :userId " +
           "AND pr.product.category IN :categories ORDER BY pr.score DESC")
    List<ProductRecommendation> findByUserIdAndCategoriesOrderByScoreDesc(@Param("userId") Long userId, 
                                                                        @Param("categories") List<String> categories);
}
