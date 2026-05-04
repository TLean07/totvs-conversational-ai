package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String name;
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;
}
