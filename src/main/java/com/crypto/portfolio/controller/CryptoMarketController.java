package com.crypto.portfolio.controller;


import com.crypto.portfolio.dto.*;
import com.crypto.portfolio.service.CryptoMarketService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/dashboard")
public class CryptoMarketController {

    private final CryptoMarketService service;

    public CryptoMarketController(CryptoMarketService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse_dto<CryptoCoin_dto> getDashboard(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getDashboardData(page, size);
    }

    @GetMapping("/coin/{id}")
    public CoinDetailWithChartResponse getCoin(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") String days)
    {

        CoinDetail detail = service.getCoinDetail(id);
        CoinChartResponse chart = service.getCoinChart(id, days);

        CoinDetailWithChartResponse response = new CoinDetailWithChartResponse();
        response.setCoinDetail(detail);
        response.setChart(chart);

        return response;
    }
}
/*

// changed
import com.crypto.portfolio.service.CryptoMarketService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class CryptoMarketController {

    private final CryptoMarketService service;

    public CryptoMarketController(CryptoMarketService service) {
        this.service = service;
    }

    @GetMapping
    public String getDashboard(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return service.getDashboardData(page, size);
    }
}
*/