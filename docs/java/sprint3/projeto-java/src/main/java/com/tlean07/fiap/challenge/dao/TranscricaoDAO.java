package com.tlean07.fiap.challenge.dao;

import com.tlean07.fiap.challenge.connection.ConnectionFactory;
import com.tlean07.fiap.challenge.exception.DAOException;
import com.tlean07.fiap.challenge.model.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD da tabela TRANSCRICOES via JDBC.
 */
public class TranscricaoDAO implements DAO<Transcricao> {

    private static final String COLUNAS = "ID, OWNER_ID, TEXTO_ORIGINAL, PARTICIPANTES, DATA_REUNIAO, DURACAO_MINUTOS, CLIENTE, VENDEDOR, "
            + "ORIGEM_TRANSCRICAO, MEETING_ID, UF, NOME_SEGMENTO, NOTA_NPS, STATUS, DATA_CRIACAO, DATA_PROCESSAMENTO, ANALISE_ID";

    private static final String SQL_INSERT = "INSERT INTO TRANSCRICOES (" + COLUNAS + ", MISSING_MEETING_ID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT = "SELECT " + COLUNAS + " FROM TRANSCRICOES";
    private static final String SQL_UPDATE = "UPDATE TRANSCRICOES SET OWNER_ID = ?, TEXTO_ORIGINAL = ?, PARTICIPANTES = ?, DATA_REUNIAO = ?, "
            + "DURACAO_MINUTOS = ?, CLIENTE = ?, VENDEDOR = ?, ORIGEM_TRANSCRICAO = ?, MEETING_ID = ?, UF = ?, NOME_SEGMENTO = ?, NOTA_NPS = ?, "
            + "STATUS = ?, DATA_PROCESSAMENTO = ?, ANALISE_ID = ? WHERE ID = ?";
    private static final String SQL_DELETE = "DELETE FROM TRANSCRICOES WHERE ID = ?";

    @Override
    public void inserir(Transcricao t) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            ps.setString(1, t.getId());
            ps.setString(2, t.getOwnerId());
            ps.setString(3, t.getTextoOriginal());
            ps.setString(4, t.getParticipantes());
            setTimestamp(ps, 5, t.getDataReuniao());
            setInteger(ps, 6, t.getDuracaoMinutos());
            ps.setString(7, t.getCliente());
            ps.setString(8, t.getVendedor());
            ps.setString(9, t.getOrigemTranscricao());
            ps.setString(10, t.getMeetingId());
            ps.setString(11, t.getUf());
            ps.setString(12, t.getNomeSegmento());
            setInteger(ps, 13, t.getNotaNps());
            ps.setString(14, t.getStatus().name());
            setTimestamp(ps, 15, t.getDataCriacao());
            setTimestamp(ps, 16, t.getDataProcessamento());
            ps.setString(17, t.getAnalise() != null ? t.getAnalise().getId() : null);
            ps.setInt(18, t.getMeetingId() == null ? 1 : 0);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir transcricao " + t.getId(), e);
        }
    }

    @Override
    public Optional<Transcricao> buscarPorId(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " WHERE ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar transcricao " + id, e);
        }
    }

    @Override
    public List<Transcricao> listar() {
        return consultar(SQL_SELECT + " ORDER BY DATA_CRIACAO DESC FETCH FIRST 100 ROWS ONLY");
    }

    public List<Transcricao> listarPorCliente(String cliente) {
        List<Transcricao> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " WHERE UPPER(CLIENTE) LIKE ? ORDER BY DATA_CRIACAO DESC")) {
            ps.setString(1, "%" + cliente.toUpperCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar transcricoes do cliente " + cliente, e);
        }
        return lista;
    }

    public List<Transcricao> listarPorStatus(StatusTranscricao status) {
        List<Transcricao> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " WHERE STATUS = ? ORDER BY DATA_CRIACAO DESC FETCH FIRST 100 ROWS ONLY")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar transcricoes com status " + status, e);
        }
        return lista;
    }

    @Override
    public void atualizar(Transcricao t) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, t.getOwnerId());
            ps.setString(2, t.getTextoOriginal());
            ps.setString(3, t.getParticipantes());
            setTimestamp(ps, 4, t.getDataReuniao());
            setInteger(ps, 5, t.getDuracaoMinutos());
            ps.setString(6, t.getCliente());
            ps.setString(7, t.getVendedor());
            ps.setString(8, t.getOrigemTranscricao());
            ps.setString(9, t.getMeetingId());
            ps.setString(10, t.getUf());
            ps.setString(11, t.getNomeSegmento());
            setInteger(ps, 12, t.getNotaNps());
            ps.setString(13, t.getStatus().name());
            setTimestamp(ps, 14, t.getDataProcessamento());
            ps.setString(15, t.getAnalise() != null ? t.getAnalise().getId() : null);
            ps.setString(16, t.getId());
            int linhas = ps.executeUpdate();
            con.commit();
            if (linhas == 0) {
                throw new DAOException("Transcricao " + t.getId() + " nao encontrada para atualizacao");
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar transcricao " + t.getId(), e);
        }
    }

    @Override
    public void excluir(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setString(1, id);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao excluir transcricao " + id, e);
        }
    }

    public long contar() {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM TRANSCRICOES");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DAOException("Erro ao contar transcricoes", e);
        }
    }

    private List<Transcricao> consultar(String sql) {
        List<Transcricao> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar transcricoes", e);
        }
        return lista;
    }

    private Transcricao mapear(ResultSet rs) throws SQLException {
        Transcricao t = new Transcricao();
        t.setId(rs.getString("ID"));
        t.setOwnerId(rs.getString("OWNER_ID"));
        t.setTextoOriginal(rs.getString("TEXTO_ORIGINAL"));
        t.setParticipantes(rs.getString("PARTICIPANTES"));
        t.setDataReuniao(getLocalDateTime(rs, "DATA_REUNIAO"));
        t.setDuracaoMinutos(getInteger(rs, "DURACAO_MINUTOS"));
        t.setCliente(rs.getString("CLIENTE"));
        t.setVendedor(rs.getString("VENDEDOR"));
        t.setOrigemTranscricao(rs.getString("ORIGEM_TRANSCRICAO"));
        t.setMeetingId(rs.getString("MEETING_ID"));
        t.setUf(rs.getString("UF"));
        t.setNomeSegmento(rs.getString("NOME_SEGMENTO"));
        t.setNotaNps(getInteger(rs, "NOTA_NPS"));
        String status = rs.getString("STATUS");
        t.setStatus(status != null ? StatusTranscricao.valueOf(status) : StatusTranscricao.PENDENTE);
        t.setDataCriacao(getLocalDateTime(rs, "DATA_CRIACAO"));
        t.setDataProcessamento(getLocalDateTime(rs, "DATA_PROCESSAMENTO"));
        return t;
    }

    static void setTimestamp(PreparedStatement ps, int indice, java.time.LocalDateTime valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(indice, Timestamp.valueOf(valor));
        }
    }

    static void setInteger(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }

    static Integer getInteger(ResultSet rs, String coluna) throws SQLException {
        int v = rs.getInt(coluna);
        return rs.wasNull() ? null : v;
    }

    static java.time.LocalDateTime getLocalDateTime(ResultSet rs, String coluna) throws SQLException {
        Timestamp ts = rs.getTimestamp(coluna);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
