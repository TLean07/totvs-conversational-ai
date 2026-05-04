package com.tlean07.fiap.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnaliseFeedbackResponse {
    private String id;
    private String transcricaoId;
    private String analiseId;
    private boolean util;
    private String ajuste;
    private LocalDateTime createdAt;
}
