package com.tlean07.fiap.challenge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscricaoRequest {

    @NotBlank(message = "O texto da transcrição é obrigatório")
    private String textoOriginal;

    private String participantes;
    private String dataReuniao;
    private Integer duracaoMinutos;
    private String cliente;
    private String vendedor;
}
