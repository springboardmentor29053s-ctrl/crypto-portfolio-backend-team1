package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Exchange;
import com.crypto.portfolio.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
    Optional<Exchange> findByName(String name);
    Optional<Exchange> findById(Long id);
    Optional<Exchange> findByNameIgnoreCase(String name);
}

