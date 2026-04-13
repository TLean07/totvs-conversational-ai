package br.com.fiap.challenge.service;

import br.com.fiap.challenge.dto.*;
import br.com.fiap.challenge.enums.*;
import br.com.fiap.challenge.model.*;
import br.com.fiap.challenge.repository.AnaliseResultadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnaliseService {

    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);

    private final ClaudeAIService claudeAIService;
    private final AnaliseResultadoRepository analiseRepository;
    private final AlertaService alertaService;

    public AnaliseService(ClaudeAIService claudeAIService,
                          AnaliseResultadoRepository analiseRepository,
                          AlertaService alertaService) {
        this.claudeAIService = claudeAIService;
        this.analiseRepository = analiseRepository;
        this.alertaService = alertaService;
    }

    @Transactional
    public AnaliseResultado processarAnalise(Transcricao transcricao) {
        log.info("Iniciando análise da transcrição ID: {}", transcricao.getId());

        ClaudeAnaliseDTO dto = claudeAIService.analisarTranscricao(transcricao.getTextoOriginal());

        AnaliseResultado analise = AnaliseResultado.builder()
                .transcricao(transcricao)
                .sentimentoGeral(Sentimento.valueOf(dto.getSentimentoGeral()))
                .resumo(dto.getResumo())
                .personaIdentificada(dto.getPersonaIdentificada())
                .budgetIdentificado(dto.getBudgetIdentificado())
                .scoreConfiancaVendedor(dto.getScoreConfiancaVendedor())
                .build();

        if (dto.getProdutos() != null) {
            for (ClaudeAnaliseDTO.ProdutoDTO p : dto.getProdutos()) {
                ProdutoIdentificado produto = ProdutoIdentificado.builder()
                        .analiseResultado(analise)
                        .nomeProduto(p.getNomeProduto())
                        .tipo(TipoProduto.valueOf(p.getTipo()))
                        .contexto(p.getContexto())
                        .build();
                analise.getProdutosIdentificados().add(produto);
            }
        }

        if (dto.getOportunidades() != null) {
            for (ClaudeAnaliseDTO.OportunidadeDTO o : dto.getOportunidades()) {
                Oportunidade oportunidade = Oportunidade.builder()
                        .analiseResultado(analise)
                        .tipo(TipoOportunidade.valueOf(o.getTipo()))
                        .descricao(o.getDescricao())
                        .produtoRelacionado(o.getProdutoRelacionado())
                        .score(o.getScore())
                        .valorEstimado(o.getValorEstimado())
                        .build();
                analise.getOportunidades().add(oportunidade);
            }
        }

        if (dto.getRiscos() != null) {
            for (ClaudeAnaliseDTO.RiscoDTO r : dto.getRiscos()) {
                Risco risco = Risco.builder()
                        .analiseResultado(analise)
                        .tipo(TipoRisco.valueOf(r.getTipo()))
                        .descricao(r.getDescricao())
                        .severidade(Severidade.valueOf(r.getSeveridade()))
                        .concorrenteMencionado(r.getConcorrenteMencionado())
                        .build();
                analise.getRiscos().add(risco);
            }
        }

        analise = analiseRepository.save(analise);

        alertaService.gerarAlertas(analise);

        log.info("Análise concluída para transcrição ID: {}", transcricao.getId());
        return analise;
    }

    public AnaliseResponse toResponse(AnaliseResultado a) {
        return AnaliseResponse.builder()
                .id(a.getId())
                .sentimentoGeral(a.getSentimentoGeral())
                .resumo(a.getResumo())
                .personaIdentificada(a.getPersonaIdentificada())
                .budgetIdentificado(a.getBudgetIdentificado())
                .scoreConfiancaVendedor(a.getScoreConfiancaVendedor())
                .dataAnalise(a.getDataAnalise())
                .produtos(a.getProdutosIdentificados().stream()
                        .map(p -> ProdutoResponse.builder()
                                .id(p.getId())
                                .nomeProduto(p.getNomeProduto())
                                .tipo(p.getTipo())
                                .contexto(p.getContexto())
                                .build())
                        .toList())
                .oportunidades(a.getOportunidades().stream()
                        .map(o -> OportunidadeResponse.builder()
                                .id(o.getId())
                                .tipo(o.getTipo())
                                .descricao(o.getDescricao())
                                .produtoRelacionado(o.getProdutoRelacionado())
                                .score(o.getScore())
                                .valorEstimado(o.getValorEstimado())
                                .build())
                        .toList())
                .riscos(a.getRiscos().stream()
                        .map(r -> RiscoResponse.builder()
                                .id(r.getId())
                                .tipo(r.getTipo())
                                .descricao(r.getDescricao())
                                .severidade(r.getSeveridade())
                                .concorrenteMencionado(r.getConcorrenteMencionado())
                                .build())
                        .toList())
                .alertas(a.getAlertas().stream()
                        .map(al -> AlertaResponse.builder()
                                .id(al.getId())
                                .tipo(al.getTipo())
                                .mensagem(al.getMensagem())
                                .prioridade(al.getPrioridade())
                                .status(al.getStatus())
                                .dataCriacao(al.getDataCriacao())
                                .build())
                        .toList())
                .build();
    }
}
