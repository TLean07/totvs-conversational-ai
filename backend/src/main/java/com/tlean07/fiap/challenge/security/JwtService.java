package com.tlean07.fiap.challenge.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.enums.UserRole;
import com.tlean07.fiap.challenge.model.AppUser;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long accessTokenMinutes;

    public JwtService(ObjectMapper objectMapper,
                      @Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("APP_JWT_SECRET deve ter pelo menos 32 caracteres");
        }
    }

    public long expiresInSeconds() {
        return accessTokenMinutes * 60;
    }

    public String createAccessToken(AppUser user) {
        try {
            Instant now = Instant.now();
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getId());
            payload.put("email", user.getEmail());
            payload.put("name", user.getName());
            payload.put("role", user.getRole().name());
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", now.plusSeconds(expiresInSeconds()).getEpochSecond());

            String encodedHeader = encodeJson(header);
            String encodedPayload = encodeJson(payload);
            String signingInput = encodedHeader + "." + encodedPayload;
            return signingInput + "." + sign(signingInput);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar token", e);
        }
    }

    public AuthenticatedUser parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BadCredentialsException("Token invalido");
            }

            String signingInput = parts[0] + "." + parts[1];
            byte[] expected = sign(signingInput).getBytes(StandardCharsets.UTF_8);
            byte[] actual = parts[2].getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new BadCredentialsException("Assinatura invalida");
            }

            Map<String, Object> payload = objectMapper.readValue(
                    URL_DECODER.decode(parts[1]),
                    new TypeReference<>() {}
            );
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new BadCredentialsException("Token expirado");
            }

            return new AuthenticatedUser(
                    (String) payload.get("sub"),
                    (String) payload.get("email"),
                    (String) payload.get("name"),
                    UserRole.valueOf((String) payload.get("role"))
            );
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Token invalido", e);
        }
    }

    private String encodeJson(Map<String, Object> data) throws Exception {
        return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(data));
    }

    private String sign(String signingInput) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
    }
}
