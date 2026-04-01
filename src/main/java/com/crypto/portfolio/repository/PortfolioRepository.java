package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
//depricated
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Portfolio findByUsernameAndCoinId(String username, String coinId);

    List<Portfolio> findByUsername(String username);
}