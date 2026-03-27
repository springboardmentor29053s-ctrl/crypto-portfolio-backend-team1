package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.AddApiKeyRequest;
import com.crypto.cryptoPortfolio.dto.OrderRequest;
import com.crypto.cryptoPortfolio.entity.ApiKey;
import com.crypto.cryptoPortfolio.entity.Exchange;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.ApiKeyRepository;
import com.crypto.cryptoPortfolio.repository.ExchangeRepository;
import com.crypto.cryptoPortfolio.service.ApiKeyService;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.BinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exchanges")
public class ExchangeController {

    private final ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Autowired
    private final BinanceService binanceService;

    @Autowired
    private final ApiKeyRepository apiKeyRepository;

    // ✅ Constructor Injection (VERY IMPORTANT)
    public ExchangeController(ApiKeyService apiKeyService,
                              BinanceService binanceService,
                              ExchangeRepository exchangeRepository,
                              ApiKeyRepository apiKeyRepository) {
        this.apiKeyService = apiKeyService;
        this.binanceService = binanceService;
        this.exchangeRepository = exchangeRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectExchange(
            @RequestBody AddApiKeyRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        apiKeyService.addApiKey(
                request.getExchangeId(),
                request.getApiKey(),
                request.getApiSecret(),
                request.getLabel(),
                user
        );

        return ResponseEntity.ok("Exchange Connected Successfully");
    }

}
