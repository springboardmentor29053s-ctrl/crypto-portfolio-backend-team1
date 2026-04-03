package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.dto.ExchangeConnectionRequest;
import com.crypto.portfoliotracker.dto.ExchangeDTO;
import com.crypto.portfoliotracker.dto.ApiKeyDTO;
import com.crypto.portfoliotracker.entity.ApiKey;
import com.crypto.portfoliotracker.entity.Exchange;
import com.crypto.portfoliotracker.repository.UserRepository;
import com.crypto.portfoliotracker.service.ExchangeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exchanges")
@CrossOrigin(origins = "http://localhost:3000")
public class ExchangeController {

    @Autowired
    private ExchangeService exchangeService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ExchangeDTO>> getAllExchanges() {
        List<Exchange> exchanges = exchangeService.getAllExchanges();
        return ResponseEntity.ok(convertToDTOList(exchanges));
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectExchange(@Valid @RequestBody ExchangeConnectionRequest request, Authentication auth) {
        try {
            Long userId = getUserId(auth.getName());
            ApiKey apiKey = exchangeService.connectExchange(userId, request);
            return ResponseEntity.ok(apiKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/api-keys")
    public ResponseEntity<List<ApiKeyDTO>> getUserApiKeys(Authentication auth) {
        try {
            Long userId = getUserId(auth.getName());
            List<ApiKey> apiKeys = exchangeService.getUserApiKeys(userId);
            return ResponseEntity.ok(convertApiKeyToDTOList(apiKeys));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/test-connection/{apiKeyId}")
    public ResponseEntity<?> testConnection(@PathVariable Long apiKeyId, Authentication auth) {
        try {
            Long userId = getUserId(auth.getName());
            ApiKey apiKey = findUserApiKey(userId, apiKeyId);
            boolean isConnected = exchangeService.testConnection(apiKey);
            Map<String, Object> result = Map.of("connected", isConnected, "message", isConnected ? "Connection successful" : "Connection failed");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    public ResponseEntity<String> deleteApiKey(@PathVariable Long apiKeyId, Authentication auth) {
        try {
            Long userId = getUserId(auth.getName());
            exchangeService.deleteApiKey(apiKeyId, userId);
            return ResponseEntity.ok("API Key deleted");
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getExchangeStatus(Authentication auth) {
        try {
            Long userId = getUserId(auth.getName());
            List<ApiKey> apiKeys = exchangeService.getUserApiKeys(userId);
            Map<String, Object> status = createStatusResponse(apiKeys);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private Long getUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private ApiKey findUserApiKey(Long userId, Long apiKeyId) {
        List<ApiKey> keys = exchangeService.getUserApiKeys(userId);
        return keys.stream()
                .filter(key -> key.getId().equals(apiKeyId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("API Key not found"));
    }

    private List<ExchangeDTO> convertToDTOList(List<Exchange> exchanges) {
        return exchanges.stream()
                .map(this::convertToDTO)
                .toList();
    }

    private ExchangeDTO convertToDTO(Exchange exchange) {
        return new ExchangeDTO(exchange.getId(), exchange.getName(), exchange.getBaseUrl(), exchange.getCreatedAt());
    }

    private List<ApiKeyDTO> convertApiKeyToDTOList(List<ApiKey> apiKeys) {
        return apiKeys.stream()
                .map(this::convertApiKeyToDTO)
                .toList();
    }

    private ApiKeyDTO convertApiKeyToDTO(ApiKey apiKey) {
        ExchangeDTO exchangeDTO = convertToDTO(apiKey.getExchange());
        return new ApiKeyDTO(apiKey.getId(), apiKey.getLabel(), exchangeDTO, apiKey.getCreatedAt());
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    private Map<String, Object> createStatusResponse(List<ApiKey> apiKeys) {
        Map<String, Object> status = new HashMap<>();
        status.put("totalConnections", apiKeys.size());
        status.put("exchanges", apiKeys.stream()
                .map(key -> key.getExchange().getName())
                .distinct()
                .count());
        return status;
    }
}
