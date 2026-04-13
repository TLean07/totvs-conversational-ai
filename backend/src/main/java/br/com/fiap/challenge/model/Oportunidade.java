package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.TipoOportunidade;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "oportunidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Oportunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_oportunidade")
    @SequenceGenerator(name = "seq_oportunidade", sequenceName = "SEQ_OPORTUNIDADE", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analise_id", nullable = false)
    private AnaliseResultado analiseResultado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOportunidade tipo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(length = 100)
    private String produtoRelacionado;

    @Column
    private Double score;

    @Column(length = 100)
    private String valorEstimado;
}
