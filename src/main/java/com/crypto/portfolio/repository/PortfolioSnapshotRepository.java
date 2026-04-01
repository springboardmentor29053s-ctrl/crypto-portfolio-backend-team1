package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PortfolioSnapshotRepository
        extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByUserIdOrderByTimestampAsc(Long userId, LocalDateTime timestamp);

    List<PortfolioSnapshot> findByUserIdAndTimestampAfterOrderByTimestampAsc(
            Long userId,
            LocalDateTime from
    );
}