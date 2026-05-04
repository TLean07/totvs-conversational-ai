package com.tlean07.fiap.challenge.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnaliseFeedbackRequest {
    private boolean util;

    @Size(max = 500)
    private String ajuste;
}
