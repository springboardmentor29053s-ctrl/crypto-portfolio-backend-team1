package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.Watchlist;
import com.crypto.portfoliotracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUser(User user);

    boolean existsByUserAndAssetSymbol(User user, String assetSymbol);
}
