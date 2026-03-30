package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.service.BinanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/binance")
@CrossOrigin(origins = "http://localhost:3000")
public class BinanceController {

    private static final Logger log = LoggerFactory.getLogger(BinanceController.class);

    private final BinanceService binanceService;

    public BinanceController(BinanceService binanceService) {
        this.binanceService = binanceService;
    }

    @GetMapping("/balance/{userId}")
    public String getBalance(@PathVariable Long userId) {
        log.info("Balance request for userId={}", userId);
        return binanceService.getAccountBalance(userId);
    }

    @PostMapping("/order")
    public String placeOrder(@RequestBody Map<String, String> body) {
        log.info("Order request userId={} symbol={} side={}",
                body.get("userId"), body.get("symbol"), body.get("side"));
        return binanceService.placeOrder(
                Long.parseLong(body.get("userId")),
                body.get("symbol"),
                body.get("side"),
                body.get("quantity")
        );
    }
}