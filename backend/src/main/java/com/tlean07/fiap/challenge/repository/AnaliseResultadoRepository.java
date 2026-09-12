package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.Sentimento;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnaliseResultadoRepository extends JpaRepository<AnaliseResultado, String> {

    Optional<AnaliseResultado> findByTranscricaoId(String transcricaoId);
    Optional<AnaliseResultado> findByTranscricaoIdAndOwnerId(String transcricaoId, String ownerId);

    @Transactional
    void deleteByTranscricaoId(String transcricaoId);

    List<AnaliseResultado> findBySentimentoGeral(Sentimento sentimento);
    List<AnaliseResultado> findAllByOrderByDataAnaliseDesc();
    List<AnaliseResultado> findAllByOwnerIdOrderByDataAnaliseDesc(String ownerId);
    List<AnaliseResultado> findTop100ByOwnerIdOrderByDataAnaliseDesc(String ownerId);

    /*
     * No MongoDB estas consultas projetavam apenas alguns campos. No Oracle as listas
     * ficam em tabelas filhas carregadas em lote, entao a consulta base e a mesma.
     */
    @Query("select a from AnaliseResultado a")
    List<AnaliseResultado> findAllForDashboard();

    @Query("select a from AnaliseResultado a where a.ownerId = :ownerId")
    List<AnaliseResultado> findAllByOwnerIdForDashboard(@Param("ownerId") String ownerId);

    @Query("select a from AnaliseResultado a order by a.dataAnalise desc")
    List<AnaliseResultado> findAllForInsights();

    @Query("select a from AnaliseResultado a where a.ownerId = :ownerId order by a.dataAnalise desc")
    List<AnaliseResultado> findAllByOwnerIdForInsights(@Param("ownerId") String ownerId);
}
