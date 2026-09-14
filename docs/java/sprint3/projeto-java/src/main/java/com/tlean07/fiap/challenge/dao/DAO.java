package com.tlean07.fiap.challenge.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrato generico do CRUD seguido por todas as classes DAO.
 *
 * @param <T> tipo da entidade
 */
public interface DAO<T> {

    /** Create. */
    void inserir(T entidade);

    /** Read (um registro). */
    Optional<T> buscarPorId(String id);

    /** Read (todos). */
    List<T> listar();

    /** Update. */
    void atualizar(T entidade);

    /** Delete. */
    void excluir(String id);
}
