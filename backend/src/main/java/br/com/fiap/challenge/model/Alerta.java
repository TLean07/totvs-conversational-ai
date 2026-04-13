package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.Severidade;
import br.com.fiap.challenge.enums.StatusAlerta;
import lombok.*;
import org.springframework.data.annotation.Id;
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

    private String analiseId;
    private String tipo;
    private String mensagem;
    private Severidade prioridade;
    private StatusAlerta status;
    private LocalDateTime dataCriacao;

    public void prePersist() {
        dataCriacao = LocalDateTime.now();
        if (status == null) {
            status = StatusAlerta.NOVO;
        }
    }
}
