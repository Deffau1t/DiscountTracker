package com.example.repository;

import com.example.entity.PcComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PcComponentRepository extends JpaRepository<PcComponent, Long> {
    
    List<PcComponent> findByComponentType(PcComponent.ComponentType componentType);
    
    List<PcComponent> findByComponentTypeAndProductCategory(PcComponent.ComponentType componentType, String category);
    
    Optional<PcComponent> findByProductId(Long productId);
    
    @Query("SELECT c FROM PcComponent c WHERE c.product.id IN :productIds")
    List<PcComponent> findByProductIds(@Param("productIds") List<Long> productIds);
    
    // Поиск совместимых компонентов
    @Query("SELECT c FROM PcComponent c WHERE c.componentType = :type " +
           "AND c.socket = :socket")
    List<PcComponent> findCompatibleCPUs(@Param("type") PcComponent.ComponentType type,
                                         @Param("socket") String socket);
    
    @Query("SELECT c FROM PcComponent c WHERE c.componentType = :type " +
           "AND c.socket = :socket")
    List<PcComponent> findCompatibleMotherboards(@Param("type") PcComponent.ComponentType type,
                                                   @Param("socket") String socket);
    
    @Query("SELECT c FROM PcComponent c WHERE c.componentType = :type " +
           "AND c.memoryType = :memoryType")
    List<PcComponent> findCompatibleRAM(@Param("type") PcComponent.ComponentType type,
                                        @Param("memoryType") String memoryType);
    
    @Query("SELECT c FROM PcComponent c WHERE c.componentType = :type " +
           "AND c.formFactor = :formFactor")
    List<PcComponent> findCompatibleCases(@Param("type") PcComponent.ComponentType type,
                                          @Param("formFactor") String formFactor);
    
    @Query("SELECT c FROM PcComponent c WHERE c.componentType = :type " +
           "AND c.wattage >= :minWattage")
    List<PcComponent> findCompatiblePSUs(@Param("type") PcComponent.ComponentType type,
                                         @Param("minWattage") Integer minWattage);
    
    // Поиск компонентов по характеристикам для рекомендаций
    // Примечание: сортировка по цене выполняется в сервисе, так как currentPrice - transient поле
}

