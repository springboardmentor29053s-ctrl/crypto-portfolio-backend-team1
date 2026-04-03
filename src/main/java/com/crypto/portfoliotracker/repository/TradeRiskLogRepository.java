package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.TradeRiskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRiskLogRepository extends JpaRepository<TradeRiskLog, Long> {
    
    List<TradeRiskLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<TradeRiskLog> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime after);
    
    List<TradeRiskLog> findByUserIdAndRiskLevelOrderByCreatedAtDesc(Long userId, String riskLevel);
    
    @Query("SELECT COUNT(t) FROM TradeRiskLog t WHERE t.user.id = :userId AND t.hasRiskAlert = true AND t.createdAt >= :after")
    Long countRiskAlertsByUserSince(@Param("userId") Long userId, @Param("after") LocalDateTime after);
    
    @Query("SELECT t.riskLevel, COUNT(t) FROM TradeRiskLog t WHERE t.user.id = :userId AND t.createdAt >= :after GROUP BY t.riskLevel")
    List<Object[]> getRiskLevelDistribution(@Param("userId") Long userId, @Param("after") LocalDateTime after);
    
    @Query("SELECT DATE(t.createdAt) as date, COUNT(t) as tradeCount FROM TradeRiskLog t WHERE t.user.id = :userId AND t.createdAt >= :after GROUP BY DATE(t.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyTradeCounts(@Param("userId") Long userId, @Param("after") LocalDateTime after);
    
    @Query("SELECT AVG(t.tradeValue) FROM TradeRiskLog t WHERE t.user.id = :userId AND t.createdAt >= :after")
    Double getAverageTradeValue(@Param("userId") Long userId, @Param("after") LocalDateTime after);
}
