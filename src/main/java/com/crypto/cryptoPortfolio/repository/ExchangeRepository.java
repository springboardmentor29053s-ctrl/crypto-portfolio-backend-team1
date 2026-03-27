package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRepository extends JpaRepository<Exchange, Integer> {
    Optional<Exchange> findByNameIgnoreCase(String name);
}

