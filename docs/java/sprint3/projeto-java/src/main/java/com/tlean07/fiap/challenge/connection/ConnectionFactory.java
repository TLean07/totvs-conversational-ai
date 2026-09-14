package com.tlean07.fiap.challenge.connection;

import com.tlean07.fiap.challenge.exception.DAOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Camada de conexao com o Oracle Database 19c da FIAP.
 * Usuario e senha ficam no codigo, conforme solicitado na entrega da disciplina.
 */
public class ConnectionFactory {

    private static final String URL = "jdbc:oracle:thin:@//oracle.fiap.com.br:1521/orcl";
    private static final String USUARIO = "RM561344";
    private static final String SENHA = "SENHA_DO_RM";

    private ConnectionFactory() {
    }

    /** Abre uma nova conexao. Quem chama e responsavel por fechar (try-with-resources). */
    public static Connection getConnection() {
        try {
            Connection conexao = DriverManager.getConnection(URL, USUARIO, SENHA);
            conexao.setAutoCommit(false);
            return conexao;
        } catch (SQLException e) {
            throw new DAOException("Nao foi possivel conectar ao Oracle em " + URL + " com o usuario " + USUARIO, e);
        }
    }

    /** Testa a conexao executando SELECT 1 FROM DUAL. */
    public static boolean testarConexao() {
        try (Connection conexao = getConnection();
             var stmt = conexao.createStatement();
             var rs = stmt.executeQuery("SELECT 1 FROM DUAL")) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            throw new DAOException("Falha ao testar a conexao", e);
        }
    }

    public static String getUrl() {
        return URL;
    }

    public static String getUsuario() {
        return USUARIO;
    }
}
