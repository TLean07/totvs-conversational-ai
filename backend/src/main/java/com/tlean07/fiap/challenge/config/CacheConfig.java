package com.tlean07.fiap.challenge.config;

import com.tlean07.fiap.challenge.security.AuthenticatedUser;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Cache de leitura em memoria (Caffeine, configurado em application.properties).
 *
 * Objetivo: o dashboard, as listas e os alertas sao consultados repetidamente pelo web e pelo
 * mobile; cada consulta fazia varias idas ao Oracle da FIAP (limite de 20 sessoes). Com o cache,
 * dentro da janela de TTL o banco so e consultado uma vez por usuario/consulta.
 *
 * Regras:
 * - Toda chave inclui o usuario autenticado (id + papel), porque as consultas filtram por dono.
 * - Toda escrita limpa os caches de dados ({@link CacheNames#DATA}); a analise assincrona
 *   tambem limpa ao terminar. Assim o TTL e apenas uma rede de seguranca.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final class CacheNames {
        public static final String DASHBOARD = "dashboard";
        public static final String TRANSCRICOES = "transcricoes";
        public static final String TRANSCRICAO = "transcricao";
        public static final String INSIGHTS = "insights";
        public static final String ALERTAS = "alertas";
        public static final String USUARIOS = "usuarios";
        public static final String LEANIA_CONTEXTO = "leania-contexto";

        /** Caches que dependem de transcricoes/analises/alertas e caem em qualquer escrita. */
        public static final String[] DATA = {DASHBOARD, TRANSCRICOES, TRANSCRICAO, INSIGHTS, ALERTAS, LEANIA_CONTEXTO};

        private CacheNames() {
        }
    }

    /** Usado nas expressoes SpEL das anotacoes: {@code key = "@cacheKeys.user()"}. */
    @Component("cacheKeys")
    public static class CacheKeys {
        public String user() {
            AuthenticatedUser user = SecurityUtils.currentUser();
            return user.id() + ":" + user.role();
        }
    }
}
