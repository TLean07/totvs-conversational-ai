package com.tlean07.fiap.challenge.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "REFRESH_TOKENS",
        uniqueConstraints = @UniqueConstraint(name = "UK_REFRESH_TOKENS_HASH", columnNames = "TOKEN_HASH"),
        indexes = @Index(name = "IDX_REFRESH_TOKENS_USER", columnList = "USER_ID"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "USER_ID", length = 36, nullable = false)
    private String userId;

    @Column(name = "TOKEN_HASH", length = 64, nullable = false)
    private String tokenHash;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "REVOKED_AT")
    private LocalDateTime revokedAt;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
