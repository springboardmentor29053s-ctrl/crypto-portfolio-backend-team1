package com.crypto.cryptoPortfolio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crypto.cryptoPortfolio.entity.ApiKey;
import com.crypto.cryptoPortfolio.entity.User;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {
    List<ApiKey> findByUser(User user);
}

