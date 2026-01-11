package com.example.repository;


import com.example.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    @Query("""
        SELECT ph FROM PriceHistory ph 
        WHERE ph.checkedAt = (
            SELECT MAX(subPh.checkedAt) 
            FROM PriceHistory subPh 
            WHERE subPh.product.id = ph.product.id
        )
    """)
    List<PriceHistory> findLatestPrices();

    List<PriceHistory> findByProductId(Long productId);
    
    List<PriceHistory> findByProductOrderByCheckedAtDesc(com.example.entity.Product product);
    
    @Query("SELECT ph.price FROM PriceHistory ph WHERE ph.product.id = :productId ORDER BY ph.checkedAt DESC")
    java.util.List<java.math.BigDecimal> findLatestPriceByProductId(Long productId);
}