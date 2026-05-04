package com.tlean07.fiap.challenge.security;

import com.tlean07.fiap.challenge.enums.UserRole;

public record AuthenticatedUser(String id, String email, String name, UserRole role) {
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
