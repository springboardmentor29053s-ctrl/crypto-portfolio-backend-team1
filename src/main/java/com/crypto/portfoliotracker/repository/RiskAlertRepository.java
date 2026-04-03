package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.RiskAlert;
import com.crypto.portfoliotracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {

    List<RiskAlert> findByUserOrderByCreatedAtDesc(User user);

    List<RiskAlert> findByUserAndSeenFalseOrderByCreatedAtDesc(User user);

    boolean existsByUserAndAssetSymbolAndAlertType(
            User user, String assetSymbol, RiskAlert.AlertType alertType);

    long countByUserAndSeenFalse(User user);
    
    List<RiskAlert> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, java.time.LocalDateTime after);
    
    List<RiskAlert> findByAssetSymbolContaining(String assetSymbol);
}
