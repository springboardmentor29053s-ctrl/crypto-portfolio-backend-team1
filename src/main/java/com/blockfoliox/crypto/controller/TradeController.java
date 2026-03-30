package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.Trade;
import com.blockfoliox.crypto.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@CrossOrigin(origins = "http://localhost:3000")
public class TradeController {

    private static final Logger log = LoggerFactory.getLogger(TradeController.class);

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/sync/{userId}/{symbol}")
    public String syncTrades(@PathVariable Long userId,
                             @PathVariable String symbol) {
        log.info("Sync trades request for userId={} symbol={}", userId, symbol);
        return tradeService.syncTrades(userId, symbol);
    }

    @GetMapping("/{userId}")
    public List<Trade> getTrades(@PathVariable Long userId) {
        log.info("Get trades request for userId={}", userId);
        return tradeService.getTrades(userId);
    }
}