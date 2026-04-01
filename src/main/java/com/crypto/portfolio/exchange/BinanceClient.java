package com.crypto.portfolio.exchange;


import com.crypto.portfolio.model.*;
import com.crypto.portfolio.repository.*;
import com.crypto.portfolio.service.HoldingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
public class BinanceClient {

    private static final String BASE_URL = "https://api.binance.com/api/v3";

    
    public String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec keySpec =
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException("Error signing Binance request", e);
        }
    }

   
    public String buildAccountUrl(String apiSecret) {

        long timestamp = System.currentTimeMillis();

        String query = "timestamp=" + timestamp;

        String signature = sign(query, apiSecret);

        return BASE_URL + "/account?" + query + "&signature=" + signature;
    }

    
    public String buildTradeUrl(String apiSecret, String symbol, Long startTime) {

        long timestamp = System.currentTimeMillis();

        String query = "symbol=" + symbol + "&timestamp=" + timestamp;

        
        if (startTime != null) {
            query += "&startTime=" + startTime;
        }

        String signature = sign(query, apiSecret);

        return BASE_URL + "/myTrades?" + query + "&signature=" + signature;
    }
}