package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.ApiKey;
import com.crypto.cryptoPortfolio.entity.Exchange;
import com.crypto.cryptoPortfolio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByUserAndExchange(User user, Exchange exchange);

    boolean existsByUserAndExchange(User user, Exchange exchange);

    List<ApiKey> findByUser(User user);

    List<ApiKey> findByUserId(Long userId);
}
