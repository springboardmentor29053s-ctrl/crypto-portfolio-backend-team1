package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.isActive = true")
    List<PriceAlert> findAllActiveAlerts();
    
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.user.id = :userId AND pa.isActive = true")
    List<PriceAlert> findActiveAlertsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.user.id = :userId AND pa.assetSymbol = :assetSymbol AND pa.isActive = true")
    List<PriceAlert> findActiveAlertsByUserIdAndAssetSymbol(@Param("userId") Long userId, @Param("assetSymbol") String assetSymbol);
    
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.user.id = :userId AND pa.assetSymbol = :assetSymbol AND pa.notificationSent = false")
    List<PriceAlert> findUnsentAlertsByUserIdAndAssetSymbol(@Param("userId") Long userId, @Param("assetSymbol") String assetSymbol);
    
    void deleteByIdAndUserId(Long id, Long userId);
}
