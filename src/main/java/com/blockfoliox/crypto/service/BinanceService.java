package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.config.EncryptionUtil;
import com.blockfoliox.crypto.model.ApiKey;
import com.blockfoliox.crypto.repository.ApiKeyRepository;
import com.blockfoliox.crypto.repository.UserRepository;
import com.blockfoliox.crypto.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Service
public class BinanceService {

    private static final Logger log = LoggerFactory.getLogger(BinanceService.class);

    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    @Value("${binance.testnet.baseUrl}")
    private String baseUrl;

    public BinanceService(RestTemplate restTemplate,
                          EncryptionUtil encryptionUtil,
                          ApiKeyRepository apiKeyRepository,
                          UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.encryptionUtil = encryptionUtil;
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    private String generateSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            return Hex.encodeHexString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Binance signature", e);
        }
    }

    private long getBinanceTimeOffset() {
        try {
            String url = baseUrl + "/api/v3/time";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            long binanceTime = root.get("serverTime").asLong();
            return binanceTime - System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("️ Could not get Binance time offset: {}", e.getMessage());
            return 0;
        }
    }

    public String getAccountBalance(Long userId) {
        log.info("Fetching Binance balance for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ApiKey> keys = apiKeyRepository.findByUserAndExchange_Name(user, "Binance");
        if (keys.isEmpty()) {
            log.error("No Binance API keys found for userId={}", userId);
            return "{\"error\": \"No Binance API keys found for this user\"}";
        }

        String decryptedKey = encryptionUtil.decrypt(keys.get(0).getApiKey());
        String decryptedSecret = encryptionUtil.decrypt(keys.get(0).getApiSecret());

        long timestamp = System.currentTimeMillis() + getBinanceTimeOffset();
        String queryString = "recvWindow=10000&timestamp=" + timestamp;
        String signature = generateSignature(queryString, decryptedSecret);
        String url = baseUrl + "/api/v3/account?" + queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", decryptedKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            log.info("Balance fetched for userId={}", userId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Binance API call failed for userId={}: {}", userId, e.getMessage());
            return "{\"error\": \"Binance API call failed: " + e.getMessage() + "\"}";
        }
    }

    public String placeOrder(Long userId, String symbol, String side, String quantity) {
        log.info("Placing {} order for userId={} symbol={} qty={}", side, userId, symbol, quantity);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ApiKey> keys = apiKeyRepository.findByUserAndExchange_Name(user, "Binance");
        if (keys.isEmpty()) return "{\"error\": \"No Binance API keys found\"}";

        String decryptedKey = encryptionUtil.decrypt(keys.get(0).getApiKey());
        String decryptedSecret = encryptionUtil.decrypt(keys.get(0).getApiSecret());

        long timestamp = System.currentTimeMillis() + getBinanceTimeOffset();
        String queryString = "symbol=" + symbol
                + "&side=" + side.toUpperCase()
                + "&type=MARKET"
                + "&quantity=" + quantity
                + "&recvWindow=10000"
                + "&timestamp=" + timestamp;

        String signature = generateSignature(queryString, decryptedSecret);
        String url = baseUrl + "/api/v3/order?" + queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", decryptedKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(headers), String.class
            );
            log.info("✅ Order placed for userId={} symbol={}", userId, symbol);
            return response.getBody();
        } catch (Exception e) {
            log.error(" Order failed for userId={} symbol={}: {}", userId, symbol, e.getMessage());
            return "{\"error\": \"Order failed: " + e.getMessage() + "\"}";
        }
    }
}