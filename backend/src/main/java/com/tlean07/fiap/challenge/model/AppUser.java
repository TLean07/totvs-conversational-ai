package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "APP_USERS", uniqueConstraints = {
        @UniqueConstraint(name = "UK_APP_USERS_EMAIL", columnNames = "EMAIL")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "EMAIL", length = 320, nullable = false)
    private String email;

    @Column(name = "USER_NAME", length = 200)
    private String name;

    @Column(name = "PASSWORD_HASH", length = 200)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "USER_ROLE", length = 20)
    private UserRole role;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean active;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
