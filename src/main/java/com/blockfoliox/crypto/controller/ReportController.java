package com.blockfoliox.crypto.controller;

import com.blockfoliox.crypto.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "http://localhost:3000")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // P&L summary per coin
    @GetMapping("/pnl/{userId}")
    public List<Map<String, Object>> getPnL(@PathVariable Long userId) {
        log.info("P&L report request for userId={}", userId);
        return reportService.getPnLSummary(userId);
    }

    // CSV export
    @GetMapping("/export/{userId}")
    public ResponseEntity<String> exportCSV(@PathVariable Long userId) {
        log.info("CSV export request for userId={}", userId);
        String csv = reportService.exportTradesCSV(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=blockfoliox_trades.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}