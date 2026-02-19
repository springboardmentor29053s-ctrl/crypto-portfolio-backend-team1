package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Register_model;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Register_model, Long> {
    Optional<Register_model> findByUsername(String username);
    Optional<Register_model> findByEmail(String email);

}
