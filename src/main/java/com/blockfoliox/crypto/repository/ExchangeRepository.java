// ExchangeRepository.java
package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {
    Exchange findByName(String name);
}