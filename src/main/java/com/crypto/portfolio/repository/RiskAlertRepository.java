package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.RiskAlert;
import com.crypto.portfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAlertRepository
        extends JpaRepository<RiskAlert, Long> {

    List<RiskAlert> findByUser(User user);
    Optional<RiskAlert> findByUserAndAssetSymbol(User user, String assetSymbol);
}