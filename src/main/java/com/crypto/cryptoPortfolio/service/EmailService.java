package com.crypto.cryptoPortfolio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send email verification OTP
     */
    public void sendVerificationEmail(String to, String otp, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Verify Your Email - Crypto Portfolio");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Welcome to Crypto Portfolio!\n\n" +
                            "Your email verification code is: %s\n\n" +
                            "This code will expire in 10 minutes.\n\n" +
                            "If you didn't create an account, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "Crypto Portfolio Team",
                    name, otp
            ));

            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    /**
     * Send password reset OTP
     */
    public void sendPasswordResetEmail(String to, String otp, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Reset Your Password - Crypto Portfolio");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "We received a request to reset your password.\n\n" +
                            "Your password reset code is: %s\n\n" +
                            "This code will expire in 10 minutes.\n\n" +
                            "If you didn't request a password reset, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "Crypto Portfolio Team",
                    name, otp
            ));

            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }
}