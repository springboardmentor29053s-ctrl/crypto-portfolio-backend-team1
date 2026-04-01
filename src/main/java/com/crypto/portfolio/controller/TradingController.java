package com.crypto.portfolio.controller;


import com.crypto.portfolio.dto.BuyRequest;
import com.crypto.portfolio.dto.SellRequest;
import com.crypto.portfolio.dto.TradeResponse;
import com.crypto.portfolio.model.Trade;
import com.crypto.portfolio.service.TradeAnalysisService;
import com.crypto.portfolio.service.TradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
public class TradingController {

    private final TradingService tradingService;
    private final TradeAnalysisService tradeAnalysisService;

    @PostMapping("/buy")
    public TradeResponse buy(@RequestBody BuyRequest request) {
        return tradingService.buyCrypto(request);
    }

    @PostMapping("/sell")
    public TradeResponse sell(@RequestBody SellRequest request) {
        return tradingService.sellCrypto(request);
    }

    @GetMapping("/history")
    public Page<TradeResponse> getTradeHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return tradingService.getTradeHistory(page, size);
    }

}