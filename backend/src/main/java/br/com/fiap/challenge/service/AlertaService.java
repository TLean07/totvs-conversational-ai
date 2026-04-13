package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.AlertaResponse;
import br.com.fiap.challenge.enums.Severidade;
import br.com.fiap.challenge.enums.StatusAlerta;
import br.com.fiap.challenge.enums.TipoProduto;
import br.com.fiap.challenge.enums.TipoRisco;
import br.com.fiap.challenge.model.Alerta;
import br.com.fiap.challenge.model.AnaliseResultado;
import br.com.fiap.challenge.model.Oportunidade;
import br.com.fiap.challenge.model.Risco;
import br.com.fiap.challenge.repository.AlertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @Transactional
    public void gerarAlertas(AnaliseResultado analise) {
        for (Risco risco : analise.getRiscos()) {
            if (risco.getSeveridade() == Severidade.ALTO) {
                Alerta alerta = Alerta.builder()
                        .analiseResultado(analise)
                        .tipo("RISCO_" + risco.getTipo().name())
                        .mensagem(buildMensagemRisco(risco, analise))
                        .prioridade(Severidade.ALTO)
                        .status(StatusAlerta.NOVO)
                        .build();
                analise.getAlertas().add(alerta);
            }

            if (risco.getTipo() == TipoRisco.CONCORRENCIA && risco.getConcorrenteMencionado() != null) {
                Alerta alerta = Alerta.builder()
                        .analiseResultado(analise)
                        .tipo("CONCORRENTE_DETECTADO")
                        .mensagem("Cliente avaliando concorrente: " + risco.getConcorrenteMencionado()
                                + ". " + risco.getDescricao())
                        .prioridade(Severidade.ALTO)
                        .status(StatusAlerta.NOVO)
                        .build();
                analise.getAlertas().add(alerta);
            }
        }

        for (Oportunidade oportunidade : analise.getOportunidades()) {
            if (oportunidade.getScore() != null && oportunidade.getScore() >= 0.7) {
                Alerta alerta = Alerta.builder()
                        .analiseResultado(analise)
                        .tipo("OPORTUNIDADE_ALTA")
                        .mensagem("Oportunidade de " + oportunidade.getTipo().name()
                                + " com alta probabilidade: " + oportunidade.getDescricao())
                        .prioridade(Severidade.MEDIO)
                        .status(StatusAlerta.NOVO)
                        .build();
                analise.getAlertas().add(alerta);
            }
        }

        boolean temConcorrenteAtivo = analise.getProdutosIdentificados().stream()
                .anyMatch(p -> p.getTipo() == TipoProduto.CONCORRENTE);
        if (temConcorrenteAtivo && analise.getSentimentoGeral().name().equals("NEGATIVO")) {
            Alerta alerta = Alerta.builder()
                    .analiseResultado(analise)
                    .tipo("CHURN_IMINENTE")
                    .mensagem("ATENÇÃO: Cliente com sentimento negativo e avaliando concorrentes. Risco de churn iminente!")
                    .prioridade(Severidade.ALTO)
                    .status(StatusAlerta.NOVO)
                    .build();
            analise.getAlertas().add(alerta);
        }
    }

    private String buildMensagemRisco(Risco risco, AnaliseResultado analise) {
        String cliente = analise.getTranscricao().getCliente();
        return "Risco %s detectado%s: %s".formatted(
                risco.getTipo().name(),
                cliente != null ? " para cliente " + cliente : "",
                risco.getDescricao()
        );
    }

    public List<AlertaResponse> listarNovos() {
        return alertaRepository.findByStatusOrderByPrioridadeAscDataCriacaoDesc(StatusAlerta.NOVO)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AlertaResponse> listarTodos() {
        return alertaRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AlertaResponse marcarComoVisto(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado: " + id));
        alerta.setStatus(StatusAlerta.VISTO);
        return toResponse(alertaRepository.save(alerta));
    }

    @Transactional
    public AlertaResponse marcarComoResolvido(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado: " + id));
        alerta.setStatus(StatusAlerta.RESOLVIDO);
        return toResponse(alertaRepository.save(alerta));
    }

    public long contarNovos() {
        return alertaRepository.countByStatus(StatusAlerta.NOVO);
    }

    public AlertaResponse toResponse(Alerta a) {
        return AlertaResponse.builder()
                .id(a.getId())
                .tipo(a.getTipo())
                .mensagem(a.getMensagem())
                .prioridade(a.getPrioridade())
                .status(a.getStatus())
                .dataCriacao(a.getDataCriacao())
                .build();
    }
}
