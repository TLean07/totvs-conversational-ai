package com.tlean07.fiap.challenge.repository;

import com.tlean07.fiap.challenge.enums.Sentimento;
import com.tlean07.fiap.challenge.model.AnaliseResultado;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnaliseResultadoRepository extends MongoRepository<AnaliseResultado, String> {
    Optional<AnaliseResultado> findByTranscricaoId(String transcricaoId);
    Optional<AnaliseResultado> findByTranscricaoIdAndOwnerId(String transcricaoId, String ownerId);
    void deleteByTranscricaoId(String transcricaoId);
    List<AnaliseResultado> findBySentimentoGeral(Sentimento sentimento);
    List<AnaliseResultado> findAllByOrderByDataAnaliseDesc();
    List<AnaliseResultado> findAllByOwnerIdOrderByDataAnaliseDesc(String ownerId);
}
