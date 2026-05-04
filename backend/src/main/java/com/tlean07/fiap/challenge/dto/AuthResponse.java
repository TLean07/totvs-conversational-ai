package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private long expiresInSeconds;
    private UserResponse user;

    @Getter
    @Builder
    public static class UserResponse {
        private String id;
        private String email;
        private String name;
        private UserRole role;
    }
}
