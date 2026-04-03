package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.Holding;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUser(User user);
    
    List<Holding> findByUserOrderByAssetSymbol(User user);
    
    Optional<Holding> findByUserAndAssetSymbolAndExchange(User user, String assetSymbol, Exchange exchange);
    
    List<Holding> findByUserAndWalletType(User user, Holding.WalletType walletType);
    
    void deleteByUser(User user);
    
    @Query("SELECT DISTINCT h.assetSymbol FROM Holding h WHERE h.user.id = :userId")
    List<String> findDistinctAssetSymbolsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT h.user FROM Holding h")
    List<User> findDistinctUsers();
}
