package br.com.fiap.challenge.repository;

import br.com.fiap.challenge.enums.StatusTranscricao;
import br.com.fiap.challenge.model.Transcricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscricaoRepository extends JpaRepository<Transcricao, Long> {
    List<Transcricao> findByStatus(StatusTranscricao status);
    List<Transcricao> findByClienteContainingIgnoreCase(String cliente);
    List<Transcricao> findAllByOrderByDataCriacaoDesc();
}
