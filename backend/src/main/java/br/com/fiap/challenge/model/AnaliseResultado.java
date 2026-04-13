package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.Sentimento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analises_resultado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnaliseResultado {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_analise")
    @SequenceGenerator(name = "seq_analise", sequenceName = "SEQ_ANALISE", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcricao_id", nullable = false)
    private Transcricao transcricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sentimento sentimentoGeral;

    @Column(length = 2000)
    private String resumo;

    @Column(length = 500)
    private String personaIdentificada;

    @Column(length = 100)
    private String budgetIdentificado;

    @Column
    private Double scoreConfiancaVendedor;

    @Column(nullable = false)
    private LocalDateTime dataAnalise;

    @Builder.Default
    @OneToMany(mappedBy = "analiseResultado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProdutoIdentificado> produtosIdentificados = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "analiseResultado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Oportunidade> oportunidades = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "analiseResultado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Risco> riscos = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "analiseResultado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Alerta> alertas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dataAnalise = LocalDateTime.now();
    }
}
