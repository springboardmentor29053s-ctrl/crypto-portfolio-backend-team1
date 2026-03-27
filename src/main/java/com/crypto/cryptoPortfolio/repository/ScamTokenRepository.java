package com.crypto.cryptoPortfolio.repository;

import com.crypto.cryptoPortfolio.entity.ScamToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScamTokenRepository extends JpaRepository<ScamToken, Long> {

    Optional<ScamToken> findByContractAddress(String contractAddress);

    boolean existsByContractAddress(String contractAddress);
}