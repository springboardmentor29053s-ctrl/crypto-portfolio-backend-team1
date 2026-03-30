package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.Trade;
import com.blockfoliox.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserOrderByExecutedAtDesc(User user);
    List<Trade> findByUserAndAssetSymbol(User user, String assetSymbol);
}