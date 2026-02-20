package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.Crypto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CryptoRepository extends JpaRepository<Crypto, Long> {
}

