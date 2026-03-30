package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.model.PriceSnapshot;
import com.blockfoliox.crypto.repository.PriceSnapshotRepository;
import com.blockfoliox.crypto.service.PriceSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/snapshots")
@CrossOrigin(origins = "http://localhost:3000")
public class PriceSnapshotController {

    private static final Logger log = LoggerFactory.getLogger(PriceSnapshotController.class);

    private final PriceSnapshotRepository priceSnapshotRepository;
    private final PriceSnapshotService priceSnapshotService;

    public PriceSnapshotController(PriceSnapshotRepository priceSnapshotRepository,
                                   PriceSnapshotService priceSnapshotService) {
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.priceSnapshotService = priceSnapshotService;
    }

    @GetMapping("/{symbol}")
    public List<PriceSnapshot> getSnapshots(@PathVariable String symbol) {
        log.info("Fetching snapshots for symbol={}", symbol);
        return priceSnapshotRepository.findByAssetSymbolOrderByCapturedAtAsc(symbol.toUpperCase());
    }

    @PostMapping("/capture")
    public String captureNow() {
        log.info("Manual snapshot capture triggered");
        priceSnapshotService.captureSnapshots();
        return "Snapshot captured!";
    }

    @GetMapping
    public List<PriceSnapshot> getAllSnapshots() {
        log.info("Fetching all snapshots");
        return priceSnapshotRepository.findAll();
    }
}