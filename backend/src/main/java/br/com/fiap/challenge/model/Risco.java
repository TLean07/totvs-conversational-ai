package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.Severidade;
import br.com.fiap.challenge.enums.TipoRisco;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "riscos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Risco {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_risco")
    @SequenceGenerator(name = "seq_risco", sequenceName = "SEQ_RISCO", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analise_id", nullable = false)
    private AnaliseResultado analiseResultado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRisco tipo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severidade severidade;

    @Column(length = 100)
    private String concorrenteMencionado;
}
