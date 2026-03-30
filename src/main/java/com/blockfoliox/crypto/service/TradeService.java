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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final ExchangeRepository exchangeRepository;

    @Value("${binance.testnet.baseUrl}")
    private String baseUrl;

    public TradeService(RestTemplate restTemplate,
                        EncryptionUtil encryptionUtil,
                        ApiKeyRepository apiKeyRepository,
                        UserRepository userRepository,
                        TradeRepository tradeRepository,
                        HoldingRepository holdingRepository,
                        ExchangeRepository exchangeRepository) {
        this.restTemplate = restTemplate;
        this.encryptionUtil = encryptionUtil;
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
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
            log.warn(" Could not get Binance time offset: {}", e.getMessage());
            return 0;
        }
    }

    public String syncTrades(Long userId, String symbol) {
        log.info("Syncing trades for userId={} symbol={}", userId, symbol);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ApiKey> keys = apiKeyRepository.findByUserAndExchange_Name(user, "Binance");
        if (keys.isEmpty()) return "No Binance API keys found";

        String decryptedKey = encryptionUtil.decrypt(keys.get(0).getApiKey());
        String decryptedSecret = encryptionUtil.decrypt(keys.get(0).getApiSecret());

        long timestamp = System.currentTimeMillis() + getBinanceTimeOffset();
        String queryString = "symbol=" + symbol + "&recvWindow=10000&timestamp=" + timestamp;
        String signature = generateSignature(queryString, decryptedSecret);
        String url = baseUrl + "/api/v3/myTrades?" + queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", decryptedKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode trades = mapper.readTree(response.getBody());

            Exchange exchange = exchangeRepository.findByName("Binance");
            int savedCount = 0;

            for (JsonNode t : trades) {
                Trade trade = new Trade();
                trade.setUser(user);
                trade.setAssetSymbol(symbol);
                trade.setSide(t.get("isBuyer").asBoolean() ? Trade.Side.buy : Trade.Side.sell);
                trade.setQuantity(new BigDecimal(t.get("qty").asText()));
                trade.setPrice(new BigDecimal(t.get("price").asText()));
                trade.setFee(new BigDecimal(t.get("commission").asText()));
                trade.setExchange(exchange);

                long epochMs = t.get("time").asLong();
                trade.setExecutedAt(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()
                ));

                tradeRepository.save(trade);
                savedCount++;
            }

            computeAvgCost(user, symbol);

            log.info(" Synced {} trades for userId={} symbol={}", savedCount, userId, symbol);
            return " Synced " + savedCount + " trades for " + symbol;

        } catch (Exception e) {
            log.error(" Trade sync failed for userId={} symbol={}: {}", userId, symbol, e.getMessage());
            return " Trade sync failed: " + e.getMessage();
        }
    }

    private void computeAvgCost(User user, String symbol) {
        String asset = symbol.replace("USDT", "")
                .replace("BUSD", "")
                .replace("BNB", "");

        List<Trade> trades = tradeRepository.findByUserAndAssetSymbol(user, symbol);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (Trade t : trades) {
            if (t.getSide() == Trade.Side.buy) {
                totalCost = totalCost.add(t.getPrice().multiply(t.getQuantity()));
                totalQty = totalQty.add(t.getQuantity());
            }
        }

        if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal avgCost = totalCost.divide(totalQty, 8, RoundingMode.HALF_UP);
            holdingRepository.findByUserAndAssetSymbol(user, asset).ifPresent(holding -> {
                holding.setAvgCost(avgCost);
                holding.setUpdatedAt(LocalDateTime.now());
                holdingRepository.save(holding);
            });
            log.info(" Avg cost computed for asset={} avgCost={}", asset, avgCost);
        }
    }

    public List<Trade> getTrades(Long userId) {
        log.info("Fetching trades for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return tradeRepository.findByUserOrderByExecutedAtDesc(user);
    }
}