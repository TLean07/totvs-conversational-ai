package com.tlean07.fiap.challenge.dao;

import com.tlean07.fiap.challenge.connection.ConnectionFactory;
import com.tlean07.fiap.challenge.exception.DAOException;
import com.tlean07.fiap.challenge.model.Perfil;
import com.tlean07.fiap.challenge.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD da tabela APP_USERS via JDBC.
 */
public class UsuarioDAO implements DAO<Usuario> {

    private static final String COLUNAS = "ID, EMAIL, USER_NAME, PASSWORD_HASH, USER_ROLE, IS_ACTIVE, CREATED_AT";
    private static final String SQL_INSERT = "INSERT INTO APP_USERS (" + COLUNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT = "SELECT " + COLUNAS + " FROM APP_USERS";
    private static final String SQL_UPDATE = "UPDATE APP_USERS SET EMAIL = ?, USER_NAME = ?, PASSWORD_HASH = ?, USER_ROLE = ?, IS_ACTIVE = ? WHERE ID = ?";

    @Override
    public void inserir(Usuario u) {
        u.validar();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_INSERT)) {
            ps.setString(1, u.getId());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getNome());
            ps.setString(4, u.getSenhaHash());
            ps.setString(5, u.getPerfil().name());
            ps.setInt(6, u.isAtivo() ? 1 : 0);
            TranscricaoDAO.setTimestamp(ps, 7, u.getCriadoEm());
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao inserir usuario " + u.getEmail(), e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        return consultarUm(SQL_SELECT + " WHERE ID = ?", id);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return consultarUm(SQL_SELECT + " WHERE LOWER(EMAIL) = ?", email.toLowerCase());
    }

    @Override
    public List<Usuario> listar() {
        List<Usuario> lista = new ArrayList<>();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_SELECT + " ORDER BY USER_NAME");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar usuarios", e);
        }
        return lista;
    }

    @Override
    public void atualizar(Usuario u) {
        u.validar();
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getNome());
            ps.setString(3, u.getSenhaHash());
            ps.setString(4, u.getPerfil().name());
            ps.setInt(5, u.isAtivo() ? 1 : 0);
            ps.setString(6, u.getId());
            int linhas = ps.executeUpdate();
            con.commit();
            if (linhas == 0) {
                throw new DAOException("Usuario " + u.getId() + " nao encontrado para atualizacao");
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar usuario " + u.getId(), e);
        }
    }

    @Override
    public void excluir(String id) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM APP_USERS WHERE ID = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            throw new DAOException("Erro ao excluir usuario " + id, e);
        }
    }

    private Optional<Usuario> consultarUm(String sql, String parametro) {
        try (Connection con = ConnectionFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao consultar usuario " + parametro, e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getString("ID"));
        u.setEmail(rs.getString("EMAIL"));
        u.setNome(rs.getString("USER_NAME"));
        u.setSenhaHash(rs.getString("PASSWORD_HASH"));
        String perfil = rs.getString("USER_ROLE");
        u.setPerfil(perfil != null ? Perfil.valueOf(perfil) : Perfil.USER);
        u.setAtivo(rs.getInt("IS_ACTIVE") == 1);
        u.setCriadoEm(TranscricaoDAO.getLocalDateTime(rs, "CREATED_AT"));
        return u;
    }
}
