package com.crypto.cryptoPortfolio.dto;

import lombok.Data;

// ========================================
// Request DTOs
// ========================================

@Data
public class ResetPasswordRequest {
    private String otp;
    private String newPassword;
}

// ========================================
// Response DTOs
// ========================================

