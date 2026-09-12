package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.config.CacheConfig.CacheNames;
import com.tlean07.fiap.challenge.dto.TranscricaoInsightsResponse;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.OportunidadeConsolidada;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.RiscoConsolidado;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Insights por transcricao para o dashboard, montados a partir de cinco consultas leves:
 * transcricoes (id, cliente) e as quatro tabelas filhas da analise (riscos, oportunidades e suas
 * versoes consolidadas). Nenhuma entidade completa e carregada.
 */
@Service
public class TranscricaoInsightsService {

    private final TranscricaoRepository transcricaoRepository;
    private final AnaliseResultadoRepository analiseRepository;

    public TranscricaoInsightsService(TranscricaoRepository transcricaoRepository,
                                      AnaliseResultadoRepository analiseRepository) {
        this.transcricaoRepository = transcricaoRepository;
        this.analiseRepository = analiseRepository;
    }

    @Cacheable(cacheNames = CacheNames.INSIGHTS, key = "@cacheKeys.user()")
    public List<TranscricaoInsightsResponse> listarTodos() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        boolean admin = user.isAdmin();
        String owner = user.id();

        List<TranscricaoRepository.InsightRow> transcricoes = admin
                ? transcricaoRepository.findInsightRows()
                : transcricaoRepository.findInsightRowsByOwnerId(owner);

        Map<String, List<Risco>> riscos = new HashMap<>();
        for (AnaliseResultadoRepository.RiscoRow r : admin ? analiseRepository.findRiscoRows() : analiseRepository.findRiscoRowsByOwnerId(owner)) {
            riscos.computeIfAbsent(r.getTranscricaoId(), k -> new ArrayList<>()).add(r.getRisco());
        }
        Map<String, List<Oportunidade>> oportunidades = new HashMap<>();
        for (AnaliseResultadoRepository.OportunidadeRow o : admin ? analiseRepository.findOportunidadeRows() : analiseRepository.findOportunidadeRowsByOwnerId(owner)) {
            oportunidades.computeIfAbsent(o.getTranscricaoId(), k -> new ArrayList<>()).add(o.getOportunidade());
        }
        Map<String, List<RiscoConsolidado>> riscosConsolidados = new HashMap<>();
        for (AnaliseResultadoRepository.RiscoConsolidadoRow r : admin ? analiseRepository.findRiscoConsolidadoRows() : analiseRepository.findRiscoConsolidadoRowsByOwnerId(owner)) {
            riscosConsolidados.computeIfAbsent(r.getTranscricaoId(), k -> new ArrayList<>()).add(r.getRisco());
        }
        Map<String, List<OportunidadeConsolidada>> oportunidadesConsolidadas = new HashMap<>();
        for (AnaliseResultadoRepository.OportunidadeConsolidadaRow o : admin ? analiseRepository.findOportunidadeConsolidadaRows() : analiseRepository.findOportunidadeConsolidadaRowsByOwnerId(owner)) {
            oportunidadesConsolidadas.computeIfAbsent(o.getTranscricaoId(), k -> new ArrayList<>()).add(o.getOportunidade());
        }

        List<TranscricaoInsightsResponse> resultado = new ArrayList<>(transcricoes.size());
        for (TranscricaoRepository.InsightRow row : transcricoes) {
            resultado.add(TranscricaoInsightsResponse.builder()
                    .id(row.getId())
                    .cliente(row.getCliente())
                    .riscosConsolidados(riscosConsolidados.getOrDefault(row.getId(), List.of()))
                    .oportunidadesConsolidadas(oportunidadesConsolidadas.getOrDefault(row.getId(), List.of()))
                    .riscos(riscos.getOrDefault(row.getId(), List.of()))
                    .oportunidades(oportunidades.getOrDefault(row.getId(), List.of()))
                    .build());
        }
        return resultado;
    }
}
