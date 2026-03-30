package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.config.EncryptionUtil;
import com.blockfoliox.crypto.model.ApiKey;
import com.blockfoliox.crypto.model.Exchange;
import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.ApiKeyRepository;
import com.blockfoliox.crypto.repository.ExchangeRepository;
import com.blockfoliox.crypto.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final ApiKeyRepository apiKeyRepository;
    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         ExchangeRepository exchangeRepository,
                         UserRepository userRepository,
                         EncryptionUtil encryptionUtil) {
        this.apiKeyRepository = apiKeyRepository;
        this.exchangeRepository = exchangeRepository;
        this.userRepository = userRepository;
        this.encryptionUtil = encryptionUtil;
    }

    public ApiKey saveApiKey(Long userId, String exchangeName, String rawApiKey, String rawApiSecret) {
        log.info("Saving API key for userId={} exchange={}", userId, exchangeName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository.findByName(exchangeName);
        if (exchange == null) {
            log.error("Exchange not found: {}", exchangeName);
            throw new RuntimeException("Exchange not found: " + exchangeName);
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setExchange(exchange);
        apiKey.setApiKey(encryptionUtil.encrypt(rawApiKey));
        apiKey.setApiSecret(encryptionUtil.encrypt(rawApiSecret));
        apiKey.setActive(true);

        log.info(" API key saved for userId={}", userId);
        return apiKeyRepository.save(apiKey);
    }

    public List<ApiKey> getDecryptedKeys(Long userId, String exchangeName) {
        log.info("Fetching decrypted keys for userId={} exchange={}", userId, exchangeName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ApiKey> keys = apiKeyRepository.findByUserAndExchange_Name(user, exchangeName);

        keys.forEach(k -> {
            k.setApiKey(encryptionUtil.decrypt(k.getApiKey()));
            k.setApiSecret(encryptionUtil.decrypt(k.getApiSecret()));
        });

        return keys;
    }
}