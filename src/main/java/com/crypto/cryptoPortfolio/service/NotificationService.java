    package com.crypto.cryptoPortfolio.service;

    import com.crypto.cryptoPortfolio.entity.RiskAlert;
    import com.crypto.cryptoPortfolio.entity.User;
    import jakarta.mail.MessagingException;
    import jakarta.mail.internet.MimeMessage;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.mail.javamail.JavaMailSender;
    import org.springframework.mail.javamail.MimeMessageHelper;
    import org.springframework.stereotype.Service;

    import java.time.ZoneId;
    import java.time.format.DateTimeFormatter;
    import java.util.List;

    /**
     * Sends email notifications to users when risk alerts are created.
     *
     * Two entry points:
     *   sendAlertEmail(user, alert)       — single alert (called right after createAlert)
     *   sendDailySummary(user, alerts)    — digest of all active alerts (called by scheduler)
     *
     * Requires these properties in application.properties:
     *   spring.mail.host=smtp.gmail.com
     *   spring.mail.port=587
     *   spring.mail.username=your@gmail.com
     *   spring.mail.password=your-app-password
     *   spring.mail.properties.mail.smtp.auth=true
     *   spring.mail.properties.mail.smtp.starttls.enable=true
     *   app.notification.from=your@gmail.com
     *   app.notification.enabled=true
     */
    @Service
    public class NotificationService {

        private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

        private static final DateTimeFormatter DISPLAY_FMT =
                DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                        .withZone(ZoneId.of("Asia/Kolkata")); // IST for Chennai users

        @Value("${app.notification.from:noreply@cryptoportfolio.com}")
        private String fromAddress;

        @Value("${app.notification.enabled:true}")
        private boolean notificationsEnabled;

        private final JavaMailSender mailSender;

        public NotificationService(JavaMailSender mailSender) {
            this.mailSender = mailSender;
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Public API
        // ─────────────────────────────────────────────────────────────────────────

        /**
         * Send an immediate email for a single freshly created alert.
         * Call this right after riskAlertRepository.save() in either scan service.
         */
        public void sendAlertEmail(User user, RiskAlert alert) {
            if (!notificationsEnabled) return;
            if (user.getEmail() == null || user.getEmail().isBlank()) return;

            String subject = buildSubject(alert);
            String html    = buildSingleAlertHtml(user, alert);

            send(user.getEmail(), subject, html);
        }

        /**
         * Send a daily digest email with all active (non-dismissed) alerts.
         * Called by RiskScheduler once a day.
         */
        public void sendDailySummary(User user, List<RiskAlert> activeAlerts) {
            if (!notificationsEnabled) return;
            if (user.getEmail() == null || user.getEmail().isBlank()) return;
            if (activeAlerts == null || activeAlerts.isEmpty()) return;

            String subject = String.format("📊 Daily Risk Summary — %d active alert(s) in your portfolio",
                    activeAlerts.size());
            String html = buildSummaryHtml(user, activeAlerts);

            send(user.getEmail(), subject, html);
        }

        // ─────────────────────────────────────────────────────────────────────────
        // Private helpers
        // ─────────────────────────────────────────────────────────────────────────

        private void send(String to, String subject, String htmlBody) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromAddress);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true); // true = isHtml

                mailSender.send(message);
                log.info("Notification sent to {}: {}", to, subject);

            } catch (MessagingException e) {
                log.error("Failed to send notification to {}: {}", to, e.getMessage());
            }
        }

        private String buildSubject(RiskAlert alert) {
            return switch (alert.getAlertType()) {
                case rugpull_warning  -> "🚨 Rug Pull Warning — " + alert.getAssetSymbol();
                case contract_risk    -> "⚠️ Contract Risk — " + alert.getAssetSymbol();
                case price_drop       -> "📉 Price Drop Alert — " + alert.getAssetSymbol();
                case volatility_warning -> "📊 Volatility Spike — " + alert.getAssetSymbol();
                case portfolio_loss   -> "🔴 Portfolio Loss Alert";
                case news             -> "📰 News Alert — " + alert.getAssetSymbol();
            };
        }

        // ── Single alert HTML ─────────────────────────────────────────────────────

        private String buildSingleAlertHtml(User user, RiskAlert alert) {
            String badgeColor  = severityColor(alert.getAlertType());
            String badgeLabel  = alert.getAlertType().name().replace("_", " ").toUpperCase();
            String createdAt   = DISPLAY_FMT.format(alert.getCreatedAt());

            return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, sans-serif; background: #0f0f0f; color: #e0e0e0; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 32px auto; background: #1a1a1a; border-radius: 12px; overflow: hidden; border: 1px solid #2a2a2a; }
                    .header { background: #1a1a1a; padding: 24px 32px; border-bottom: 1px solid #2a2a2a; }
                    .logo { font-size: 20px; font-weight: 700; color: #f5a623; }
                    .body { padding: 32px; }
                    .greeting { font-size: 16px; color: #aaa; margin-bottom: 16px; }
                    .alert-card { background: #222; border-radius: 10px; padding: 20px 24px; border-left: 4px solid %s; margin-bottom: 20px; }
                    .badge { display: inline-block; background: %s22; color: %s; font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 4px; letter-spacing: 0.5px; margin-bottom: 12px; }
                    .symbol { font-size: 18px; font-weight: 700; color: #fff; margin-bottom: 8px; }
                    .details { font-size: 14px; color: #ccc; line-height: 1.6; }
                    .timestamp { font-size: 12px; color: #666; margin-top: 12px; }
                    .cta { display: block; width: fit-content; margin: 24px auto 0; background: #f5a623; color: #000; font-weight: 700; font-size: 14px; padding: 12px 28px; border-radius: 8px; text-decoration: none; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #555; border-top: 1px solid #2a2a2a; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <div class="logo">⚡ CryptoPortfolio</div>
                    </div>
                    <div class="body">
                      <p class="greeting">Hi %s,</p>
                      <div class="alert-card">
                        <div class="badge">%s</div>
                        <div class="symbol">%s</div>
                        <div class="details">%s</div>
                        <div class="timestamp">%s IST</div>
                      </div>
                      <a class="cta" href="http://localhost:3000/alerts">View Alerts Dashboard →</a>
                    </div>
                    <div class="footer">
                      You're receiving this because you have risk alerts enabled.<br/>
                      <a href="#" style="color:#666">Manage notification preferences</a>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                    badgeColor, badgeColor, badgeColor,
                    user.getName() != null ? user.getName() : "there",
                    badgeLabel,
                    alert.getAssetSymbol(),
                    alert.getDetails(),
                    createdAt
            );
        }

        // ── Summary HTML ─────────────────────────────────────────────────────────

        private String buildSummaryHtml(User user, List<RiskAlert> alerts) {
            StringBuilder rows = new StringBuilder();
            for (RiskAlert alert : alerts) {
                String color = severityColor(alert.getAlertType());
                String label = alert.getAlertType().name().replace("_", " ").toUpperCase();
                rows.append("""
                    <tr>
                      <td style="padding:12px 16px;border-bottom:1px solid #2a2a2a;">
                        <span style="display:inline-block;background:%s22;color:%s;font-size:11px;font-weight:700;padding:2px 8px;border-radius:4px;">%s</span>
                      </td>
                      <td style="padding:12px 16px;border-bottom:1px solid #2a2a2a;font-weight:600;color:#fff;">%s</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #2a2a2a;color:#ccc;font-size:13px;">%s</td>
                    </tr>
                    """.formatted(color, color, label, alert.getAssetSymbol(), alert.getDetails()));
            }

            return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, sans-serif; background: #0f0f0f; color: #e0e0e0; margin: 0; padding: 0; }
                    .container { max-width: 640px; margin: 32px auto; background: #1a1a1a; border-radius: 12px; overflow: hidden; border: 1px solid #2a2a2a; }
                    .header { padding: 24px 32px; border-bottom: 1px solid #2a2a2a; }
                    .logo { font-size: 20px; font-weight: 700; color: #f5a623; }
                    .body { padding: 32px; }
                    table { width: 100%%; border-collapse: collapse; border-radius: 8px; overflow: hidden; background: #222; }
                    th { text-align: left; padding: 10px 16px; font-size: 11px; text-transform: uppercase; color: #666; background: #1e1e1e; letter-spacing: 0.5px; }
                    .cta { display: block; width: fit-content; margin: 24px auto 0; background: #f5a623; color: #000; font-weight: 700; font-size: 14px; padding: 12px 28px; border-radius: 8px; text-decoration: none; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #555; border-top: 1px solid #2a2a2a; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <div class="logo">⚡ CryptoPortfolio</div>
                    </div>
                    <div class="body">
                      <p style="color:#aaa;margin-bottom:20px;">Hi %s — here's your daily risk summary. You have <strong style="color:#fff">%d active alert(s)</strong>.</p>
                      <table>
                        <thead><tr><th>Type</th><th>Asset</th><th>Details</th></tr></thead>
                        <tbody>%s</tbody>
                      </table>
                      <a class="cta" href="http://localhost:3000/alerts">View &amp; Manage Alerts →</a>
                    </div>
                    <div class="footer">
                      Daily digest · <a href="#" style="color:#666">Unsubscribe</a>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                    user.getName() != null ? user.getName() : "there",
                    alerts.size(),
                    rows.toString()
            );
        }

        /** Returns a hex color that maps to alert severity/type. */
        private String severityColor(RiskAlert.AlertType type) {
            return switch (type) {
                case rugpull_warning    -> "#e53e3e"; // red
                case portfolio_loss     -> "#e53e3e"; // red
                case price_drop         -> "#ed8936"; // orange
                case contract_risk      -> "#f5a623"; // amber
                case volatility_warning -> "#f6e05e"; // yellow
                case news               -> "#63b3ed"; // blue
            };
        }
    }