package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.enums.Sentimento;
import br.com.fiap.challenge.model.AnaliseResultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnaliseResultadoRepository extends JpaRepository<AnaliseResultado, Long> {
    Optional<AnaliseResultado> findByTranscricaoId(Long transcricaoId);
    List<AnaliseResultado> findBySentimentoGeral(Sentimento sentimento);
    List<AnaliseResultado> findAllByOrderByDataAnaliseDesc();
}
