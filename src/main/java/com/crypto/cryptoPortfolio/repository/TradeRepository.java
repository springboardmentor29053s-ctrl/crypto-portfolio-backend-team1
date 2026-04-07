package com.crypto.cryptoPortfolio.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.crypto.cryptoPortfolio.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

import com.crypto.cryptoPortfolio.entity.Trade;
import com.crypto.cryptoPortfolio.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUser(User user);
    boolean existsByExternalTradeId(Long externalTradeId);
    List<Trade> findByUserAndAssetSymbol(User user, String assetSymbol);
    List<Trade> findByUserAndAssetSymbolOrderByExecutedAtDesc(User user, String assetSymbol);
    List<Trade> findByUserAndExchange(User user, Exchange exchange);
    List<Trade> findByUserAndExchangeAndAssetSymbolOrderByExecutedAtDesc(
            User user,
            Exchange exchange,
            String assetSymbol
    );
    @Query("SELECT SUM(t.realizedProfit) FROM Trade t WHERE t.user.id = :userId")
    Optional<BigDecimal> sumRealizedProfitByUserId(@Param("userId") Long userId);
    // ── ADD THIS NEW METHOD for PnlService (needs ascending order for FIFO) ──
    List<Trade> findByUserIdOrderByExecutedAtAsc(Long userId);
    List<Trade> findByUserIdOrderByExecutedAtDesc(Long userId);


}