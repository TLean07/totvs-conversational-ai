package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.StatusTranscricao;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.LocalDateTime;

@Document(collection = "transcricoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcricao {

    @Id
    private String id;

    private String ownerId;
    private String textoOriginal;
    private String participantes;
    private LocalDateTime dataReuniao;
    private Integer duracaoMinutos;
    private String cliente;
    private String vendedor;
    private StatusTranscricao status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataProcessamento;

    @DBRef
    private AnaliseResultado analiseResultado;

    public void prePersist() {
        dataCriacao = LocalDateTime.now();
        if (status == null) {
            status = StatusTranscricao.PENDENTE;
        }
    }
}
