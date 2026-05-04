package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.AuthResponse;
import com.tlean07.fiap.challenge.dto.LoginRequest;
import com.tlean07.fiap.challenge.model.AppUser;
import com.tlean07.fiap.challenge.model.RefreshToken;
import com.tlean07.fiap.challenge.repository.AppUserRepository;
import com.tlean07.fiap.challenge.repository.RefreshTokenRepository;
import com.tlean07.fiap.challenge.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

    public static final String REFRESH_COOKIE = "refresh_token";

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenDays;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthService(AppUserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.refresh-token-days:14}") long refreshTokenDays,
                       @Value("${app.cookie.secure:true}") boolean cookieSecure,
                       @Value("${app.cookie.same-site:Lax}") String cookieSameSite) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenDays = refreshTokenDays;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    public AuthSession login(LoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .filter(AppUser::isActive)
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        return createSession(user);
    }

    public AuthSession refresh(String rawRefreshToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalido"));

        AppUser user = userRepository.findById(current.getUserId())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new BadCredentialsException("Usuario invalido"));

        current.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(current);
        return createSession(user);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(0)
                .build();
    }

    private AuthSession createSession(AppUser user) {
        String rawRefreshToken = randomToken();
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawRefreshToken))
                .createdAt(now)
                .expiresAt(now.plusDays(refreshTokenDays))
                .build());

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ofDays(refreshTokenDays))
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken(jwtService.createAccessToken(user))
                .expiresInSeconds(jwtService.expiresInSeconds())
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole())
                        .build())
                .build();

        return new AuthSession(response, cookie);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao proteger refresh token", e);
        }
    }

    public record AuthSession(AuthResponse response, ResponseCookie refreshCookie) {
    }
}
