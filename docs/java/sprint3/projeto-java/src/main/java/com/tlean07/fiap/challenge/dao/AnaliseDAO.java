package com.tlean07.fiap.challenge.dao;

import com.tlean07.fiap.challenge.connection.ConnectionFactory;
import com.tlean07.fiap.challenge.exception.DAOException;
import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Insight;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.Produto;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.Sentimento;
import com.tlean07.fiap.challenge.model.Severidade;
import com.tlean07.fiap.challenge.model.TipoOportunidade;
import com.tlean07.fiap.challenge.model.TipoProduto;
import com.tlean07.fiap.challenge.model.TipoRisco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD da tabela ANALISES e das tabelas filhas ANALISE_PRODUTOS, ANALISE_OPORTUNIDADES e ANALISE_RISCOS.
 * Insercao e atualizacao acontecem em uma unica transacao (tudo ou nada).
 */
public class AnaliseDAO implements DAO<Analise> {

    private static final String SQL_INSERT = "INSERT INTO ANALISES (ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, "
            + "BUDGET_IDENTIFICADO, SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, DATA_ANALISE, PROXIMA_ACAO) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT = "SELECT ID, OWNER_ID, TRANSCRICAO_ID, SENTIMENTO_GERAL, RESUMO, PERSONA_IDENTIFICADA, BUDGET_IDENTIFICADO, "
            + "SCORE_CONFIANCA_VENDEDOR, SCORE_SENTIMENTO, DATA_ANALISE, PROXIMA_ACAO FROM ANALISES";
    private static final String SQL_UPDATE = "UPDATE ANALISES SET SENTIMENTO_GERAL = ?, RESUMO = ?, PERSONA_IDENTIFICADA = ?, BUDGET_IDENTIFICADO = ?, "
            + "SCORE_CONFIANCA_VENDEDOR = ?, SCORE_SENTIMENTO = ?, PROXIMA_ACAO = ? WHERE ID = ?";
    private static final String SQL_PRODUTO = "INSERT INTO ANALISE_PRODUTOS (ANALISE_ID, POSICAO, NOME_PRODUTO, TIPO, CONTEXTO) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_OPORT = "INSERT INTO ANALISE_OPORTUNIDADES (ANALISE_ID, POSICAO, TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_RISCO = "INSERT INTO ANALISE_RISCOS (ANALISE_ID, POSICAO, TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO) VALUES (?, ?, ?, ?, ?, ?)";

    @Override
    public void inserir(Analise a) {
        try (Connection con = ConnectionFactory.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
                ps.setString(1, a.getId());
                ps.setString(2, a.getOwnerId());
                ps.setString(3, a.getTranscricaoId());
                ps.setString(4, a.getSentimentoGeral().name());
                ps.setString(5, a.getResumo());
                ps.setString(6, a.getPersonaIdentificada());
                ps.setString(7, a.getBudgetIdentificado());
                ps.setDouble(8, a.getScoreConfiancaVendedor());
                ps.setDouble(9, a.getScoreSentimento());
                TranscricaoDAO.setTimestamp(ps, 10, a.getDataAnalise());
                ps.setString(11, a.getProximaAcao());
                ps.executeUpdate();
            }
            inserirFilhas(con, a);
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir analise " + a.getId(), e);
        }
    }

