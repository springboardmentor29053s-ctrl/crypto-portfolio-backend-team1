package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoinRepository extends JpaRepository<Coin,Long> {

    Optional<Coin> findBySymbol(String symbol);
    List<Coin> findBySymbolIn(List<String> symbols);

}