package br.com.fiap.challenge.dto;

import br.com.fiap.challenge.enums.StatusTranscricao;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscricaoResponse {
    private String id;
    private String textoOriginal;
    private String participantes;
    private LocalDateTime dataReuniao;
    private Integer duracaoMinutos;
    private String cliente;
    private String vendedor;
    private StatusTranscricao status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataProcessamento;
    private AnaliseResponse analise;
}
