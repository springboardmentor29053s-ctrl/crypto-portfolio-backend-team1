package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.service.CoinGeckoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final CoinGeckoService coinGeckoService;

    public MarketController(CoinGeckoService coinGeckoService) {
        this.coinGeckoService = coinGeckoService;
    }

    @GetMapping("/overview")
    public Map<String, Map<String, Double>> getMarketOverview() {

        List<String> coins = List.of(
                "bitcoin",
                "ethereum",
                "solana",
                "binancecoin"
        );

        return coinGeckoService.getPrices(coins);
    }
}

