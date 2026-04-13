package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.enums.Severidade;
import br.com.fiap.challenge.enums.StatusAlerta;
import br.com.fiap.challenge.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByStatus(StatusAlerta status);
    List<Alerta> findByPrioridade(Severidade prioridade);
    List<Alerta> findByStatusOrderByPrioridadeAscDataCriacaoDesc(StatusAlerta status);
    long countByStatus(StatusAlerta status);
}
