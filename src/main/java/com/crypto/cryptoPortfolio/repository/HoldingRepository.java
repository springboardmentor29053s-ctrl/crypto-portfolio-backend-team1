package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.dto.HoldingResponse;
import com.crypto.cryptoPortfolio.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository
        extends JpaRepository<Holding, Long> {

    List<Holding> findByUserId(Long userId);
    List<Holding> findByUserIdAndExchangeId(Long userId, Integer exchangeId);
    Optional<Holding> findByUserIdAndExchangeIdAndAssetSymbol(
            Long userId, Integer exchangeId, String assetSymbol
    );

    // 2. Get holdings with calculated values from trades
    @Query("SELECT new com.crypto.cryptoPortfolio.dto.HoldingResponse(" +
            "h.id, h.assetSymbol, h.quantity, h.avgCost) " +
            "FROM Holding h WHERE h.user.id = :userId AND h.exchange.id = :exchangeId")
    List<HoldingResponse> findHoldingsWithDetails(
            @Param("userId") Long userId,
            @Param("exchangeId") Integer exchangeId
    );
}