    private void inserirFilhas(Connection con, Analise a) throws SQLException {
        int pos = 0;
        try (PreparedStatement ps = con.prepareStatement(SQL_PRODUTO)) {
            for (Produto p : a.getProdutos()) {
                ps.setString(1, a.getId());
                ps.setInt(2, pos++);
                ps.setString(3, p.getNome());
                ps.setString(4, p.getTipo().name());
                ps.setString(5, p.getContexto());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        pos = 0;
        try (PreparedStatement ps = con.prepareStatement(SQL_OPORT)) {
            for (Oportunidade o : a.getOportunidades()) {
                ps.setString(1, a.getId());
                ps.setInt(2, pos++);
                ps.setString(3, o.getTipo().name());
                ps.setString(4, o.getDescricao());
                ps.setString(5, o.getProdutoRelacionado());
                ps.setDouble(6, o.getScore());
                ps.setString(7, o.getValorEstimado());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        pos = 0;
        try (PreparedStatement ps = con.prepareStatement(SQL_RISCO)) {
            for (Risco r : a.getRiscos()) {
                ps.setString(1, a.getId());
                ps.setInt(2, pos++);
                ps.setString(3, r.getTipo().name());
                ps.setString(4, r.getDescricao());
                ps.setString(5, r.getSeveridade().name());
                ps.setString(6, r.getConcorrenteMencionado());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void excluirFilhas(Connection con, String analiseId) throws SQLException {
        for (String tabela : new String[]{"ANALISE_PRODUTOS", "ANALISE_OPORTUNIDADES", "ANALISE_RISCOS"}) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM " + tabela + " WHERE ANALISE_ID = ?")) {
                ps.setString(1, analiseId);
                ps.executeUpdate();
            }
        }
    }

    @Override
    public Optional<Analise> buscarPorId(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " WHERE ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Analise a = mapear(rs);
                carregarFilhas(con, a);
                return Optional.of(a);
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar analise " + id, e);
        }
    }

    public Optional<Analise> buscarPorTranscricao(String transcricaoId) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " WHERE TRANSCRICAO_ID = ?")) {
            ps.setString(1, transcricaoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Analise a = mapear(rs);
                carregarFilhas(con, a);
                return Optional.of(a);
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar analise da transcricao " + transcricaoId, e);
        }
    }

    @Override
    public List<Analise> listar() {
        List<Analise> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " ORDER BY DATA_ANALISE DESC FETCH FIRST 50 ROWS ONLY");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            for (Analise a : lista) {
                carregarFilhas(con, a);
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar analises", e);
        }
        return lista;
    }

    @Override
    public void atualizar(Analise a) {
        try (Connection con = ConnectionFactory.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
                ps.setString(1, a.getSentimentoGeral().name());
                ps.setString(2, a.getResumo());
                ps.setString(3, a.getPersonaIdentificada());
                ps.setString(4, a.getBudgetIdentificado());
                ps.setDouble(5, a.getScoreConfiancaVendedor());
                ps.setDouble(6, a.getScoreSentimento());
                ps.setString(7, a.getProximaAcao());
                ps.setString(8, a.getId());
                if (ps.executeUpdate() == 0) {
                    throw new DAOException("Analise " + a.getId() + " nao encontrada para atualizacao");
                }
            }
            excluirFilhas(con, a.getId());
            inserirFilhas(con, a);
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar analise " + a.getId(), e);
        }
    }

    @Override
    public void excluir(String id) {
        // as tabelas filhas e os alertas saem em cascata (ON DELETE CASCADE); TRANSCRICOES.ANALISE_ID vira NULL
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ANALISES WHERE ID = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao excluir analise " + id, e);
        }
    }

    private void carregarFilhas(Connection con, Analise a) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT NOME_PRODUTO, TIPO, CONTEXTO FROM ANALISE_PRODUTOS WHERE ANALISE_ID = ? ORDER BY POSICAO")) {
            ps.setString(1, a.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    a.getProdutos().add(new Produto(rs.getString(1), TipoProduto.valueOf(rs.getString(2)), rs.getString(3)));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT TIPO, DESCRICAO, PRODUTO_RELACIONADO, SCORE, VALOR_ESTIMADO FROM ANALISE_OPORTUNIDADES WHERE ANALISE_ID = ? ORDER BY POSICAO")) {
            ps.setString(1, a.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // a evidencia nao e persistida em coluna propria: a descricao gravada ja contem o trecho
                    a.getInsights().add(new Oportunidade(TipoOportunidade.valueOf(rs.getString(1)), rs.getString(2), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getString(5)));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT TIPO, DESCRICAO, SEVERIDADE, CONCORRENTE_MENCIONADO FROM ANALISE_RISCOS WHERE ANALISE_ID = ? ORDER BY POSICAO")) {
            ps.setString(1, a.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    a.getInsights().add(new Risco(TipoRisco.valueOf(rs.getString(1)), rs.getString(2), rs.getString(2), Severidade.valueOf(rs.getString(3)), rs.getString(4)));
                }
            }
        }
    }

    private Analise mapear(ResultSet rs) throws SQLException {
        Analise a = new Analise();
        a.setId(rs.getString("ID"));
        a.setOwnerId(rs.getString("OWNER_ID"));
        a.setTranscricaoId(rs.getString("TRANSCRICAO_ID"));
        String sentimento = rs.getString("SENTIMENTO_GERAL");
        a.setSentimentoGeral(sentimento != null ? Sentimento.valueOf(sentimento) : Sentimento.NEUTRO);
        a.setResumo(rs.getString("RESUMO"));
        a.setPersonaIdentificada(rs.getString("PERSONA_IDENTIFICADA"));
        a.setBudgetIdentificado(rs.getString("BUDGET_IDENTIFICADO"));
        a.setScoreConfiancaVendedor(rs.getDouble("SCORE_CONFIANCA_VENDEDOR"));
        a.setScoreSentimento(rs.getDouble("SCORE_SENTIMENTO"));
        a.setDataAnalise(TranscricaoDAO.getLocalDateTime(rs, "DATA_ANALISE"));
        a.setProximaAcao(rs.getString("PROXIMA_ACAO"));
        return a;
    }
}
