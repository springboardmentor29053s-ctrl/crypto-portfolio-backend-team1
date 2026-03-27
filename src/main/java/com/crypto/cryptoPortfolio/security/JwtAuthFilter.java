package com.crypto.cryptoPortfolio.security;

import com.crypto.cryptoPortfolio.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter
 * Intercepts all requests and validates JWT tokens
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Extract Authorization header
            String header = request.getHeader("Authorization");

            // If no Authorization header or doesn't start with "Bearer ", skip authentication
            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract token (remove "Bearer " prefix)
            String token = header.substring(7);

            // Validate token
            boolean valid = jwtService.validateToken(token);

            if (!valid) {
                System.out.println("❌ Invalid token: " + token.substring(0, Math.min(20, token.length())) + "...");
                filterChain.doFilter(request, response);
                return;
            }

            // Extract email from token
            String email = jwtService.extractEmail(token);

            // ✅ NULL CHECK: Ensure email exists
            if (email == null || email.isEmpty()) {
                System.out.println("❌ No email found in token");
                filterChain.doFilter(request, response);
                return;
            }

            // ✅ OPTIMIZATION: Only set authentication if not already set
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(() -> "ROLE_USER")
                        );

                // Set authentication details
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                System.out.println("✅ Authentication set for: " + email + " | Path: " + request.getRequestURI());
            }

        } catch (Exception e) {
            // ✅ ERROR HANDLING: Don't crash on JWT parsing errors
            System.err.println("❌ JWT Filter Error: " + e.getMessage());
            // Continue filter chain even on error (will be handled as unauthenticated)
        }

        // Always continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * ✅ OPTIMIZATION: Skip filter for public endpoints
     * This improves performance by not processing JWT for public routes
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip JWT filter for these paths
        return path.startsWith("/api/login") ||
                path.startsWith("/api/signup") ||
                path.startsWith("/api/auth/verify-email") ||
                path.startsWith("/api/auth/resend-otp") ||
                path.startsWith("/api/auth/forgot-password") ||
                path.startsWith("/api/auth/reset-password") ||
                path.equals("/error") ||
                path.startsWith("/actuator/");
    }
}