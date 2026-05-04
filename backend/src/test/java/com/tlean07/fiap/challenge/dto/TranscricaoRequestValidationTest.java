package com.tlean07.fiap.challenge.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscricaoRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsOversizedTranscript() {
        TranscricaoRequest request = TranscricaoRequest.builder()
                .textoOriginal("a".repeat(20001))
                .build();

        assertThat(validator.validate(request))
                .anyMatch(v -> v.getPropertyPath().toString().equals("textoOriginal"));
    }

    @Test
    void acceptsReasonableTranscript() {
        TranscricaoRequest request = TranscricaoRequest.builder()
                .textoOriginal("Reuniao comercial com cliente.")
                .cliente("Cliente")
                .duracaoMinutos(30)
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}
