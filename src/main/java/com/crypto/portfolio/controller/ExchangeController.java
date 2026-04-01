package com.crypto.portfolio.controller;

import com.crypto.portfolio.dto.ActivateExchangeRequest;
import com.crypto.portfolio.dto.ConnectExchangeRequest;
import com.crypto.portfolio.dto.CreateExchangeRequest;
import com.crypto.portfolio.dto.UserExchangeResponse;
import com.crypto.portfolio.exchange.BinanceService;
import com.crypto.portfolio.model.Exchange;
import com.crypto.portfolio.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final BinanceService binanceService;


    @PostMapping("/new")
    public Exchange createExchange(@RequestBody CreateExchangeRequest request) {
        return exchangeService.createExchange(request);
    }

    @PostMapping("/connect")
    public String connectExchange(@RequestBody ActivateExchangeRequest request) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        exchangeService.connectExchange(username, request.getExchangeId());

        return "Exchange activated successfully";
    }



    @PostMapping("/binance/sync")
    public String syncBinanceBalances() {

            String username = (String) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            binanceService.syncAll(username);

            return "Binance synced successfully";
    }

    // depricated
    @PostMapping("/binance/sync-trades")
    public String syncTrades() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        binanceService.syncTrades(username);

        return "Binance trades synced successfully";
    }

    @PostMapping("/connect/api")
    public String registerApiKey(@RequestBody ConnectExchangeRequest request) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        exchangeService.registerApiKey(username, request);

        return "API key registered successfully";
    }

    @GetMapping("/active")
    public String getActiveExchange() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return exchangeService.getActiveExchangeName(username);
    }

    @GetMapping("/my-exchanges")
    public List<UserExchangeResponse> getUserExchanges() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return exchangeService.getUserExchanges(username);
    }

    @DeleteMapping("/{exchangeName}")
    public ResponseEntity<String> disconnectExchange(@PathVariable String exchangeName) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        exchangeService.disconnectExchange(exchangeName, username);

        return ResponseEntity.ok("Exchange disconnected successfully");
    }
}