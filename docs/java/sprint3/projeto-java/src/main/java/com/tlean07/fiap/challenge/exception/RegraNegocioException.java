package com.tlean07.fiap.challenge.exception;

/**
 * Lancada quando uma regra de negocio e violada (ex.: transicao de status invalida,
 * transcricao sem texto, usuario inativo).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
