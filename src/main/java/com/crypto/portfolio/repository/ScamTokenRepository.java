package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.ScamToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScamTokenRepository
        extends JpaRepository<ScamToken, Long> {

    Optional<ScamToken> findByContractAddress(String contractAddress);

    List<ScamToken> findAll();
}