package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.UserRole;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String passwordHash;
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;
}
