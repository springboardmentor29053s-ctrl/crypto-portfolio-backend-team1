package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.config.EncryptionUtil;
import com.blockfoliox.crypto.model.ApiKey;
import com.blockfoliox.crypto.model.Exchange;
import com.blockfoliox.crypto.model.User;
import com.blockfoliox.crypto.repository.ApiKeyRepository;
import com.blockfoliox.crypto.repository.ExchangeRepository;
import com.blockfoliox.crypto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiKeyService {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    // ✅ Save API key — encrypts before storing
    public ApiKey saveApiKey(Long userId, String exchangeName, String rawApiKey, String rawApiSecret) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository.findByName(exchangeName);
        if (exchange == null) {
            throw new RuntimeException("Exchange not found: " + exchangeName);
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setExchange(exchange);
        apiKey.setApiKey(encryptionUtil.encrypt(rawApiKey));       // ✅ encrypted
        apiKey.setApiSecret(encryptionUtil.encrypt(rawApiSecret)); // ✅ encrypted
        apiKey.setActive(true);

        return apiKeyRepository.save(apiKey);
    }

    // ✅ Get decrypted keys for a user + exchange
    public List<ApiKey> getDecryptedKeys(Long userId, String exchangeName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ApiKey> keys = apiKeyRepository.findByUserAndExchange_Name(user, exchangeName);

        // Decrypt before returning
        keys.forEach(k -> {
            k.setApiKey(encryptionUtil.decrypt(k.getApiKey()));
            k.setApiSecret(encryptionUtil.decrypt(k.getApiSecret()));
        });

        return keys;
    }
}