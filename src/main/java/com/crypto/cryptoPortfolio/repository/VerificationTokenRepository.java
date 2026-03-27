//package com.crypto.cryptoPortfolio.repository;
//
//import com.crypto.cryptoPortfolio.entity.TokenType;
//import com.crypto.cryptoPortfolio.entity.User;
//import com.crypto.cryptoPortfolio.entity.VerificationToken;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
//@Repository
//public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
//
//    Optional<VerificationToken> findByOtpAndTypeAndUsedFalse(String otp, TokenType type);
//
//    Optional<VerificationToken> findByUserAndTypeAndUsedFalse(User user, TokenType type);
//
//    void deleteByUser(User user);
//}