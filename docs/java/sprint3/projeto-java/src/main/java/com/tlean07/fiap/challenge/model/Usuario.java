package com.tlean07.fiap.challenge.model;

import com.tlean07.fiap.challenge.exception.RegraNegocioException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuario da plataforma (tabela APP_USERS).
 */
public class Usuario {

    private String id;
    private String email;
    private String nome;
    private String senhaHash;
    private Perfil perfil;
    private boolean ativo;
    private LocalDateTime criadoEm;

    public Usuario() {
        this.id = UUID.randomUUID().toString();
        this.perfil = Perfil.USER;
        this.ativo = true;
        this.criadoEm = LocalDateTime.now();
    }

    public Usuario(String email, String nome, String senhaHash, Perfil perfil) {
        this();
        this.email = email;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    /**
     * Regra de negocio: e-mail obrigatorio e em formato valido, nome com ao menos 3 caracteres.
     */
    public void validar() {
        if (email == null || !email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new RegraNegocioException("E-mail do usuario invalido: " + email);
        }
        if (nome == null || nome.trim().length() < 3) {
            throw new RegraNegocioException("Nome do usuario deve ter ao menos 3 caracteres");
        }
        if (perfil == null) {
            throw new RegraNegocioException("Perfil do usuario e obrigatorio");
        }
    }

    /**
     * Regra de negocio: administradores enxergam tudo; usuarios comuns so o que criaram.
     */
    public boolean podeAcessar(Transcricao transcricao) {
        if (!ativo) {
            return false;
        }
        if (perfil == Perfil.ADMIN) {
            return true;
        }
        return transcricao != null && Objects.equals(transcricao.getOwnerId(), id);
    }

    public boolean isAdministrador() {
        return perfil == Perfil.ADMIN;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public String toString() {
        return "Usuario{" + nome + " <" + email + "> " + perfil + (ativo ? "" : " (inativo)") + "}";
    }
}
