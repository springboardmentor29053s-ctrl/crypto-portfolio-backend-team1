package com.crypto.portfolio.controller;

import com.crypto.portfolio.service.CoinSyncService;
import com.crypto.portfolio.service.ScamTokenSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
public class CoinController {
// testing only
    private final CoinSyncService coinSyncService;
    private final ScamTokenSyncService scamTokenSyncService;

    @PostMapping("/sync")
    public String syncCoins(){

        coinSyncService.syncCoins();

        return "Coins synced successfully";
    }

    @PostMapping("/scam/sync")
    public String syncScamTokens(){


        scamTokenSyncService.syncScamTokens();

        return "Scam tokens updated";
    }
}