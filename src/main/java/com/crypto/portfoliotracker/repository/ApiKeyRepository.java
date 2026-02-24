package com.crypto.portfoliotracker.repository;

import com.crypto.portfoliotracker.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByUserId(Long userId);
    List<ApiKey> findByUserIdAndExchangeId(Long userId, Long exchangeId);
    Optional<ApiKey> findByUserIdAndExchangeIdAndLabel(Long userId, Long exchangeId, String label);
}
