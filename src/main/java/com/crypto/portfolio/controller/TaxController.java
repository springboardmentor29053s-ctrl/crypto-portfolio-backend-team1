package com.crypto.portfolio.controller;

import com.crypto.portfolio.dto.TaxReportResponse;
import com.crypto.portfolio.service.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @GetMapping("/report")
    public TaxReportResponse getTaxReport() {

        String username = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return taxService.generateTaxReport(username);
    }
}