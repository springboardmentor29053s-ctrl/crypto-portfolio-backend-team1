//package com.crypto.cryptoPortfolio.controller;
//
//import com.crypto.cryptoPortfolio.dto.*;
//import com.crypto.cryptoPortfolio.service.AuthService;
//import com.crypto.cryptoPortfolio.service.VerificationService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/auth")
//@CrossOrigin(origins = "http://localhost:3000")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final VerificationService verificationService;
//    private final AuthService authService;
//
//    /**
//     * ✅ Verify email with OTP
//     * POST /api/auth/verify-email
//     */
//    @PostMapping("/verify-email")
//    public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request) {
//        boolean verified = verificationService.verifyEmail(request.getOtp());
//
//        if (verified) {
//            return ResponseEntity.ok(new MessageResponse(
//                    "Email verified successfully! You can now login."
//            ));
//        } else {
//            return ResponseEntity.badRequest().body(new MessageResponse(
//                    "Invalid or expired OTP"
//            ));
//        }
//    }
//
//    /**
//     * ✅ Resend verification OTP
//     * POST /api/auth/resend-otp
//     */
//    @PostMapping("/resend-otp")
//    public ResponseEntity<?> resendOTP(@RequestBody ResendOTPRequest request) {
//        try {
//            authService.resendVerificationOTP(request.getEmail());
//            return ResponseEntity.ok(new MessageResponse(
//                    "Verification code sent to your email"
//            ));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
//        }
//    }
//
//    /**
//     * ✅ Forgot Password - Send OTP
//     * POST /api/auth/forgot-password
//     */
//    @PostMapping("/forgot-password")
//    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
//        try {
//            authService.initiatePasswordReset(request.getEmail());
//            return ResponseEntity.ok(new MessageResponse(
//                    "Password reset code sent to your email"
//            ));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
//        }
//    }
//
//    /**
//     * ✅ Reset Password with OTP
//     * POST /api/auth/reset-password
//     */
//    @PostMapping("/reset-password")
//    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
//        try {
//            authService.resetPassword(request.getOtp(), request.getNewPassword());
//            return ResponseEntity.ok(new MessageResponse(
//                    "Password reset successfully! You can now login with your new password."
//            ));
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
//        }
//    }
//}