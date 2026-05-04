package com.tlean07.fiap.challenge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.enums.UserRole;
import com.tlean07.fiap.challenge.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new ObjectMapper(),
            "0123456789abcdef0123456789abcdef",
            15
    );

    @Test
    void createsAndParsesSignedAccessToken() {
        AppUser user = AppUser.builder()
                .id("user-1")
                .email("admin@example.com")
                .name("Admin")
                .role(UserRole.ADMIN)
                .build();

        String token = jwtService.createAccessToken(user);
        AuthenticatedUser parsed = jwtService.parseAccessToken(token);

        assertThat(parsed.id()).isEqualTo("user-1");
        assertThat(parsed.email()).isEqualTo("admin@example.com");
        assertThat(parsed.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void rejectsTamperedAccessToken() {
        AppUser user = AppUser.builder()
                .id("user-1")
                .email("admin@example.com")
                .name("Admin")
                .role(UserRole.ADMIN)
                .build();

        String token = jwtService.createAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseAccessToken(tampered))
                .isInstanceOf(BadCredentialsException.class);
    }
}
