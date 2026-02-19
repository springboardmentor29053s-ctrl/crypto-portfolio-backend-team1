package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRepository extends JpaRepository<Exchange, Integer> {
}

