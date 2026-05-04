package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.dto.LeanIAContextDTO;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import com.tlean07.fiap.challenge.model.AnaliseFeedback;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.ProdutoIdentificado;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.repository.AnaliseFeedbackRepository;
import com.tlean07.fiap.challenge.repository.AnaliseResultadoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeanIALearningService {

    private static final int MAX_CONTEXT_ITEMS = 8;
    private static final int MAX_ANALYSES = 100;

    private final AnaliseResultadoRepository analiseRepository;
    private final AnaliseFeedbackRepository feedbackRepository;

    public LeanIALearningService(AnaliseResultadoRepository analiseRepository,
                                 AnaliseFeedbackRepository feedbackRepository) {
        this.analiseRepository = analiseRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public LeanIAContextDTO buildContext(String ownerId) {
        List<AnaliseResultado> historico = analiseRepository.findAllByOwnerIdOrderByDataAnaliseDesc(ownerId)
                .stream()
                .limit(MAX_ANALYSES)
                .toList();

        Map<String, Integer> produtos = new HashMap<>();
        Map<String, Integer> concorrentes = new HashMap<>();
        Map<String, Integer> riscos = new HashMap<>();
        Map<String, Integer> oportunidades = new HashMap<>();

        for (AnaliseResultado analise : historico) {
            for (ProdutoIdentificado produto : analise.getProdutosIdentificados()) {
                increment(produtos, produto.getNomeProduto());
            }
            for (Risco risco : analise.getRiscos()) {
                increment(riscos, risco.getTipo().name());
                increment(concorrentes, risco.getConcorrenteMencionado());
            }
            for (Oportunidade oportunidade : analise.getOportunidades()) {
                increment(oportunidades, oportunidade.getProdutoRelacionado());
                increment(oportunidades, oportunidade.getTipo().name());
            }
        }

        return LeanIAContextDTO.builder()
                .produtosFrequentes(top(produtos))
                .concorrentesFrequentes(top(concorrentes))
                .riscosFrequentes(top(riscos))
                .oportunidadesFrequentes(top(oportunidades))
                .ajustesRecentes(feedbackRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                        .stream()
                        .filter(feedback -> !feedback.isUtil())
                        .map(AnaliseFeedback::getAjuste)
                        .filter(ajuste -> ajuste != null && !ajuste.isBlank())
                        .limit(MAX_CONTEXT_ITEMS)
                        .toList())
                .build();
    }

    private void increment(Map<String, Integer> values, String key) {
        if (key == null || key.isBlank() || "Nao identificado".equalsIgnoreCase(key) || "A definir".equalsIgnoreCase(key)) {
            return;
        }
        String normalized = key.trim();
        values.merge(normalized, 1, Integer::sum);
    }

    private List<String> top(Map<String, Integer> values) {
        return values.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_CONTEXT_ITEMS)
                .map(Map.Entry::getKey)
                .toList();
    }
}
