package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.*;
import com.crypto.cryptoPortfolio.repository.*;
import com.crypto.cryptoPortfolio.security.EncryptionUtil;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ExchangeRepository exchangeRepository;
    private final EncryptionUtil encryptionUtil;

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         ExchangeRepository exchangeRepository,
                         EncryptionUtil encryptionUtil) {
        this.apiKeyRepository = apiKeyRepository;
        this.exchangeRepository = exchangeRepository;
        this.encryptionUtil = encryptionUtil;
    }

    public void addApiKey(Integer exchangeId,
                          String apiKey,
                          String apiSecret,
                          String label,
                          User user) {

        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        ApiKey newKey = new ApiKey();
        newKey.setUser(user);
        newKey.setExchange(exchange);
        newKey.setApiKey(encryptionUtil.encrypt(apiKey));
        newKey.setApiSecret(encryptionUtil.encrypt(apiSecret));
        newKey.setLabel(label);

        apiKeyRepository.save(newKey);
    }
}
