package com.crypto.portfoliotracker.controller;

import com.crypto.portfoliotracker.entity.PLReport;
import com.crypto.portfoliotracker.entity.TaxTransaction;
import com.crypto.portfoliotracker.entity.User;
import com.crypto.portfoliotracker.service.PLReportService;
import com.crypto.portfoliotracker.service.TaxService;
import com.crypto.portfoliotracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
     * Get current user
     */
    private User getCurrentUser(UserDetails userDetails) {
        return userService.getUserByEmail(userDetails.getUsername());
    }

    /**
     * Get all P&L reports for user
     */
    @GetMapping("/reports")
    public ResponseEntity<List<PLReport>> getReports(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            List<PLReport> reports = plReportService.getUserReports(user);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            System.err.println("Error fetching P&L reports: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get specific P&L report
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<PLReport> getReport(@PathVariable Long reportId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            PLReport report = plReportService.getReportById(user, reportId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.err.println("Error fetching P&L report: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate custom P&L report
     */
    @PostMapping("/reports/generate")
    public ResponseEntity<PLReport> generateCustomReport(@RequestBody Map<String, Object> requestData,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            
            String reportType = (String) requestData.get("reportType");
            LocalDateTime startDate = LocalDateTime.parse((String) requestData.get("startDate"));
            LocalDateTime endDate = LocalDateTime.parse((String) requestData.get("endDate"));
            
            PLReport report = plReportService.generatePLReport(user, reportType, startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.err.println("Error generating custom P&L report: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate monthly P&L report
     */
    @PostMapping("/reports/monthly")
    public ResponseEntity<PLReport> generateMonthlyReport(@RequestBody Map<String, Object> requestData,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            int year = (Integer) requestData.get("year");
            int month = (Integer) requestData.get("month");
            
            PLReport report = plReportService.generateMonthlyReport(user, year, month);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.err.println("Error generating monthly P&L report: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate quarterly P&L report
     */
    @PostMapping("/reports/quarterly")
    public ResponseEntity<PLReport> generateQuarterlyReport(@RequestBody Map<String, Object> requestData,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            int year = (Integer) requestData.get("year");
            int quarter = (Integer) requestData.get("quarter");
            
            PLReport report = plReportService.generateQuarterlyReport(user, year, quarter);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.err.println("Error generating quarterly P&L report: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate yearly P&L report
     */
    @PostMapping("/reports/yearly")
    public ResponseEntity<PLReport> generateYearlyReport(@RequestBody Map<String, Object> requestData,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            int year = (Integer) requestData.get("year");
            
            PLReport report = plReportService.generateYearlyReport(user, year);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            System.err.println("Error generating yearly P&L report: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete P&L report
     */
    @DeleteMapping("/reports/{reportId}")
    public ResponseEntity<?> deleteReport(@PathVariable Long reportId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            plReportService.deleteReport(user, reportId);
            return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
        } catch (Exception e) {
            System.err.println("Error deleting P&L report: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            
            Map<String, Object> summary = new HashMap<>();
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
            summary.put("reportGeneratedAt", currentYearReport.getGeneratedAt());
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            System.err.println("Error fetching P&L summary: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all tax transactions for user - DISABLED
     */
    @GetMapping("/tax/transactions")
    public ResponseEntity<List<TaxTransaction>> getTaxTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        return ResponseEntity.ok(new ArrayList<>());
    }

    /**
     * Get tax transactions for specific year - DISABLED
     */
    @GetMapping("/tax/transactions/{year}")
    public ResponseEntity<List<TaxTransaction>> getTaxTransactionsByYear(@PathVariable int year,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        return ResponseEntity.ok(new ArrayList<>());
    }

    /**
     * Generate tax transactions for a year - DISABLED
     */
    @PostMapping("/tax/generate/{year}")
    public ResponseEntity<List<TaxTransaction>> generateTaxTransactions(@PathVariable int year,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        return ResponseEntity.ok(new ArrayList<>());
    }

    /**
     * Get tax summary for a year - DISABLED
     */
    @GetMapping("/tax/summary/{year}")
    public ResponseEntity<Map<String, Object>> getTaxSummary(@PathVariable int year,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled
        Map<String, Object> emptySummary = new HashMap<>();
        emptySummary.put("taxYear", year);
        emptySummary.put("totalGains", BigDecimal.ZERO);
        emptySummary.put("totalLosses", BigDecimal.ZERO);
        emptySummary.put("totalEstimatedTax", BigDecimal.ZERO);
        emptySummary.put("transactionCount", 0);
        emptySummary.put("shortTermGains", BigDecimal.ZERO);
        emptySummary.put("longTermGains", BigDecimal.ZERO);
        emptySummary.put("ordinaryIncome", BigDecimal.ZERO);
        emptySummary.put("netGains", BigDecimal.ZERO);
        return ResponseEntity.ok(emptySummary);
    }

    /**
     * Export tax transactions to CSV - DISABLED
     */
    @GetMapping("/tax/export/{year}")
    public ResponseEntity<String> exportTaxTransactions(@PathVariable int year,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        // Tax report functionality disabled - return empty CSV
        String emptyCsv = "Date,Type,Asset,Quantity,Price USD,Total USD,Fee USD,Cost Basis,Proceeds,Gain/Loss,Holding Period,Gain/Loss Type,Tax Event Type,Taxable Amount,Tax Rate,Estimated Tax,Notes\n";
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=tax_transactions_" + year + ".csv")
            .body(emptyCsv);
    }

    /**
     * Get available report years
     */
    @GetMapping("/reports/years")
    public ResponseEntity<List<Integer>> getAvailableReportYears(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            List<PLReport> reports = plReportService.getUserReports(user);
            
            // Extract unique years from reports
            List<Integer> years = reports.stream()
                .map(report -> report.getStartDate().getYear())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(years);
        } catch (Exception e) {
            System.err.println("Error fetching available report years: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
