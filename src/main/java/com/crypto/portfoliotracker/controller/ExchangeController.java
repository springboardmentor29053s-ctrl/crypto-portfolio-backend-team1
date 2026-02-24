package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.dto.ExchangeConnectionRequest;
import com.crypto.portfoliotracker.entity.ApiKey;
import com.crypto.portfoliotracker.entity.Exchange;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.service.ExchangeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchanges")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://10.14.189.34:3000"})
public class ExchangeController {

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Exchange>> getAllExchanges() {
        List<Exchange> exchanges = exchangeService.getAllExchanges();
        return ResponseEntity.ok(exchanges);
    }

    @PostMapping("/connect")
    public ResponseEntity<ApiKey> connectExchange(
            @Valid @RequestBody ExchangeConnectionRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = getUserIdFromEmail(email);
            ApiKey apiKey = exchangeService.connectExchange(userId, request);
            return ResponseEntity.ok(apiKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api-keys")
    public ResponseEntity<List<ApiKey>> getUserApiKeys(Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = getUserIdFromEmail(email);
            List<ApiKey> apiKeys = exchangeService.getUserApiKeys(userId);
            return ResponseEntity.ok(apiKeys);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    public ResponseEntity<String> deleteApiKey(
            @PathVariable Long apiKeyId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            Long userId = getUserIdFromEmail(email);
            exchangeService.deleteApiKey(apiKeyId, userId);
            return ResponseEntity.ok("API Key deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long getUserIdFromEmail(String email) {
        com.crypto.portfoliotracker.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
