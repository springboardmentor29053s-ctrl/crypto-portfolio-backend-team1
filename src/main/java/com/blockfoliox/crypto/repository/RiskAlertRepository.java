package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.RiskAlert;
import com.blockfoliox.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    List<RiskAlert> findByUserOrderByCreatedAtDesc(User user);
    List<RiskAlert> findByAssetSymbol(String assetSymbol);
}