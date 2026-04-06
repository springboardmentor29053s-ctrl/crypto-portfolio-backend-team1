package com.crypto.portfolio.service;

import com.crypto.portfolio.dto.ConnectExchangeRequest;
import com.crypto.portfolio.dto.CreateExchangeRequest;
import com.crypto.portfolio.dto.UserExchangeResponse;
import com.crypto.portfolio.model.ApiKey;
import com.crypto.portfolio.model.Exchange;
import com.crypto.portfolio.model.User;
import com.crypto.portfolio.repository.ApiKeyRepository;
import com.crypto.portfolio.repository.ExchangeRepository;
import com.crypto.portfolio.repository.HoldingRepository;
import com.crypto.portfolio.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final UserRepository userRepository;
    private final ExchangeRepository exchangeRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final HoldingRepository holdingRepository;

    public Exchange createExchange(CreateExchangeRequest request) {

        // 🔍 check if already exists
        Optional<Exchange> existing =
                exchangeRepository.findByName(request.getName());

        if (existing.isPresent()) {
            return existing.get(); // ✅ return existing instead of error
        }

        // 🆕 create new exchange
        Exchange exchange = new Exchange();
        exchange.setName(request.getName());
        exchange.setBaseUrl(request.getBaseUrl());
        exchange.setCreatedAt(LocalDateTime.now());

        return exchangeRepository.save(exchange);
    }

    @Transactional
    public void connectExchange(String username, Long exchangeId) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        // 🔴 deactivate all active exchanges
        apiKeyRepository.deactivateAllForUser(user.getId());

        // 🟢 find existing API key
        ApiKey apiKey = apiKeyRepository
                .findByUserAndExchange(user, exchange)
                .orElseThrow(() -> new RuntimeException("API key not registered for this exchange"));

        // 🟢 activate this one
        apiKey.setIsActive(true);

        apiKeyRepository.save(apiKey);
    }

    public void registerApiKey(String username, ConnectExchangeRequest request) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔥 find exchange by name (case-insensitive)
        Exchange exchange = exchangeRepository
                .findByNameIgnoreCase(request.getExchangeName())
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        // 🔍 check if already exists
        Optional<ApiKey> existing =
                apiKeyRepository.findByUserAndExchange(user, exchange);

        if (existing.isPresent()) {
            throw new RuntimeException("API key already exists for this exchange");
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setExchange(exchange);
        apiKey.setApiKey(request.getApiKey());
        apiKey.setApiSecret(request.getApiSecret());
        apiKey.setLabel(request.getExchangeName()); // optional
        apiKey.setIsActive(false);

        apiKeyRepository.save(apiKey);
    }

    public void activateExchange(String username, Long exchangeId) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        // deactivate all
        apiKeyRepository.deactivateAllForUser(user.getId());

        ApiKey apiKey = apiKeyRepository
                .findByUserAndExchange(user, exchange)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        apiKey.setIsActive(true);

        apiKeyRepository.save(apiKey);
    }

    public String getActiveExchangeName(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return apiKeyRepository.findByUserAndIsActiveTrue(user)
                .map(apiKey -> apiKey.getExchange().getName())
                .orElse("No active exchange");
    }


    public Exchange getActiveExchange(User user) {

        return apiKeyRepository.findByUserAndIsActiveTrue(user)
                .map(ApiKey::getExchange)
                .orElseThrow(() -> new RuntimeException("No active exchange found"));
    }

    @Transactional
    public void setDefaultExchange(User user) {

        Exchange defaultExchange = exchangeRepository.findByName("Binance TestNet")
                .orElseThrow(() -> new RuntimeException("Default exchange not found"));

        ApiKey apiKey = apiKeyRepository
                .findByUserAndExchange(user, defaultExchange)
                .orElse(null);

        if (apiKey == null) {
            // create empty key (optional)
            apiKey = new ApiKey();
            apiKey.setUser(user);
            apiKey.setExchange(defaultExchange);
            apiKey.setApiKey("");
            apiKey.setApiSecret("");
        }

        apiKey.setIsActive(true);
        apiKeyRepository.save(apiKey);
    }

    public List<UserExchangeResponse> getUserExchanges(String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔥 get ALL exchanges (not api_keys)
        List<Exchange> exchanges = exchangeRepository.findAll();

        // 🔍 get user's API keys
        List<ApiKey> apiKeys = apiKeyRepository.findByUser(user);

        // 🔁 convert to map → exchangeId → isActive
        Map<Long, Boolean> activeMap = new HashMap<>();

        for (ApiKey api : apiKeys) {
            activeMap.put(api.getExchange().getId(), api.getIsActive());
        }

        // 🔁 build response
        return exchanges.stream()
                .map(exchange -> new UserExchangeResponse(
                        exchange.getId(),
                        exchange.getName(),
                        activeMap.getOrDefault(exchange.getId(), false)
                ))
                .toList();
    }

    @Transactional
    public void disconnectExchange(String exchangeName, String username) {

        User user = userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exchange exchange = exchangeRepository.findByNameIgnoreCase(exchangeName)
                .orElseThrow(() -> new RuntimeException("Exchange not found"));

        // ❌ Remove API key
        apiKeyRepository.deleteByUserAndExchange(user, exchange);

        // ❌ Remove holdings
        holdingRepository.deleteByUserAndExchange(user, exchange);

        // 🔥 Optional: remove trades
        // tradeRepository.deleteByUserAndExchange(user, exchange);
    }
}
