package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.TipoProduto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "produtos_identificados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoIdentificado {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_produto")
    @SequenceGenerator(name = "seq_produto", sequenceName = "SEQ_PRODUTO", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analise_id", nullable = false)
    private AnaliseResultado analiseResultado;

    @Column(nullable = false, length = 100)
    private String nomeProduto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProduto tipo;

    @Column(length = 1000)
    private String contexto;
}
