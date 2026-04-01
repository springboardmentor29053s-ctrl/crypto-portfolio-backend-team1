package com.crypto.portfolio.repository;

import com.crypto.portfolio.model.Alert;
import com.crypto.portfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserAndTriggeredFalse(User user);

    List<Alert> findByTriggeredFalse();
}