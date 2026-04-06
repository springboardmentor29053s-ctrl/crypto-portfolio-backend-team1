package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Exchange;
import com.crypto.portfolio.model.Trade;
import com.crypto.portfolio.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    //List<Trade> findByUser(User user);
    List<Trade> findByUserOrderByExecutedAtDesc(User user);
    Optional<Trade> findByExchangeIdAndExecutedAtAndAssetSymbol(
            Long exchangeId,
            LocalDateTime executedAt,
            String assetSymbol
    );

    List<Trade> findByUserOrderByExecutedAtAsc(User user);
    Page<Trade> findByUser(User user, Pageable pageable);

    Optional<Trade> findTopByUserAndExchangeAndAssetSymbolOrderByExecutedAtDesc(
            User user,
            Exchange exchange,
            String assetSymbol
    );
    void deleteByUserAndExchange(User user, Exchange exchange);
}
