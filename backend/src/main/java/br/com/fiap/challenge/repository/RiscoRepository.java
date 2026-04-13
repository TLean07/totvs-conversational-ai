package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.enums.Severidade;
import br.com.fiap.challenge.enums.TipoRisco;
import br.com.fiap.challenge.model.Risco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiscoRepository extends JpaRepository<Risco, Long> {
    List<Risco> findByTipo(TipoRisco tipo);
    List<Risco> findBySeveridade(Severidade severidade);
    List<Risco> findByConcorrenteMencionadoIsNotNull();
}
