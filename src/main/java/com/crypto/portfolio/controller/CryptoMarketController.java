package com.crypto.portfolio.controller;


import com.crypto.portfolio.dto.CryptoCoin_dto;
import com.crypto.portfolio.dto.PageResponse_dto;
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
    public PageResponse_dto<CryptoCoin_dto> getDashboard(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getDashboardData(page, size);
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