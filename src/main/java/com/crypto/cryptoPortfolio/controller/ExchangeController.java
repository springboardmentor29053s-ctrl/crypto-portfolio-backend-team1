package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.AddApiKeyRequest;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.service.ApiKeyService;
import com.crypto.cryptoPortfolio.repository.UserRepository;
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

    // ✅ Constructor Injection (VERY IMPORTANT)
    public ExchangeController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectExchange(
            @RequestBody AddApiKeyRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

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
