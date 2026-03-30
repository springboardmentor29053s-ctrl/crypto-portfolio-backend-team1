package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.ScamToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ScamTokenRepository extends JpaRepository<ScamToken, Long> {
    Optional<ScamToken> findByContractAddress(String contractAddress);
}