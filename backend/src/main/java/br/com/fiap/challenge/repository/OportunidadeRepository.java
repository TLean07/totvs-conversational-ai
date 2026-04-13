package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.enums.TipoOportunidade;
import br.com.fiap.challenge.model.Oportunidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long> {
    List<Oportunidade> findByTipo(TipoOportunidade tipo);
    List<Oportunidade> findByScoreGreaterThanEqual(Double score);
}
