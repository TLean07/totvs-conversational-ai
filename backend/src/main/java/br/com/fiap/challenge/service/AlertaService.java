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
import br.com.fiap.challenge.model.Transcricao;
import br.com.fiap.challenge.repository.AlertaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertaService {

    private final AlertaRepository alertaRepository;

    public AlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    public List<Alerta> gerarAlertas(AnaliseResultado analise, Transcricao transcricao) {
        List<Alerta> alertas = new ArrayList<>();

        for (Risco risco : analise.getRiscos()) {
            if (risco.getSeveridade() == Severidade.ALTO) {
                alertas.add(salvarAlerta(analise.getId(), "RISCO_" + risco.getTipo().name(),
                        buildMensagemRisco(risco, transcricao), Severidade.ALTO));
            }

            if (risco.getTipo() == TipoRisco.CONCORRENCIA && risco.getConcorrenteMencionado() != null) {
                alertas.add(salvarAlerta(analise.getId(), "CONCORRENTE_DETECTADO",
                        "Cliente avaliando concorrente: " + risco.getConcorrenteMencionado()
                                + ". " + risco.getDescricao(), Severidade.ALTO));
            }
        }

        for (Oportunidade oportunidade : analise.getOportunidades()) {
            if (oportunidade.getScore() != null && oportunidade.getScore() >= 0.7) {
                alertas.add(salvarAlerta(analise.getId(), "OPORTUNIDADE_ALTA",
                        "Oportunidade de " + oportunidade.getTipo().name()
                                + " com alta probabilidade: " + oportunidade.getDescricao(), Severidade.MEDIO));
            }
        }

        boolean temConcorrenteAtivo = analise.getProdutosIdentificados().stream()
                .anyMatch(p -> p.getTipo() == TipoProduto.CONCORRENTE);
        if (temConcorrenteAtivo && analise.getSentimentoGeral().name().equals("NEGATIVO")) {
            alertas.add(salvarAlerta(analise.getId(), "CHURN_IMINENTE",
                    "ATENCAO: Cliente com sentimento negativo e avaliando concorrentes. Risco de churn iminente!",
                    Severidade.ALTO));
        }

        return alertas;
    }

    private Alerta salvarAlerta(String analiseId, String tipo, String mensagem, Severidade prioridade) {
        Alerta alerta = Alerta.builder()
                .analiseId(analiseId)
                .tipo(tipo)
                .mensagem(mensagem)
                .prioridade(prioridade)
                .status(StatusAlerta.NOVO)
                .dataCriacao(LocalDateTime.now())
                .build();
        return alertaRepository.save(alerta);
    }

    private String buildMensagemRisco(Risco risco, Transcricao transcricao) {
        String cliente = transcricao.getCliente();
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

    public AlertaResponse marcarComoVisto(String id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
        alerta.setStatus(StatusAlerta.VISTO);
        return toResponse(alertaRepository.save(alerta));
    }

    public AlertaResponse marcarComoResolvido(String id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
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
