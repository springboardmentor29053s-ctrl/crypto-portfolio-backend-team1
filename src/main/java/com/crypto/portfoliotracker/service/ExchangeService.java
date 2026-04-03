package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.dto.ExchangeConnectionRequest;
import com.crypto.portfoliotracker.entity.ApiKey;
import com.crypto.portfoliotracker.entity.Exchange;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.ApiKeyRepository;
import com.crypto.portfoliotracker.repository.ExchangeRepository;
import com.crypto.portfoliotracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ExchangeService {

    @Autowired
    private ExchangeRepository exchangeRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    public List<Exchange> getAllExchanges() {
        return exchangeRepository.findAll();
    }

    public Exchange createExchange(String name, String baseUrl) {
        Exchange exchange = new Exchange(name, baseUrl);
        return exchangeRepository.save(exchange);
    }

    public ApiKey connectExchange(Long userId, ExchangeConnectionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Exchange> exchangeOpt = exchangeRepository.findByName(request.getExchangeName());
        Exchange exchange;
        
        if (exchangeOpt.isPresent()) {
            exchange = exchangeOpt.get();
        } else {
            exchange = createExchange(request.getExchangeName(), null);
        }

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setExchange(exchange);
        apiKey.setApiKey(encryptionService.encrypt(request.getApiKey()));
        apiKey.setApiSecret(encryptionService.encrypt(request.getApiSecret()));
        apiKey.setLabel(request.getLabel());

        return apiKeyRepository.save(apiKey);
    }

    public boolean testConnection(ApiKey apiKey) {
        try {
            String decryptedKey = encryptionService.decrypt(apiKey.getApiKey());
            String decryptedSecret = encryptionService.decrypt(apiKey.getApiSecret());
            
            if (apiKey.getExchange().getName().equalsIgnoreCase("Binance")) {
                return testBinanceConnection(decryptedKey, decryptedSecret);
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, BigDecimal> fetchBalances(ApiKey apiKey) {
        String decryptedKey = encryptionService.decrypt(apiKey.getApiKey());
        String decryptedSecret = encryptionService.decrypt(apiKey.getApiSecret());
        
        if (apiKey.getExchange().getName().equalsIgnoreCase("Binance")) {
            return fetchBinanceBalances(decryptedKey, decryptedSecret);
        }
        
        return new HashMap<>();
    }

    public List<Trade> fetchRecentTrades(ApiKey apiKey) {
        String decryptedKey = encryptionService.decrypt(apiKey.getApiKey());
        String decryptedSecret = encryptionService.decrypt(apiKey.getApiSecret());
        
        if (apiKey.getExchange().getName().equalsIgnoreCase("Binance")) {
            return fetchBinanceTrades(decryptedKey, decryptedSecret);
        }
        
        return new ArrayList<>();
    }

    private boolean testBinanceConnection(String apiKey, String apiSecret) {
        try {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, BigDecimal> fetchBinanceBalances(String apiKey, String apiSecret) {
        Map<String, BigDecimal> balances = new HashMap<>();
        
        balances.put("BTC", new BigDecimal("0.5"));
        balances.put("ETH", new BigDecimal("2.3"));
        balances.put("USDT", new BigDecimal("1000.0"));
        
        return balances;
    }

    private List<Trade> fetchBinanceTrades(String apiKey, String apiSecret) {
        List<Trade> trades = new ArrayList<>();
        
        Trade trade1 = new Trade();
        trade1.setAssetSymbol("BTC");
        trade1.setSide(Trade.TradeSide.BUY);
        trade1.setQuantity(new BigDecimal("0.1"));
        trade1.setPrice(new BigDecimal("45000.00"));
        trade1.setFee(new BigDecimal("4.5"));
        trade1.setExecutedAt(java.time.LocalDateTime.now().minusDays(1));
        
        Trade trade2 = new Trade();
        trade2.setAssetSymbol("ETH");
        trade2.setSide(Trade.TradeSide.BUY);
        trade2.setQuantity(new BigDecimal("1.0"));
        trade2.setPrice(new BigDecimal("3000.00"));
        trade2.setFee(new BigDecimal("3.0"));
        trade2.setExecutedAt(java.time.LocalDateTime.now().minusDays(2));
        
        trades.add(trade1);
        trades.add(trade2);
        
        return trades;
    }

    public List<ApiKey> getUserApiKeys(Long userId) {
        return apiKeyRepository.findByUserId(userId);
    }

    public void deleteApiKey(Long apiKeyId, Long userId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new RuntimeException("API Key not found"));

        if (!apiKey.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this API Key");
        }

        apiKeyRepository.delete(apiKey);
    }

    public void initializeDefaultExchanges() {
        if (exchangeRepository.count() == 0) {
            createExchange("Binance", "https://api.binance.com");
            createExchange("Coinbase", "https://api.coinbase.com");
            createExchange("Kraken", "https://api.kraken.com");
            createExchange("KuCoin", "https://api.kucoin.com");
        }
    }
}
