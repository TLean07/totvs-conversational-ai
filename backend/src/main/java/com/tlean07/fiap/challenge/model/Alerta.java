package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.enums.Severidade;
import com.tlean07.fiap.challenge.enums.StatusAlerta;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "alertas")
@CompoundIndexes({
        @CompoundIndex(name = "status_priority_created", def = "{'status': 1, 'prioridade': 1, 'dataCriacao': -1}"),
        @CompoundIndex(name = "owner_status_priority_created",
                def = "{'ownerId': 1, 'status': 1, 'prioridade': 1, 'dataCriacao': -1}")
})
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
    private String titulo;
    private String categoria;
    private String mensagem;
    private String motivo;
    private String recomendacao;
    private Integer ocorrencias;
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
