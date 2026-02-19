package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.dto.PortfolioResponse;
import com.crypto.cryptoPortfolio.entity.ApiKey;
import com.crypto.cryptoPortfolio.security.EncryptionUtil;
import com.crypto.cryptoPortfolio.security.HmacUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class BinanceService {

    private final RestTemplate restTemplate;
    private final EncryptionUtil encryptionUtil;

    private final String BASE_URL = "https://api.binance.com";

    public BinanceService(RestTemplate restTemplate,
                          EncryptionUtil encryptionUtil) {
        this.restTemplate = restTemplate;
        this.encryptionUtil = encryptionUtil;
    }

    public List<PortfolioResponse> fetchPortfolio(ApiKey apiKeyEntity) {

        String apiKey = encryptionUtil.decrypt(apiKeyEntity.getApiKey()).trim();
        String secret = encryptionUtil.decrypt(apiKeyEntity.getApiSecret()).trim();

        long timestamp = System.currentTimeMillis();
        String queryString = "timestamp=" + timestamp;

        String signature = HmacUtil.generateSignature(secret, queryString);

        String url = BASE_URL + "/api/v3/account?" +
                queryString + "&signature=" + signature;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        List<Map<String, String>> balances =
                (List<Map<String, String>>) response.getBody().get("balances");

        List<PortfolioResponse> result = new ArrayList<>();

        for (Map<String, String> balance : balances) {

            String free = balance.get("free");
            String locked = balance.get("locked");

            if (!free.equals("0.00000000") || !locked.equals("0.00000000")) {
                result.add(new PortfolioResponse(
                        balance.get("asset"),
                        free,
                        locked
                ));
            }
        }
        System.out.println("Decrypted API Key: [" + apiKey + "]");
        System.out.println("Length: " + apiKey.length());

        return result;
    }
}
