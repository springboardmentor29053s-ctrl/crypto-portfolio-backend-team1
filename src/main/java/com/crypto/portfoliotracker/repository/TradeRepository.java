package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserOrderByExecutedAtAsc(User user);
    
    List<Trade> findByUserOrderByExecutedAtDesc(User user);
    
    List<Trade> findByUserAndAssetSymbolOrderByExecutedAtDesc(User user, String assetSymbol);
    
    List<Trade> findTop10ByUserOrderByExecutedAtDesc(User user);
    
    List<Trade> findByUserAndExecutedAtAfterOrderByExecutedAtDesc(User user, LocalDateTime after);
    
    List<Trade> findByUserAndExecutedAtBetweenOrderByExecutedAtAsc(User user, LocalDateTime start, LocalDateTime end);
    
    Optional<Trade> findByUserAndAssetSymbolAndSideAndQuantityAndPriceAndExecutedAt(
        User user, String assetSymbol, Trade.TradeSide side, BigDecimal quantity, BigDecimal price, LocalDateTime executedAt);
    
    @Query("SELECT SUM(t.quantity * t.price) FROM Trade t WHERE t.user.id = :userId AND t.side = 'BUY'")
    BigDecimal getTotalBuyValue(@Param("userId") Long userId);
    
    @Query("SELECT SUM(t.quantity * t.price) FROM Trade t WHERE t.user.id = :userId AND t.side = 'SELL'")
    BigDecimal getTotalSellValue(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT t.assetSymbol FROM Trade t WHERE t.user.id = :userId")
    List<String> findDistinctAssetSymbolsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT t.user FROM Trade t")
    List<User> findDistinctUsers();
    
    void deleteByUser(User user);
}
