package com.crypto.portfolio.repository;


import com.crypto.portfolio.model.ApiKey;
import com.crypto.portfolio.model.Exchange;
import com.crypto.portfolio.model.User;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByUserAndExchange(User user, Exchange exchange);

    List<ApiKey> findByUser(User user);
    @Modifying
    @Transactional
    @Query("UPDATE ApiKey a SET a.isActive = false WHERE a.user.id = :userId")
    void deactivateAllForUser(Long userId);

    Optional<ApiKey> findByUserAndIsActiveTrue(User user);

    void deleteByUserAndExchange(User user, Exchange exchange);
}
