package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.model.Transcricao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metodo de relevancia 4: indicadores de qualidade da conversa a partir dos turnos
 * "[LOCUTOR N]:" (participacao de cada locutor, perguntas, escuta ativa do vendedor).
 * O locutor que mais fala e considerado o vendedor quando ha marcacao de locutores.
 */
public class CalculadoraIndicadores {

    private static final Pattern TURNO = Pattern.compile("\\[?LOCUTOR\\s*(\\d+)\\]?\\s*:", Pattern.CASE_INSENSITIVE);

    /** Resultado do calculo. */
    public static class IndicadoresConversa {
        private int locutores;
        private int turnos;
        private int palavras;
        private int perguntasVendedor;
        private int perguntasCliente;
        private int participacaoVendedorPct;
        private int participacaoClientePct;
        private String escutaAtiva;

        public int getLocutores() { return locutores; }
        public int getTurnos() { return turnos; }
        public int getPalavras() { return palavras; }
        public int getPerguntasVendedor() { return perguntasVendedor; }
        public int getPerguntasCliente() { return perguntasCliente; }
        public int getParticipacaoVendedorPct() { return participacaoVendedorPct; }
        public int getParticipacaoClientePct() { return participacaoClientePct; }
        public String getEscutaAtiva() { return escutaAtiva; }

        @Override
        public String toString() {
            return "Indicadores{locutores=" + locutores + ", turnos=" + turnos + ", palavras=" + palavras
                    + ", vendedor=" + participacaoVendedorPct + "%, cliente=" + participacaoClientePct + "%"
                    + ", perguntasVendedor=" + perguntasVendedor + ", perguntasCliente=" + perguntasCliente + ", escuta=" + escutaAtiva + "}";
        }
    }

    public IndicadoresConversa calcular(Transcricao transcricao) {
        String texto = transcricao.getTextoOriginal() == null ? "" : transcricao.getTextoOriginal();
        IndicadoresConversa ind = new IndicadoresConversa();
        Map<String, Integer> palavrasPorLocutor = new LinkedHashMap<>();
        Map<String, Integer> perguntasPorLocutor = new LinkedHashMap<>();

        Matcher m = TURNO.matcher(texto);
        int inicioFala = -1;
        String locutorAtual = null;
        while (m.find()) {
            if (locutorAtual != null) {
                contabilizar(texto.substring(inicioFala, m.start()), locutorAtual, palavrasPorLocutor, perguntasPorLocutor);
            }
            locutorAtual = m.group(1);
            inicioFala = m.end();
            ind.turnos++;
        }
        if (locutorAtual != null) {
            contabilizar(texto.substring(inicioFala), locutorAtual, palavrasPorLocutor, perguntasPorLocutor);
        } else {
            contabilizar(texto, "unico", palavrasPorLocutor, perguntasPorLocutor);
            ind.turnos = 1;
        }

        ind.locutores = palavrasPorLocutor.size();
        ind.palavras = palavrasPorLocutor.values().stream().mapToInt(Integer::intValue).sum();
        String vendedor = palavrasPorLocutor.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("unico");
        int palavrasVendedor = palavrasPorLocutor.getOrDefault(vendedor, 0);
        ind.perguntasVendedor = perguntasPorLocutor.getOrDefault(vendedor, 0);
        ind.perguntasCliente = perguntasPorLocutor.values().stream().mapToInt(Integer::intValue).sum() - ind.perguntasVendedor;
        if (ind.palavras > 0 && ind.locutores > 1) {
            ind.participacaoVendedorPct = (int) Math.round(100.0 * palavrasVendedor / ind.palavras);
            ind.participacaoClientePct = 100 - ind.participacaoVendedorPct;
        } else {
            ind.participacaoVendedorPct = 100;
            ind.participacaoClientePct = 0;
        }
        if (ind.locutores <= 1) {
            ind.escutaAtiva = "Nao avaliado (um unico locutor)";
        } else if (ind.participacaoVendedorPct >= 65) {
            ind.escutaAtiva = "Vendedor falou mais do que ouviu";
        } else if (ind.participacaoVendedorPct <= 40) {
            ind.escutaAtiva = "Vendedor ouviu mais do que falou";
        } else {
            ind.escutaAtiva = "Equilibrado";
        }
        return ind;
    }

    private void contabilizar(String fala, String locutor, Map<String, Integer> palavras, Map<String, Integer> perguntas) {
        String limpa = fala.trim();
        if (limpa.isEmpty()) {
            return;
        }
        int n = limpa.split("\\s+").length;
        palavras.merge(locutor, n, Integer::sum);
        int q = (int) limpa.chars().filter(c -> c == '?').count();
        perguntas.merge(locutor, q, Integer::sum);
    }
}
