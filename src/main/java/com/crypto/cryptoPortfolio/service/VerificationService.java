package com.crypto.cryptoPortfolio.service;

import com.crypto.cryptoPortfolio.entity.TokenType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import com.crypto.cryptoPortfolio.entity.User;
import com.crypto.cryptoPortfolio.entity.VerificationToken;
import com.crypto.cryptoPortfolio.repository.UserRepository;
import com.crypto.cryptoPortfolio.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;

    /**
     * Generate and send email verification OTP
     */
    @Transactional
    public void sendEmailVerificationOTP(User user) {
        // Delete any existing unused tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate new OTP
        String otp = generateOTP();

        // Create and save token
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setOtp(otp);
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        token.setUsed(false);

        tokenRepository.save(token);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), otp, user.getName());
    }

    /**
     * Generate and send password reset OTP
     */
    @Transactional
    public void sendPasswordResetOTP(User user) {
        // Delete any existing unused tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate new OTP
        String otp = generateOTP();

        // Create and save token
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setOtp(otp);
        token.setType(TokenType.PASSWORD_RESET);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        token.setUsed(false);

        tokenRepository.save(token);

        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), otp, user.getName());
    }

    /**
     * Verify email with OTP
     */
    @Transactional
    public boolean verifyEmail(String otp) {
        Optional<VerificationToken> tokenOpt = tokenRepository
                .findByOtpAndTypeAndUsedFalse(otp, TokenType.EMAIL_VERIFICATION);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        VerificationToken token = tokenOpt.get();

        // Check if expired
        if (token.isExpired()) {
            return false;
        }

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);

        // Update user
        User user = token.getUser();
        user.setEmailVerified(true);
        user.setAccountEnabled(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        return true;
    }

    /**
     * Verify password reset OTP
     */
    @Transactional
    public Optional<User> verifyPasswordResetOTP(String otp) {
        Optional<VerificationToken> tokenOpt = tokenRepository
                .findByOtpAndTypeAndUsedFalse(otp, TokenType.PASSWORD_RESET);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        VerificationToken token = tokenOpt.get();

        // Check if expired
        if (token.isExpired()) {
            return Optional.empty();
        }

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);

        return Optional.of(token.getUser());
    }

    /**
     * Generate random 6-digit OTP
     */
    private String generateOTP() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(otp);
    }

    private String hashOtp(String otp) {
        return BCrypt.hashpw(otp, BCrypt.gensalt());
    }

    private boolean validateOtp(String plainOtp, String hashedOtp) {
        return BCrypt.checkpw(plainOtp, hashedOtp);
    }
}