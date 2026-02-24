package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.ApiKey;
import com.blockfoliox.crypto.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
@CrossOrigin(origins = "http://localhost:3000")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    // ✅ Save a new API key
    @PostMapping("/save")
    public ApiKey saveKey(@RequestBody Map<String, String> body) {
        return apiKeyService.saveApiKey(
                Long.parseLong(body.get("userId")),
                body.get("exchangeName"),
                body.get("apiKey"),
                body.get("apiSecret")
        );
    }

    // ✅ Get decrypted keys for a user
    @GetMapping("/{userId}/{exchangeName}")
    public List<ApiKey> getKeys(
            @PathVariable Long userId,
            @PathVariable String exchangeName
    ) {
        return apiKeyService.getDecryptedKeys(userId, exchangeName);
    }
}