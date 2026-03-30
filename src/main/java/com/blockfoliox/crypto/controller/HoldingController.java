package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.Holding;
import com.blockfoliox.crypto.service.HoldingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holdings")
@CrossOrigin(origins = "http://localhost:3000")
public class HoldingController {

    private static final Logger log = LoggerFactory.getLogger(HoldingController.class);

    private final HoldingService holdingService;

    public HoldingController(HoldingService holdingService) {
        this.holdingService = holdingService;
    }

    @PostMapping("/sync/{userId}")
    public String syncHoldings(@PathVariable Long userId) {
        log.info("Sync holdings request for userId={}", userId);
        return holdingService.syncHoldings(userId);
    }

    @GetMapping("/{userId}")
    public List<Holding> getHoldings(@PathVariable Long userId) {
        log.info("Get holdings request for userId={}", userId);
        return holdingService.getHoldings(userId);
    }
}