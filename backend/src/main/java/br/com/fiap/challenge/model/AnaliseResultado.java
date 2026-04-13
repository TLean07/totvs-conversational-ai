package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.Sentimento;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "analises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseResultado {

    @Id
    private String id;

    private String transcricaoId;
    private Sentimento sentimentoGeral;
    private String resumo;
    private String personaIdentificada;
    private String budgetIdentificado;
    private Double scoreConfiancaVendedor;
    private LocalDateTime dataAnalise;

    @Builder.Default
    private List<ProdutoIdentificado> produtosIdentificados = new ArrayList<>();

    @Builder.Default
    private List<Oportunidade> oportunidades = new ArrayList<>();

    @Builder.Default
    private List<Risco> riscos = new ArrayList<>();

    @Builder.Default
    private List<Alerta> alertas = new ArrayList<>();

    public void prePersist() {
        dataAnalise = LocalDateTime.now();
    }
}
