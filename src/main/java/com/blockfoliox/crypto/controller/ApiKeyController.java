package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.ApiKey;
import com.blockfoliox.crypto.service.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
@CrossOrigin(origins = "http://localhost:3000")
public class ApiKeyController {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyController.class);

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/save")
    public ApiKey saveKey(@RequestBody Map<String, String> body) {
        log.info("Saving API key for userId={}", body.get("userId"));
        return apiKeyService.saveApiKey(
                Long.parseLong(body.get("userId")),
                body.get("exchangeName"),
                body.get("apiKey"),
                body.get("apiSecret")
        );
    }

    @GetMapping("/{userId}/{exchangeName}")
    public List<ApiKey> getKeys(@PathVariable Long userId,
                                @PathVariable String exchangeName) {
        log.info("Fetching keys for userId={} exchange={}", userId, exchangeName);
        return apiKeyService.getDecryptedKeys(userId, exchangeName);
    }
}