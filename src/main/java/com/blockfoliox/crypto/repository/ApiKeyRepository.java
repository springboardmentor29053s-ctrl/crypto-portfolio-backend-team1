// ApiKeyRepository.java
package com.blockfoliox.crypto.repository;

import com.blockfoliox.crypto.model.ApiKey;
import com.blockfoliox.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByUser(User user);
    List<ApiKey> findByUserAndExchange_Name(User user, String exchangeName);
}