package com.tlean07.fiap.challenge.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ANALISE_FEEDBACKS", indexes = {
        @Index(name = "IDX_FEEDBACK_OWNER", columnList = "OWNER_ID"),
        @Index(name = "IDX_FEEDBACK_TRANSCRICAO", columnList = "TRANSCRICAO_ID"),
        @Index(name = "IDX_FEEDBACK_ANALISE", columnList = "ANALISE_ID"),
        @Index(name = "IDX_FEEDBACK_CREATED", columnList = "CREATED_AT")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseFeedback {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "OWNER_ID", length = 36)
    private String ownerId;

    @Column(name = "TRANSCRICAO_ID", length = 36)
    private String transcricaoId;

    @Column(name = "ANALISE_ID", length = 36)
    private String analiseId;

    @Column(name = "UTIL", nullable = false)
    private boolean util;

    @Lob
    @Column(name = "AJUSTE")
    private String ajuste;

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
