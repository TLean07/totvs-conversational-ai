package com.tlean07.fiap.challenge.dao;

import com.tlean07.fiap.challenge.connection.ConnectionFactory;
import com.tlean07.fiap.challenge.exception.DAOException;
import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.Severidade;
import com.tlean07.fiap.challenge.model.StatusAlerta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD da tabela ALERTAS via JDBC.
 */
public class AlertaDAO implements DAO<Alerta> {

    private static final String COLUNAS = "ID, OWNER_ID, ANALISE_ID, TIPO, TITULO, CATEGORIA, MENSAGEM, PRIORIDADE, STATUS, DATA_CRIACAO";
    private static final String SQL_INSERT = "INSERT INTO ALERTAS (" + COLUNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT = "SELECT " + COLUNAS + " FROM ALERTAS";
    private static final String SQL_UPDATE = "UPDATE ALERTAS SET TIPO = ?, TITULO = ?, CATEGORIA = ?, MENSAGEM = ?, PRIORIDADE = ?, STATUS = ? WHERE ID = ?";

    @Override
    public void inserir(Alerta a) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            ps.setString(1, a.getId());
            ps.setString(2, a.getOwnerId());
            ps.setString(3, a.getAnaliseId());
            ps.setString(4, a.getTipo());
            ps.setString(5, a.getTitulo());
            ps.setString(6, a.getCategoria());
            ps.setString(7, a.getMensagem());
            ps.setString(8, a.getPrioridade().name());
            ps.setString(9, a.getStatus().name());
            TranscricaoDAO.setTimestamp(ps, 10, a.getDataCriacao());
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir alerta " + a.getId(), e);
        }
    }

    /** Insere varios alertas de uma vez (mesma transacao). */
    public void inserirTodos(List<Alerta> alertas) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            for (Alerta a : alertas) {
                ps.setString(1, a.getId());
                ps.setString(2, a.getOwnerId());
                ps.setString(3, a.getAnaliseId());
                ps.setString(4, a.getTipo());
                ps.setString(5, a.getTitulo());
                ps.setString(6, a.getCategoria());
                ps.setString(7, a.getMensagem());
                ps.setString(8, a.getPrioridade().name());
                ps.setString(9, a.getStatus().name());
                TranscricaoDAO.setTimestamp(ps, 10, a.getDataCriacao());
                ps.addBatch();
            }
            ps.executeBatch();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir alertas em lote", e);
        }
    }

    @Override
    public Optional<Alerta> buscarPorId(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " WHERE ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar alerta " + id, e);
        }
    }

    @Override
    public List<Alerta> listar() {
        return consultar(SQL_SELECT + " ORDER BY DATA_CRIACAO DESC FETCH FIRST 100 ROWS ONLY", null);
    }

    public List<Alerta> listarPorStatus(StatusAlerta status) {
        return consultar(SQL_SELECT + " WHERE STATUS = ? ORDER BY PRIORIDADE, DATA_CRIACAO DESC FETCH FIRST 100 ROWS ONLY", status.name());
    }

    public List<Alerta> listarPorAnalise(String analiseId) {
        return consultar(SQL_SELECT + " WHERE ANALISE_ID = ? ORDER BY DATA_CRIACAO", analiseId);
    }

    @Override
    public void atualizar(Alerta a) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, a.getTipo());
            ps.setString(2, a.getTitulo());
            ps.setString(3, a.getCategoria());
            ps.setString(4, a.getMensagem());
            ps.setString(5, a.getPrioridade().name());
            ps.setString(6, a.getStatus().name());
            ps.setString(7, a.getId());
            int linhas = ps.executeUpdate();
            con.commit();
            if (linhas == 0) {
                throw new DAOException("Alerta " + a.getId() + " nao encontrado para atualizacao");
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar alerta " + a.getId(), e);
        }
    }

    @Override
    public void excluir(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ALERTAS WHERE ID = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao excluir alerta " + id, e);
        }
    }

    private List<Alerta> consultar(String sql, String parametro) {
        List<Alerta> lista = new ArrayList<>();
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
            throw new DAOException("Erro ao listar alertas", e);
        }
        return lista;
    }

    private Alerta mapear(ResultSet rs) throws SQLException {
        Alerta a = new Alerta();
        a.setId(rs.getString("ID"));
        a.setOwnerId(rs.getString("OWNER_ID"));
        a.setAnaliseId(rs.getString("ANALISE_ID"));
        a.setTipo(rs.getString("TIPO"));
        a.setTitulo(rs.getString("TITULO"));
        a.setCategoria(rs.getString("CATEGORIA"));
        a.setMensagem(rs.getString("MENSAGEM"));
        String prioridade = rs.getString("PRIORIDADE");
        a.setPrioridade(prioridade != null ? Severidade.valueOf(prioridade) : Severidade.MEDIO);
        String status = rs.getString("STATUS");
        a.setStatus(status != null ? StatusAlerta.valueOf(status) : StatusAlerta.NOVO);
        a.setDataCriacao(TranscricaoDAO.getLocalDateTime(rs, "DATA_CRIACAO"));
        return a;
    }
}
