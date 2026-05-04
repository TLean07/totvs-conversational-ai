package com.tlean07.fiap.challenge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "analise_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseFeedback {

    @Id
    private String id;

    @Indexed
    private String ownerId;
    @Indexed
    private String transcricaoId;
    @Indexed
    private String analiseId;

    private boolean util;
    private String ajuste;
    @Indexed
    private LocalDateTime createdAt;
}
