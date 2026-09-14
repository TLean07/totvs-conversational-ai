package com.tlean07.fiap.challenge.exception;

/**
 * Encapsula erros de acesso ao banco (SQLException) para que as camadas superiores
 * nao dependam da API JDBC.
 */
public class DAOException extends RuntimeException {

    public DAOException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public DAOException(String mensagem) {
        super(mensagem);
    }
}
