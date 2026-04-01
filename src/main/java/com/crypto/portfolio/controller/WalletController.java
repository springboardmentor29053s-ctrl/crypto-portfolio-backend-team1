package com.crypto.portfolio.controller;


import com.crypto.portfolio.dto.WalletResponse;
import com.crypto.portfolio.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public WalletResponse getWallet() {
        return walletService.getWalletBalance();
    }
}