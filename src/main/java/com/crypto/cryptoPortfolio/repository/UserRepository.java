package com.crypto.cryptoPortfolio.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crypto.cryptoPortfolio.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}


