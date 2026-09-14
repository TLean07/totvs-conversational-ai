package com.tlean07.fiap.challenge.dao;

import com.tlean07.fiap.challenge.connection.ConnectionFactory;
import com.tlean07.fiap.challenge.exception.DAOException;
import com.tlean07.fiap.challenge.model.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD da tabela ANALISE_FEEDBACKS via JDBC.
 */
public class FeedbackDAO implements DAO<Feedback> {

    private static final String COLUNAS = "ID, OWNER_ID, TRANSCRICAO_ID, ANALISE_ID, UTIL, AJUSTE, CREATED_AT";
    private static final String SQL_INSERT = "INSERT INTO ANALISE_FEEDBACKS (" + COLUNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT = "SELECT " + COLUNAS + " FROM ANALISE_FEEDBACKS";

    @Override
    public void inserir(Feedback f) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            ps.setString(1, f.getId());
            ps.setString(2, f.getOwnerId());
            ps.setString(3, f.getTranscricaoId());
            ps.setString(4, f.getAnaliseId());
            ps.setInt(5, f.isUtil() ? 1 : 0);
            ps.setString(6, f.getAjuste());
            TranscricaoDAO.setTimestamp(ps, 7, f.getCriadoEm());
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir feedback " + f.getId(), e);
        }
    }

    @Override
    public Optional<Feedback> buscarPorId(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " WHERE ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar feedback " + id, e);
        }
    }

    @Override
    public List<Feedback> listar() {
        return consultar(SQL_SELECT + " ORDER BY CREATED_AT DESC FETCH FIRST 100 ROWS ONLY", null);
    }

    public List<Feedback> listarPorTranscricao(String transcricaoId) {
        return consultar(SQL_SELECT + " WHERE TRANSCRICAO_ID = ? ORDER BY CREATED_AT", transcricaoId);
    }

    @Override
    public void atualizar(Feedback f) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE ANALISE_FEEDBACKS SET UTIL = ?, AJUSTE = ? WHERE ID = ?")) {
            ps.setInt(1, f.isUtil() ? 1 : 0);
            ps.setString(2, f.getAjuste());
            ps.setString(3, f.getId());
            int linhas = ps.executeUpdate();
            con.commit();
            if (linhas == 0) {
                throw new DAOException("Feedback " + f.getId() + " nao encontrado para atualizacao");
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar feedback " + f.getId(), e);
        }
    }

    @Override
    public void excluir(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ANALISE_FEEDBACKS WHERE ID = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao excluir feedback " + id, e);
        }
    }

    private List<Feedback> consultar(String sql, String parametro) {
        List<Feedback> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setString(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar feedbacks", e);
        }
        return lista;
    }

    private Feedback mapear(ResultSet rs) throws SQLException {
        Feedback f = new Feedback();
        f.setId(rs.getString("ID"));
        f.setOwnerId(rs.getString("OWNER_ID"));
        f.setTranscricaoId(rs.getString("TRANSCRICAO_ID"));
        f.setAnaliseId(rs.getString("ANALISE_ID"));
        f.setUtil(rs.getInt("UTIL") == 1);
        f.setAjuste(rs.getString("AJUSTE"));
        f.setCriadoEm(TranscricaoDAO.getLocalDateTime(rs, "CREATED_AT"));
        return f;
    }
}
