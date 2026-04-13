package br.com.fiap.challenge.model;

import br.com.fiap.challenge.enums.StatusTranscricao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcricoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcricao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_transcricao")
    @SequenceGenerator(name = "seq_transcricao", sequenceName = "SEQ_TRANSCRICAO", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 4000)
    private String textoOriginal;

    @Column(length = 500)
    private String participantes;

    @Column
    private LocalDateTime dataReuniao;

    @Column
    private Integer duracaoMinutos;

    @Column(length = 200)
    private String cliente;

    @Column(length = 200)
    private String vendedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTranscricao status;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Column
    private LocalDateTime dataProcessamento;

    @OneToOne(mappedBy = "transcricao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AnaliseResultado analiseResultado;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        if (status == null) {
            status = StatusTranscricao.PENDENTE;
        }
    }
}
