package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.dto.ExchangeConnectionRequest;
import com.crypto.portfoliotracker.entity.ApiKey;
import com.crypto.portfoliotracker.entity.Exchange;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.ApiKeyRepository;
import com.crypto.portfoliotracker.repository.ExchangeRepository;
import com.crypto.portfoliotracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExchangeService {

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    public List<Exchange> getAllExchanges() {
        return exchangeRepository.findAll();
    }

    public Exchange createExchange(String name, String baseUrl) {
        Exchange exchange = new Exchange(name, baseUrl);
        return exchangeRepository.save(exchange);
    }

    public ApiKey connectExchange(Long userId, ExchangeConnectionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Exchange> exchangeOpt = exchangeRepository.findByName(request.getExchangeName());
        Exchange exchange;
        
        if (exchangeOpt.isPresent()) {
            exchange = exchangeOpt.get();
        } else {
            exchange = createExchange(request.getExchangeName(), null);
        }

        String encryptedApiKey = encryptionService.encrypt(request.getApiKey());
        String encryptedApiSecret = encryptionService.encrypt(request.getApiSecret());

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setExchange(exchange);
        apiKey.setApiKey(encryptedApiKey);
        apiKey.setApiSecret(encryptedApiSecret);
        apiKey.setLabel(request.getLabel());

        return apiKeyRepository.save(apiKey);
    }

    public List<ApiKey> getUserApiKeys(Long userId) {
        return apiKeyRepository.findByUserId(userId);
    }

    public void deleteApiKey(Long apiKeyId, Long userId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new RuntimeException("API Key not found"));

        if (!apiKey.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this API Key");
        }

        apiKeyRepository.delete(apiKey);
    }

    public void initializeDefaultExchanges() {
        if (exchangeRepository.count() == 0) {
            createExchange("Binance", "https://api.binance.com");
            createExchange("Coinbase", "https://api.coinbase.com");
            createExchange("Kraken", "https://api.kraken.com");
            createExchange("KuCoin", "https://api.kucoin.com");
        }
    }
}
