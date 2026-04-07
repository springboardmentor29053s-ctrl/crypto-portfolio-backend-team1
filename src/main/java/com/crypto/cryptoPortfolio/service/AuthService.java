package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.dto.LoginRequest;
import com.crypto.cryptoPortfolio.dto.RegisterRequest;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationService verificationService;

    /**
     * ✅ UPDATED: Register user and send verification email
     */
    @Transactional
    public Map<String, String> register(RegisterRequest request) {

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);  // ✅ Not verified yet
        user.setAccountEnabled(false);  // ✅ Account disabled until verified

        User savedUser = userRepository.save(user);

        // ✅ Send verification email
        verificationService.sendEmailVerificationOTP(savedUser);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful! Please check your email for verification code.");
        response.put("email", savedUser.getEmail());

        return response;
    }

    /**
     * ✅ UPDATED: Login with email verification check
     */
    public Map<String, String> login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // ✅ Check if email is verified
        if (!user.getEmailVerified()) {
            throw new RuntimeException("Please verify your email before logging in");
        }

        // ✅ Check if account is enabled
        if (!user.getAccountEnabled()) {
            throw new RuntimeException("Account is disabled. Please contact support.");
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getEmail());

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("email", user.getEmail());
        response.put("name", user.getName());

        return response;
    }

    /**
     * ✅ NEW: Resend verification OTP
     */
    public void resendVerificationOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        verificationService.sendEmailVerificationOTP(user);
    }

    /**
     * ✅ NEW: Initiate password reset
     */
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        verificationService.sendPasswordResetOTP(user);
    }

    /**
     * ✅ NEW: Reset password with OTP
     */
    @Transactional
    public void resetPassword(String otp, String newPassword) {
        User user = verificationService.verifyPasswordResetOTP(otp)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}