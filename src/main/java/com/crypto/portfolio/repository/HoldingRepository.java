package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Exchange;
import com.crypto.portfolio.model.Holding;
import com.crypto.portfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByUserAndExchangeAndAssetSymbol(User user, Exchange exchange, String assetSymbol);

    List<Holding> findByUserAndExchange(User user, Exchange exchange);
    List<Holding> findByUser(User user);


    void deleteByUserAndExchange(User user, Exchange exchange);
}