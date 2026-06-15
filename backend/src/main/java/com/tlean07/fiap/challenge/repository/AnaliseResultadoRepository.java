package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.Sentimento;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnaliseResultadoRepository extends MongoRepository<AnaliseResultado, String> {
    String DASHBOARD_FIELDS = "{ 'sentimentoGeral': 1, 'metricasAgregadas': 1, 'riscos': 1, "
            + "'oportunidades': 1, 'riscosConsolidados': 1, 'oportunidadesConsolidadas': 1 }";
    String INSIGHT_FIELDS = "{ 'transcricaoId': 1, 'riscos': 1, 'oportunidades': 1, "
            + "'riscosConsolidados': 1, 'oportunidadesConsolidadas': 1 }";

    Optional<AnaliseResultado> findByTranscricaoId(String transcricaoId);
    Optional<AnaliseResultado> findByTranscricaoIdAndOwnerId(String transcricaoId, String ownerId);
    void deleteByTranscricaoId(String transcricaoId);
    List<AnaliseResultado> findBySentimentoGeral(Sentimento sentimento);
    List<AnaliseResultado> findAllByOrderByDataAnaliseDesc();
    List<AnaliseResultado> findAllByOwnerIdOrderByDataAnaliseDesc(String ownerId);

    @Query(value = "{}", fields = DASHBOARD_FIELDS)
    List<AnaliseResultado> findAllForDashboard();

    @Query(value = "{ 'ownerId': ?0 }", fields = DASHBOARD_FIELDS)
    List<AnaliseResultado> findAllByOwnerIdForDashboard(String ownerId);

    @Query(value = "{}", fields = INSIGHT_FIELDS)
    List<AnaliseResultado> findAllForInsights();

    @Query(value = "{ 'ownerId': ?0 }", fields = INSIGHT_FIELDS)
    List<AnaliseResultado> findAllByOwnerIdForInsights(String ownerId);
}
