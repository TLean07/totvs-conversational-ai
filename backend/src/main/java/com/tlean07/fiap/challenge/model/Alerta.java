package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alertas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    private String id;

    @Indexed
    private String ownerId;
    @Indexed
    private String analiseId;
    private String tipo;
    private String mensagem;
    private Severidade prioridade;
    @Indexed
    private StatusAlerta status;
    @Indexed
    private LocalDateTime dataCriacao;

    public void prePersist() {
        dataCriacao = LocalDateTime.now();
        if (status == null) {
            status = StatusAlerta.NOVO;
        }
    }
}
