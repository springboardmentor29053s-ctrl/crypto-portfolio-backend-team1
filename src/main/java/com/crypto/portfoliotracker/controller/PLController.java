package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.PLReport;
import com.crypto.portfoliotracker.entity.TaxTransaction;
import com.crypto.portfoliotracker.entity.Trade;
import com.crypto.portfoliotracker.entity.Holding;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.repository.PLReportRepository;
import com.crypto.portfoliotracker.repository.TaxTransactionRepository;
import com.crypto.portfoliotracker.service.PLReportService;
import com.crypto.portfoliotracker.service.TaxService;
import com.crypto.portfoliotracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pl")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PLController {

    @Autowired
    private PLReportService plReportService;

    @Autowired
    private TaxService taxService;

    @Autowired
    private UserService userService;

    /**
     * Simple test endpoint to verify backend is working
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testBackend() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Backend is running");
        response.put("timestamp", new java.util.Date().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user
     */
    private User getCurrentUser(UserDetails userDetails) {
        return userService.getUserByEmail(userDetails.getUsername());
    }

    /**
     * Get all P&L reports for user
     */
    @GetMapping("/reports")
    public ResponseEntity<List<PLReport>> getPLReports(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            List<PLReport> reports = plReportService.getUserReports(user);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get P&L summary (current portfolio)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPLSummary(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            
            // Generate current year to date report
            LocalDateTime startOfYear = LocalDateTime.of(LocalDateTime.now().getYear(), 1, 1, 0, 0);
            LocalDateTime now = LocalDateTime.now();
            
            PLReport currentYearReport = plReportService.generatePLReport(user, "YEARLY", startOfYear, now);
            
            java.util.Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("totalInvested", currentYearReport.getTotalInvested());
            summary.put("totalValue", currentYearReport.getTotalValue());
            summary.put("totalProfitLoss", currentYearReport.getTotalProfitLoss());
            summary.put("totalProfitLossPercentage", currentYearReport.getTotalProfitLossPercentage());
            summary.put("realizedGains", currentYearReport.getRealizedGains());
            summary.put("realizedLosses", currentYearReport.getRealizedLosses());
            summary.put("unrealizedGains", currentYearReport.getUnrealizedGains());
            summary.put("unrealizedLosses", currentYearReport.getUnrealizedLosses());
            summary.put("totalTrades", currentYearReport.getTotalTrades());
            summary.put("winRate", currentYearReport.getWinRate());
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate P&L report for date range
     */
    @PostMapping("/generate")
    public ResponseEntity<PLReport> generatePLReport(
            @RequestBody Map<String, Object> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            
            String reportType = (String) requestBody.get("reportType");
            LocalDateTime startDate = LocalDateTime.parse((String) requestBody.get("startDate"));
            LocalDateTime endDate = LocalDateTime.parse((String) requestBody.get("endDate"));
            
            PLReport report = plReportService.generatePLReport(user, reportType, startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all tax transactions for user - DISABLED
     */
    @GetMapping("/tax/transactions")
    public ResponseEntity<List<TaxTransaction>> getTaxTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }

    /**
     * Get tax transactions for specific year - DISABLED
     */
    @GetMapping("/tax/transactions/{year}")
    public ResponseEntity<List<TaxTransaction>> getTaxTransactionsByYear(@PathVariable int year,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }

    /**
     * Generate tax transactions for a year - DISABLED
     */
    @PostMapping("/tax/generate/{year}")
    public ResponseEntity<List<TaxTransaction>> generateTaxTransactions(@PathVariable int year,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        return ResponseEntity.ok(new java.util.ArrayList<>());
    }

    /**
     * Get tax summary for a year - DISABLED
     */
    @GetMapping("/tax/summary/{year}")
    public ResponseEntity<Map<String, Object>> getTaxSummary(@PathVariable int year,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        Map<String, Object> emptySummary = new HashMap<>();
        emptySummary.put("totalTaxLiability", BigDecimal.ZERO);
        emptySummary.put("totalCapitalGains", BigDecimal.ZERO);
        emptySummary.put("totalDeductions", BigDecimal.ZERO);
        emptySummary.put("taxableIncome", BigDecimal.ZERO);
        emptySummary.put("year", year);
        return ResponseEntity.ok(emptySummary);
    }

    /**
     * Export P&L report to CSV
     */
    @GetMapping("/export/{reportId}")
    public ResponseEntity<byte[]> exportPLReport(@PathVariable Long reportId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            PLReport report = plReportService.getReportById(getCurrentUser(userDetails), reportId);
            
            if (report == null || !report.getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }
            
            // Generate CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Date,Asset,Type,Quantity,Price,Total,Profit/Loss\n");
            
            // Add report data to CSV
            // This is a simplified version - you'd add actual trade data here
            
            byte[] csvBytes = csv.toString().getBytes();
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=pl_report_" + reportId + ".csv")
                    .header("Content-Type", "text/csv")
                    .body(csvBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
