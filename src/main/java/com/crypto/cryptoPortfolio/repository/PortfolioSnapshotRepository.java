package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
public interface PortfolioSnapshotRepository
        extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByUserIdAndCapturedAtAfterOrderByCapturedAtAsc(
            Long userId,
            Instant capturedAt
    );

    boolean existsByUserIdAndCapturedAtAfter(Long userId, Instant time);
}
