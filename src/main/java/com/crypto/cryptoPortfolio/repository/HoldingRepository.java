package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingRepository
        extends JpaRepository<Holding, Long> {

    List<Holding> findByUserId(Long userId);
}

