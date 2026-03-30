package com.blockfoliox.crypto.service;

import com.blockfoliox.crypto.config.EncryptionUtil;
import com.blockfoliox.crypto.model.*;
import com.blockfoliox.crypto.repository.*;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);

    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final ExchangeRepository exchangeRepository;

    @Value("${binance.testnet.baseUrl}")
    private String baseUrl;

    public HoldingService(RestTemplate restTemplate,
                          EncryptionUtil encryptionUtil,
                          ApiKeyRepository apiKeyRepository,
                          UserRepository userRepository,
                          HoldingRepository holdingRepository,
                          ExchangeRepository exchangeRepository) {
        this.restTemplate = restTemplate;
        this.encryptionUtil = encryptionUtil;
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.exchangeRepository = exchangeRepository;
    }

    private String generateSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            return Hex.encodeHexString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Signature generation failed", e);
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

    public String syncHoldings(Long userId) {
        log.info("Syncing holdings for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ApiKey> keys = apiKeyRepository.findByUserAndExchange_Name(user, "Binance");
        if (keys.isEmpty()) {
            log.error("No Binance API keys found for userId={}", userId);
            return "No Binance API keys found for this user";
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

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode balances = root.get("balances");

            Exchange exchange = exchangeRepository.findByName("Binance");
            int savedCount = 0;

            for (JsonNode balance : balances) {
                String asset = balance.get("asset").asText();
                BigDecimal free = new BigDecimal(balance.get("free").asText());
                BigDecimal locked = new BigDecimal(balance.get("locked").asText());
                BigDecimal total = free.add(locked);

                if (total.compareTo(BigDecimal.ZERO) == 0) continue;

                Optional<Holding> existing = holdingRepository.findByUserAndAssetSymbol(user, asset);
                Holding holding = existing.orElse(new Holding());
                holding.setUser(user);
                holding.setAssetSymbol(asset);
                holding.setQuantity(total);
                holding.setWalletType(Holding.WalletType.exchange);
                holding.setExchange(exchange);
                holding.setUpdatedAt(LocalDateTime.now());

                if (existing.isEmpty()) {
                    holding.setAvgCost(BigDecimal.ZERO);
                }

                holdingRepository.save(holding);
                savedCount++;
            }

            log.info(" Synced {} holdings for userId={}", savedCount, userId);
            return " Synced " + savedCount + " holdings from Binance";

        } catch (Exception e) {
            log.error(" Sync failed for userId={}: {}", userId, e.getMessage());
            return " Sync failed: " + e.getMessage();
        }
    }

    public List<Holding> getHoldings(Long userId) {
        log.info("Fetching holdings for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return holdingRepository.findByUser(user);
    }
}