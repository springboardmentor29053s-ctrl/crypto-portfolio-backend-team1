package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.dto.AddApiKeyRequest;

import com.crypto.cryptoPortfolio.dto.ApiKeyResponse;
import com.crypto.cryptoPortfolio.entity.ApiKey;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.ApiKeyRepository;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ✅ FINAL ExchangeController - Complete Implementation
 *
 * Endpoints:
 * GET    /api/api-keys              → Get all API keys for user
 * POST   /api/api-keys              → Add new API key
 * DELETE /api/api-keys/{id}         → Delete API key
 * POST   /api/exchanges/connect     → Connect exchange (legacy, using apiKeyService)
 */
@RestController
@RequestMapping("/api")
public class ExchangeController {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // ✅ 1️⃣ GET ALL API KEYS FOR CURRENT USER
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Retrieve all API keys for the authenticated user
     *
     * Security: ✅ Shows masked keys only (last 4 characters visible)
     *
     * Endpoint: GET /api/api-keys
     *
     * Response Example:
     * [
     *   {
     *     "id": 1,
     *     "exchange": "Binance",
     *     "apiKeyMasked": "****abc123",
     *     "label": "Trading Account",
     *     "createdAt": "2024-04-06T10:30:00",
     *     "isActive": true
     *   }
     * ]
     */
    @GetMapping("/api-keys")
    public ResponseEntity<?> getAllApiKeys(Authentication authentication) {
        try {
            // ✅ Step 1: Get authenticated user
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("❌ User not found"));

            // ✅ Step 2: Fetch all API keys for this user
            List<ApiKey> apiKeys = apiKeyRepository.findByUserId(user.getId());

            // ✅ Step 3: Map to DTOs (hide sensitive data)
            List<ApiKeyResponse> response = apiKeys.stream()
                    .map(key -> new ApiKeyResponse(
                            key.getId(),
                            key.getExchange() != null ? key.getExchange().getName() : "Unknown",
                            maskApiKey(key.getApiKey()),
                            key.getLabel(),
                            key.getCreatedAt(),
                            true  // isActive
                    ))
                    .collect(Collectors.toList());

            if (response.isEmpty()) {
                return ResponseEntity.ok()
                        .body(Map.of(
                                "message", "ℹ️ No API keys configured",
                                "keys", response,
                                "count", 0
                        ));
            }

            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "✅ Retrieved " + response.size() + " API key(s)",
                            "keys", response,
                            "count", response.size()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "❌ " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ✅ 2️⃣ GET SINGLE API KEY BY ID
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Get a specific API key by ID (verify ownership first)
     *
     * Endpoint: GET /api/api-keys/{id}
     */
    @GetMapping("/api-keys/{id}")
    public ResponseEntity<?> getApiKeyById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ApiKey apiKey = apiKeyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("API Key not found"));

            // ✅ Security: Verify user owns this key
            if (!apiKey.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "❌ Unauthorized: Cannot access other user's API key"));
            }

            ApiKeyResponse response = new ApiKeyResponse(
                    apiKey.getId(),
                    apiKey.getExchange().getName(),
                    maskApiKey(apiKey.getApiKey()),
                    apiKey.getLabel(),
                    apiKey.getCreatedAt(),
                    true
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "❌ " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ✅ 3️⃣ ADD NEW API KEY
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Add a new API key for an exchange
     *
     * Endpoint: POST /api/api-keys
     *
     * Request Body:
     * {
     *   "exchangeId": 1,
     *   "apiKey": "your-api-key",
     *   "apiSecret": "your-api-secret",
     *   "label": "Trading Account"
     * }
     */
    @PostMapping("/api-keys")
    public ResponseEntity<?> addApiKey(
            @RequestBody AddApiKeyRequest request,
            Authentication authentication) {
        try {
            // ✅ Step 1: Validate request
            if (request.getApiKey() == null || request.getApiKey().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ API Key cannot be empty"));
            }

            if (request.getApiSecret() == null || request.getApiSecret().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ API Secret cannot be empty"));
            }

            // ✅ Step 2: Get authenticated user
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Step 3: Add API key through service
            // (Service will encrypt the keys before storing)
            ApiKey newKey = apiKeyService.addApiKey(
                    request.getExchangeId(),
                    request.getApiKey(),
                    request.getApiSecret(),
                    request.getLabel() != null ? request.getLabel() : "Default",
                    user
            );

            // ✅ Step 4: Return masked response
            ApiKeyResponse response = new ApiKeyResponse(
                    newKey.getId(),
                    newKey.getExchange().getName(),
                    maskApiKey(newKey.getApiKey()),
                    newKey.getLabel(),
                    newKey.getCreatedAt(),
                    true
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "✅ API Key added successfully",
                            "key", response
                    ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error adding API key: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ✅ 4️⃣ DELETE API KEY
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Delete an API key
     *
     * Security: ✅ User can only delete their own keys
     *
     * Endpoint: DELETE /api/api-keys/{id}
     */
    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<?> deleteApiKey(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            // ✅ Step 1: Get authenticated user
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Step 2: Find API key
            ApiKey apiKey = apiKeyRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("API Key not found"));

            // ✅ Step 3: Verify ownership
            if (!apiKey.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "❌ Unauthorized: Cannot delete other user's API key"));
            }

            // ✅ Step 4: Delete the key
            apiKeyRepository.delete(apiKey);

            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "✅ API Key deleted successfully",
                            "deletedId", id
                    ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Error deleting API key: " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ✅ 5️⃣ CONNECT EXCHANGE (Legacy endpoint - kept for backward compatibility)
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Connect exchange (OLD endpoint - use POST /api/api-keys instead)
     *
     * Endpoint: POST /api/exchanges/connect
     *
     * Deprecated: Use POST /api/api-keys instead
     */
    @PostMapping("/exchanges/connect")
    public ResponseEntity<?> connectExchange(
            @RequestBody AddApiKeyRequest request,
            Authentication authentication) {
        try {
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

            return ResponseEntity.ok()
                    .body(Map.of("message", "✅ Exchange Connected Successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ " + e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPER: Mask API Key (show only last 4 characters)
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Mask API key for security display
     * Example: "abc123xyz456" → "****3456"
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}