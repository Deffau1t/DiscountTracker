package com.example.repository;

import com.example.entity.PcBuild;
import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PcBuildRepository extends JpaRepository<PcBuild, Long> {
    
    List<PcBuild> findByUserId(Long userId);
    
    List<PcBuild> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    List<PcBuild> findByIsPublicTrue();
    
    List<PcBuild> findByIsPublicTrueOrderByCreatedAtDesc();
    
    Optional<PcBuild> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT b FROM PcBuild b WHERE b.user.id = :userId " +
           "AND b.buildStatus = :status")
    List<PcBuild> findByUserIdAndStatus(@Param("userId") Long userId,
                                        @Param("status") PcBuild.BuildStatus status);
    
    @Query("SELECT COUNT(b) FROM PcBuild b WHERE b.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    // Поиск публичных сборок для рекомендаций
    @Query("SELECT b FROM PcBuild b WHERE b.isPublic = true " +
           "AND b.buildStatus = 'COMPLETE' " +
           "ORDER BY b.totalPrice ASC")
    List<PcBuild> findPublicBuildsOrderByPrice();
    
    // Поиск похожих сборок
    @Query("SELECT DISTINCT b FROM PcBuild b WHERE b.isPublic = true " +
           "AND b.buildStatus = 'COMPLETE' " +
           "AND (b.cpu.id = :cpuId OR b.gpu.id = :gpuId OR b.motherboard.id = :motherboardId)")
    List<PcBuild> findSimilarBuilds(@Param("cpuId") Long cpuId,
                                     @Param("gpuId") Long gpuId,
                                     @Param("motherboardId") Long motherboardId);
}

