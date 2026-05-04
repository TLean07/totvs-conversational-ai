package com.tlean07.fiap.challenge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscricaoRequest {

    @NotBlank(message = "O texto da transcrição é obrigatório")
    @Size(max = 20000, message = "A transcrição deve ter no máximo 20000 caracteres")
    private String textoOriginal;

    @Size(max = 500)
    private String participantes;

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$", message = "Data da reunião deve estar no formato ISO local")
    private String dataReuniao;

    @Min(1)
    @Max(1440)
    private Integer duracaoMinutos;

    @Size(max = 160)
    private String cliente;

    @Size(max = 160)
    private String vendedor;
}
