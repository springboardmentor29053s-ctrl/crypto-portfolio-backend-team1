package com.crypto.portfolio.controller;

import com.crypto.portfolio.dto.*;
import com.crypto.portfolio.service.CsvExportService;
import com.crypto.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CsvExportService csvExportService;

    @GetMapping
    public List<PortfolioResponse> getPortfolio() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return portfolioService.getPortfolio(username);
    }
    @GetMapping("/value")
    public PortfolioValueResponse getPortfolioValue() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return portfolioService.getPortfolioValue(username);
    }

    @GetMapping("/allocation")
    public List<PortfolioAllocationResponse> getAllocation() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return portfolioService.getAllocation(username);
    }

    @GetMapping("/performance")
    public List<PortfolioPerformancePoint> getPerformance(
            @RequestParam(defaultValue = "1") int days
    ) {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return portfolioService.getPortfolioPerformance(username, days);
    }

    @GetMapping("/allocation/quantity")
    public List<PortfolioQuantityAllocationResponse> getQuantityAllocation() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return portfolioService.getQuantityAllocation(username);
    }


// new
    @GetMapping("/summary")
    public PortfolioSummaryResponse getSummary() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return portfolioService.getPortfolioSummary(username);
    }
// info
    @GetMapping("/pnl")
    public ProfitLossResponse getProfitLoss() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return portfolioService.getProfitLoss(username);
    }


    /* Export Service*/
    @GetMapping("/export/portfolio")
    public ResponseEntity<InputStreamResource> exportPortfolio() throws IOException {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();


        ByteArrayInputStream csv =
                csvExportService.exportPortfolio(username);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=portfolio.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(csv));
    }

    @GetMapping("/export/trades")
    public ResponseEntity<InputStreamResource> exportTrades() throws IOException {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        ByteArrayInputStream csv =
                csvExportService.exportTrades(username);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=trades.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(csv));
    }
}
