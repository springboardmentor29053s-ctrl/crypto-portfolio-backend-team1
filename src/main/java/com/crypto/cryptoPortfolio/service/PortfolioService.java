package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.dto.PortfolioResponse;
import com.crypto.cryptoPortfolio.entity.ApiKey;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PortfolioService {

    private final ApiKeyRepository apiKeyRepository;
    private final BinanceService binanceService;

    public PortfolioService(ApiKeyRepository apiKeyRepository,
                            BinanceService binanceService) {
        this.apiKeyRepository = apiKeyRepository;
        this.binanceService = binanceService;
    }

    public List<PortfolioResponse> getPortfolio(User user) {

        List<ApiKey> keys = apiKeyRepository.findByUser(user);

        List<PortfolioResponse> portfolio = new ArrayList<>();

        for (ApiKey key : keys) {
            if (key.getExchange().getName().equalsIgnoreCase("Binance")) {
                portfolio.addAll(binanceService.fetchPortfolio(key));
            }
        }

        return portfolio;
    }
}
