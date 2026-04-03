package com.crypto.portfoliotracker.service;

import com.crypto.portfoliotracker.entity.RiskAlert;
import com.crypto.portfoliotracker.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.mail.host", havingValue = "smtp.gmail.com", matchIfMissing = false)
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send simple email
     */
    public void sendEmail(String to, String subject, String message) {
        try {
            SimpleMailMessage emailMessage = new SimpleMailMessage();
            emailMessage.setFrom(fromEmail);
            emailMessage.setTo(to);
            emailMessage.setSubject(subject);
            emailMessage.setText(message);
            mailSender.send(emailMessage);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Send risk alert email to user
     */
    public void sendRiskAlertEmail(User user, RiskAlert alert) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject(getEmailSubject(alert));
            message.setText(buildEmailBody(user, alert));
            
            mailSender.send(message);
            System.out.println("Risk alert email sent to: " + user.getEmail());
            
        } catch (Exception e) {
            System.err.println("Failed to send risk alert email: " + e.getMessage());
            throw new RuntimeException("Failed to send email notification", e);
        }
    }

    public void sendPriceAlert(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            
            System.out.println("✅ Email sent to " + to + " with subject: " + subject);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            
            mailSender.send(message);
            
            System.out.println("✅ HTML email sent to " + to + " with subject: " + subject);
        } catch (Exception e) {
            System.err.println("Failed to send HTML email: " + e.getMessage());
        }
    }
    
    /**
     * Get email subject based on alert type and severity
     */
    private String getEmailSubject(RiskAlert alert) {
        String baseSubject = "Crypto Risk Alert";
        
        // Determine severity based on alert type
        String severity = getSeverityFromAlertType(alert.getAlertType());
        
        if ("CRITICAL".equals(severity)) {
            baseSubject = "🚨 " + baseSubject + " - CRITICAL";
        } else if ("HIGH".equals(severity)) {
            baseSubject = "⚠️ " + baseSubject + " - HIGH";
        } else {
            baseSubject = "ℹ️ " + baseSubject;
        }
        
        return baseSubject + ": " + alert.getAssetSymbol();
    }
    
    private String getSeverityFromAlertType(RiskAlert.AlertType alertType) {
        switch (alertType) {
            case RUGPULL_WARNING:
            case NOT_ON_COINGECKO:
                return "CRITICAL";
            case CONTRACT_RISK:
            case LIQUIDITY_RISK:
                return "HIGH";
            case HOLDER_CONCENTRATION:
            case LOW_MARKET_CAP:
            case HIGH_VOLATILITY:
                return "MEDIUM";
            case NEWS:
            case PRICE_VOLATILITY:
                return "LOW";
            default:
                return "LOW";
        }
    }
    
    /**
     * Build email body for risk alert
     */
    private String buildEmailBody(User user, RiskAlert alert) {
        StringBuilder body = new StringBuilder();
        
        body.append("Hello ").append(user.getName()).append(",\n\n");
        
        body.append("A new risk alert has been generated for your portfolio:\n\n");
        
        body.append("Asset: ").append(alert.getAssetSymbol()).append("\n");
        body.append("Alert Type: ").append(alert.getAlertType().toString().replace("_", " ")).append("\n");
        body.append("Severity: ").append(getSeverityFromAlertType(alert.getAlertType())).append("\n");
        body.append("Details: ").append(alert.getDetails()).append("\n");
        body.append("Time: ").append(alert.getCreatedAt()).append("\n\n");
        
        body.append("Please review this alert and take appropriate action.\n\n");
        
        body.append("You can view all your alerts in the Risk Dashboard.\n\n");
        
        body.append("Stay safe,\n");
        body.append("Crypto Portfolio Tracker Team");
        
        return body.toString();
    }
}
