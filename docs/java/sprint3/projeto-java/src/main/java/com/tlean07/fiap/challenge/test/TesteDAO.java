package com.tlean07.fiap.challenge.test;

import com.tlean07.fiap.challenge.connection.ConnectionFactory;
import com.tlean07.fiap.challenge.dao.AlertaDAO;
import com.tlean07.fiap.challenge.dao.AnaliseDAO;
import com.tlean07.fiap.challenge.dao.FeedbackDAO;
import com.tlean07.fiap.challenge.dao.TranscricaoDAO;
import com.tlean07.fiap.challenge.dao.UsuarioDAO;
import com.tlean07.fiap.challenge.model.Alerta;
import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Feedback;
import com.tlean07.fiap.challenge.model.Perfil;
import com.tlean07.fiap.challenge.model.StatusAlerta;
import com.tlean07.fiap.challenge.model.StatusTranscricao;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.model.Usuario;
import com.tlean07.fiap.challenge.service.TranscricaoService;

import java.util.List;
import java.util.Optional;

/**
 * Classe de teste do CRUD contra o Oracle da FIAP: cria um usuario e uma reuniao de teste,
 * processa (analise + alertas), le, atualiza, e ao final exclui tudo o que criou.
 */
public class TesteDAO {

    public static void main(String[] args) {
        System.out.println("=== TOTVS Inteligencia Conversacional - Teste do CRUD (JDBC / Oracle) ===\n");
        System.out.println("Conexao: " + ConnectionFactory.getUrl() + " como " + ConnectionFactory.getUsuario());
        System.out.println("SELECT 1 FROM DUAL -> " + (ConnectionFactory.testarConexao() ? "OK" : "FALHOU"));

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        TranscricaoDAO transcricaoDAO = new TranscricaoDAO();
        AnaliseDAO analiseDAO = new AnaliseDAO();
        AlertaDAO alertaDAO = new AlertaDAO();
        FeedbackDAO feedbackDAO = new FeedbackDAO();
        TranscricaoService service = new TranscricaoService();

        String sufixo = String.valueOf(System.currentTimeMillis());
        Usuario usuario = new Usuario("teste.java." + sufixo + "@company404.com.br", "Teste Java Sprint 3", "$2b$10$hash-de-teste", Perfil.USER);
        Transcricao reuniao = null;
        try {
            // CREATE
            System.out.println("\n-- CREATE");
            usuarioDAO.inserir(usuario);
            System.out.println("   usuario inserido: " + usuario);
            reuniao = new Transcricao("Clinica Vida Plena (teste Java)", "Rafael Moura",
                    "[LOCUTOR 2]: Estamos ha tres semanas com o chamado do RM aberto e ninguem retorna. "
                    + "[LOCUTOR 1]: Entendo a frustracao, vou escalar hoje. "
                    + "[LOCUTOR 2]: A Senior ja nos apresentou uma proposta e a diretoria esta pensando em trocar. "
                    + "Se nao resolver ate o fim do mes, vamos cancelar o contrato.");
            reuniao.setOwnerId(usuario.getId());
            reuniao.setMeetingId("JAVA-TESTE-" + sufixo);
            reuniao.setUf("SP");
            reuniao.setNomeSegmento("SAUDE");
            reuniao.setNotaNps(3);
            reuniao.setDuracaoMinutos(38);
            service.cadastrar(reuniao);
            System.out.println("   transcricao inserida: " + reuniao.getId() + " (" + reuniao.getStatus() + ")");

            // processamento completo: analise + alertas gravados pelos DAOs
            Analise analise = service.processar(reuniao);
            System.out.println("   analise inserida: " + analise);
            List<Alerta> alertas = alertaDAO.listarPorAnalise(analise.getId());
            System.out.println("   alertas inseridos: " + alertas.size());
            alertas.forEach(a -> System.out.println("     - " + a));

            Feedback feedback = new Feedback(usuario.getId(), reuniao.getId(), analise.getId(), true, "Risco de churn confirmado com o cliente.");
            feedbackDAO.inserir(feedback);
            System.out.println("   feedback inserido: " + feedback);

            // READ
            System.out.println("\n-- READ");
            Optional<Transcricao> lida = transcricaoDAO.buscarPorId(reuniao.getId());
            System.out.println("   buscarPorId(transcricao): " + lida.map(Transcricao::toString).orElse("nao encontrada"));
            Optional<Analise> analiseLida = analiseDAO.buscarPorTranscricao(reuniao.getId());
            System.out.println("   buscarPorTranscricao(analise): " + analiseLida.map(Analise::toString).orElse("nao encontrada"));
            analiseLida.ifPresent(a -> a.getInsights().forEach(i -> System.out.println("     - " + i)));
            System.out.println("   listarPorCliente('Vida Plena'): " + transcricaoDAO.listarPorCliente("Vida Plena").size() + " registro(s)");
            System.out.println("   buscarPorEmail(usuario): " + usuarioDAO.buscarPorEmail(usuario.getEmail()).map(Usuario::getNome).orElse("nao encontrado"));
            System.out.println("   listarPorTranscricao(feedback): " + feedbackDAO.listarPorTranscricao(reuniao.getId()).size() + " registro(s)");
            System.out.println("   total de transcricoes no banco: " + transcricaoDAO.contar());

            // UPDATE
            System.out.println("\n-- UPDATE");
            Transcricao paraAtualizar = lida.orElseThrow();
            paraAtualizar.setNotaNps(9);
            paraAtualizar.setCliente("Clinica Vida Plena (teste Java, atualizada)");
            paraAtualizar.setAnalise(analise);
            transcricaoDAO.atualizar(paraAtualizar);
            System.out.println("   transcricao atualizada: " + transcricaoDAO.buscarPorId(reuniao.getId()).map(t -> t.getCliente() + " / NPS " + t.getNotaNps()).orElse("?"));

            if (!alertas.isEmpty()) {
                Alerta alerta = alertas.get(0);
                alerta.marcarVisto();
                alertaDAO.atualizar(alerta);
                alerta.resolver();
                alertaDAO.atualizar(alerta);
                System.out.println("   alerta atualizado: " + alertaDAO.buscarPorId(alerta.getId()).map(Alerta::getStatus).orElse(null)
                        + " (esperado " + StatusAlerta.RESOLVIDO + ")");
            }
            feedback.setUtil(false);
            feedback.setAjuste("Severidade deveria ser MEDIO.");
            feedbackDAO.atualizar(feedback);
            System.out.println("   feedback atualizado: " + feedbackDAO.buscarPorId(feedback.getId()).map(Feedback::toString).orElse("?"));

            usuario.setNome("Teste Java Sprint 3 (atualizado)");
            usuarioDAO.atualizar(usuario);
            System.out.println("   usuario atualizado: " + usuarioDAO.buscarPorId(usuario.getId()).map(Usuario::getNome).orElse("?"));
            System.out.println("   status final da transcricao: " + transcricaoDAO.buscarPorId(reuniao.getId()).map(Transcricao::getStatus).orElse(null)
                    + " (esperado " + StatusTranscricao.PROCESSADA + ")");
        } finally {
            // DELETE (limpeza): a analise, os detalhes, os alertas e o feedback saem em cascata
            System.out.println("\n-- DELETE");
            if (reuniao != null) {
                transcricaoDAO.excluir(reuniao.getId());
                System.out.println("   transcricao excluida; ainda existe? " + transcricaoDAO.buscarPorId(reuniao.getId()).isPresent());
                System.out.println("   analise ainda existe? " + analiseDAO.buscarPorTranscricao(reuniao.getId()).isPresent());
            }
            usuarioDAO.excluir(usuario.getId());
            System.out.println("   usuario excluido; ainda existe? " + usuarioDAO.buscarPorId(usuario.getId()).isPresent());
        }
        System.out.println("\n=== CRUD concluido com sucesso ===");
    }
}
