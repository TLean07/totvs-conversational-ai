package com.tlean07.fiap.challenge.controller;

import com.tlean07.fiap.challenge.dto.AuthResponse;
import com.tlean07.fiap.challenge.dto.LoginRequest;
import com.tlean07.fiap.challenge.service.AuditService;
import com.tlean07.fiap.challenge.service.AuthService;
import com.tlean07.fiap.challenge.service.OriginValidationService;
import com.tlean07.fiap.challenge.service.RateLimitService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final AuditService auditService;
    private final OriginValidationService originValidationService;

    public AuthController(AuthService authService,
                          RateLimitService rateLimitService,
                          AuditService auditService,
                          OriginValidationService originValidationService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.auditService = auditService;
        this.originValidationService = originValidationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        originValidationService.validate(httpRequest);
        String client = clientKey(httpRequest);
        rateLimitService.check("login:ip:" + client, 12, Duration.ofMinutes(10));
        rateLimitService.check("login:email:" + request.getEmail().toLowerCase(), 8, Duration.ofMinutes(10));
        try {
            AuthService.AuthSession session = authService.login(request);
            auditService.record("auth.login", session.response().getUser().getId(), client, "success");
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, session.refreshCookie().toString())
                    .body(session.response());
        } catch (RuntimeException e) {
            auditService.record("auth.login", "-", client, "failure");
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        originValidationService.validate(request);
        rateLimitService.check("refresh:ip:" + clientKey(request), 60, Duration.ofMinutes(10));
        AuthService.AuthSession session = authService.refresh(readRefreshCookie(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, session.refreshCookie().toString())
                .body(session.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        originValidationService.validate(request);
        authService.logout(readRefreshCookie(request));
        auditService.record("auth.logout", "-", clientKey(request), "success");
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
                .build();
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return "";
        }
        for (Cookie cookie : request.getCookies()) {
            if (AuthService.REFRESH_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
