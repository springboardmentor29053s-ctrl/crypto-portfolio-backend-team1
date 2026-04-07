package com.crypto.cryptoPortfolio.controller;

import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.service.CsvExportService;
import com.crypto.cryptoPortfolio.service.PnlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/pnl")
public class PnlController {

    private final PnlService       pnlService;
    private final CsvExportService csvExportService;
    private final UserRepository   userRepository;

    public PnlController(PnlService pnlService,
                         CsvExportService csvExportService,
                         UserRepository userRepository) {
        this.pnlService       = pnlService;
        this.csvExportService = csvExportService;
        this.userRepository   = userRepository;
    }

    // ── Existing endpoint — your frontend calls GET /pnl/summary ─────────────
    @GetMapping("/summary")
    public ResponseEntity<?> getPnlSummary() {
        User user = getAuthenticatedUser();
        // ✅ Calls getPnlSummary() — matches your PnlService method name exactly
        return ResponseEntity.ok(pnlService.getPnlSummary(user.getId()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSV Export endpoints
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/pnl/export/csv
     *
     * ✅ Matches your existing frontend call: /pnl/export/csv
     * Trade history CSV — same format as your existing export
     * + adds Realized P&L column for SELL rows.
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportTradeHistory() {
        User user = getAuthenticatedUser();
        String csv = csvExportService.generateTradeHistoryCsv(user.getId());
        String filename = "trade_history_" + LocalDate.now() + ".csv";
        return buildCsvResponse(csv, filename);
    }

    /**
     * GET /api/pnl/export/tax
     * GET /api/pnl/export/tax?year=2026
     *
     * Tax report CSV — SELL trades only with FIFO cost basis.
     * Optional year filter. Summary totals at the bottom.
     */
    @GetMapping("/export/tax")
    public ResponseEntity<byte[]> exportTaxReport(
            @RequestParam(required = false) Integer year) {
        User user = getAuthenticatedUser();
        String csv = csvExportService.generateTaxReportCsv(user.getId(), year);
        String filename = "tax_report_" + (year != null ? year : "all") + ".csv";
        return buildCsvResponse(csv, filename);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildCsvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .contentLength(bytes.length)
                .body(bytes);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}