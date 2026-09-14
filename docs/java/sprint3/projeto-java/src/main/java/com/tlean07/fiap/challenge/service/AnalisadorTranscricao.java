package com.tlean07.fiap.challenge.service;

import com.tlean07.fiap.challenge.model.Analise;
import com.tlean07.fiap.challenge.model.Oportunidade;
import com.tlean07.fiap.challenge.model.Produto;
import com.tlean07.fiap.challenge.model.Risco;
import com.tlean07.fiap.challenge.model.Sentimento;
import com.tlean07.fiap.challenge.model.Severidade;
import com.tlean07.fiap.challenge.model.TipoOportunidade;
import com.tlean07.fiap.challenge.model.TipoProduto;
import com.tlean07.fiap.challenge.model.TipoRisco;
import com.tlean07.fiap.challenge.model.Transcricao;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metodo de relevancia 1: analisa o texto de uma reuniao e produz a Analise
 * (sentimento com negacao, produtos e concorrentes, oportunidades, riscos, persona, budget,
 * confianca no vendedor, resumo e proxima acao). Versao Java das regras da LeanIA.
 */
public class AnalisadorTranscricao {

    private static final Map<String, String> PRODUTOS_TOTVS = new LinkedHashMap<>();
    private static final Map<String, String> CONCORRENTES = new LinkedHashMap<>();
    private static final String[] POSITIVAS = {"otimo", "excelente", "perfeito", "adorei", "gostei", "satisfeito", "funciona bem", "atende bem", "redondo", "parabens", "maravilha"};
    private static final String[] NEGATIVAS = {"problema", "ruim", "pessimo", "frustra", "insatisf", "lento", "demora", "travou", "trava", "erro", "reclama", "nao funciona", "horrivel", "perdemos a confianca"};
    private static final String[] NEGACOES = {"nao ", "nunca ", "nem ", "sem "};
    private static final String[] INTENSIFICADORES = {"muito ", "extremamente ", "bastante ", "super "};
    private static final Pattern BUDGET = Pattern.compile("r\\$\\s?([0-9][0-9.,]*)(\\s?(mil|milhoes|milhao|k))?(\\s?(por mes|/mes|mensais|ao mes))?");
    private static final Pattern LOCUTOR = Pattern.compile("\\[?locutor\\s*(\\d+)\\]?\\s*:");

    static {
        PRODUTOS_TOTVS.put("protheus", "TOTVS Protheus");
        PRODUTOS_TOTVS.put("datasul", "TOTVS Datasul");
        PRODUTOS_TOTVS.put("fluig", "TOTVS Fluig");
        PRODUTOS_TOTVS.put("techfin", "TOTVS Techfin");
        PRODUTOS_TOTVS.put("carol", "TOTVS Carol");
        PRODUTOS_TOTVS.put("rm ", "TOTVS RM");
        PRODUTOS_TOTVS.put("totvs varejo", "TOTVS Varejo");
        PRODUTOS_TOTVS.put("totvs saude", "TOTVS Saude");
        PRODUTOS_TOTVS.put("totvs agro", "TOTVS Agro");
        CONCORRENTES.put("sap", "SAP");
        CONCORRENTES.put("senior", "Senior");
        CONCORRENTES.put("sankhya", "Sankhya");
        CONCORRENTES.put("oracle netsuite", "Oracle NetSuite");
        CONCORRENTES.put("netsuite", "Oracle NetSuite");
        CONCORRENTES.put("omie", "Omie");
        CONCORRENTES.put("bling", "Bling");
        CONCORRENTES.put("conta azul", "Conta Azul");
        CONCORRENTES.put("linx", "Linx");
    }

    public Analise analisar(Transcricao transcricao) {
        transcricao.validar();
        String texto = normalizar(transcricao.getTextoOriginal());
        List<String> sentencas = dividirSentencas(texto);
        Analise analise = new Analise();
        analise.setTranscricaoId(transcricao.getId());
        analise.setOwnerId(transcricao.getOwnerId());

        identificarProdutos(texto, sentencas, analise);
        identificarOportunidades(sentencas, analise);
        identificarRiscos(sentencas, analise);
        classificarSentimento(sentencas, analise);
        analise.setPersonaIdentificada(identificarPersona(texto));
        analise.setBudgetIdentificado(identificarBudget(texto));
        analise.setScoreConfiancaVendedor(calcularConfiancaVendedor(texto, analise));
        analise.gerarResumo();
        return analise;
    }

    // ------------------------------------------------------------------ produtos e concorrentes
    private void identificarProdutos(String texto, List<String> sentencas, Analise analise) {
        for (Map.Entry<String, String> e : PRODUTOS_TOTVS.entrySet()) {
            String chave = e.getKey().trim();
            if (contemPalavra(texto, chave)) {
                String sentenca = primeiraSentencaCom(sentencas, chave);
                // "usamos o X" / "chamado do X" = produto em uso; "o X resolve" / "o X tem o modulo" = argumento de venda
                boolean emUso = contem(sentenca, "usamos", "uso do", "uso da", "nosso ", "nossa ", "chamado", "suporte", "esta redondo", "atende");
                boolean pitch = contem(sentenca, "resolve", "resolveria", "faz essa", "tem o modulo", "tem modulo", "integra", "oferece", "migrar",
                        "implantar", "contratar", "proposta", "novo", "queremos", "interesse", "apresentar", "demonstra");
                TipoProduto tipo = pitch && !emUso ? TipoProduto.OPORTUNIDADE : TipoProduto.ATUAL;
                analise.getProdutos().add(new Produto(e.getValue(), tipo, recortar(sentenca)));
            }
        }
        for (Map.Entry<String, String> e : CONCORRENTES.entrySet()) {
            if (contemPalavra(texto, e.getKey()) && analise.getProdutos().stream().noneMatch(p -> p.getNome().equals(e.getValue()))) {
                analise.getProdutos().add(new Produto(e.getValue(), TipoProduto.CONCORRENTE, recortar(primeiraSentencaCom(sentencas, e.getKey()))));
            }
        }
    }

    // ------------------------------------------------------------------ oportunidades
    private void identificarOportunidades(List<String> sentencas, Analise analise) {
        List<Oportunidade> candidatas = new ArrayList<>();
        for (String s : sentencas) {
            if (contem(s, "manda a proposta", "mandar a proposta", "enviar proposta", "envie a proposta", "precisamos de uma proposta", "uma proposta ate",
                    "verba aprovada", "orcamento aprovado", "orcamento previsto", "vamos avancar", "queremos implantar", "fechar", "contratar")) {
                candidatas.add(new Oportunidade(tipoOportunidade(s, analise), "Cliente sinaliza intencao de compra: " + recortar(s),
                        recortar(s), produtoRelacionado(s, analise), 0.85, valorNaSentenca(s)));
            } else if (contem(s, "interesse", "gostei", "faz sentido", "demonstracao", "apresentar", "avaliar", "novo modulo", "modulo de", "integrar", "integracao")) {
                candidatas.add(new Oportunidade(tipoOportunidade(s, analise), "Cliente demonstra interesse: " + recortar(s),
                        recortar(s), produtoRelacionado(s, analise), 0.6, valorNaSentenca(s)));
            }
        }
        // as mais fortes primeiro; no maximo tres oportunidades por reuniao
        candidatas.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        analise.getInsights().addAll(candidatas.subList(0, Math.min(3, candidatas.size())));
    }

    private TipoOportunidade tipoOportunidade(String sentenca, Analise analise) {
        boolean temProdutoAtual = analise.getProdutos().stream().anyMatch(p -> p.getTipo() == TipoProduto.ATUAL);
        if (contem(sentenca, "techfin", "antecipa", "financeiro", "recebiveis", "rd station", "carol")) {
            return TipoOportunidade.CROSS_SELL;
        }
        if (!temProdutoAtual || contem(sentenca, "migrar", "trocar", "implantar", "substituir")) {
            return TipoOportunidade.NOVO_PRODUTO;
        }
        return TipoOportunidade.UPSELL;
    }

    private String produtoRelacionado(String sentenca, Analise analise) {
        for (Map.Entry<String, String> e : PRODUTOS_TOTVS.entrySet()) {
            if (contemPalavra(sentenca, e.getKey().trim())) {
                return e.getValue();
            }
        }
        return analise.getProdutos().stream().filter(p -> p.getTipo() == TipoProduto.OPORTUNIDADE).map(Produto::getNome).findFirst().orElse("A definir");
    }

    // ------------------------------------------------------------------ riscos
    private void identificarRiscos(List<String> sentencas, Analise analise) {
        for (String s : sentencas) {
            if (contem(s, "cancelar", "cancelamento", "nao vamos renovar", "nao renovar", "trocar de fornecedor", "vamos trocar", "encerrar o contrato", "sair da totvs")) {
                analise.getInsights().add(new Risco(TipoRisco.CHURN, "Cliente verbalizou cancelamento ou troca de fornecedor", recortar(s), Severidade.ALTO, null));
            } else if (contem(s, "perdemos a confianca", "nao confio", "sem confianca", "mentira", "enganad")) {
                analise.getInsights().add(new Risco(TipoRisco.PERDA_CONFIANCA, "Cliente sinaliza perda de confianca na relacao", recortar(s), Severidade.ALTO, null));
            } else {
                String concorrente = concorrenteNa(s);
                if (concorrente != null && contem(s, "proposta", "apresent", "avaliando", "procurando", "cotando", "negocia", "pensando em trocar")) {
                    analise.getInsights().add(new Risco(TipoRisco.CONCORRENCIA, "Concorrente em negociacao com o cliente", recortar(s), Severidade.MEDIO, concorrente));
                } else if (contemNegativaSemNegacao(s)) {
                    Severidade sev = contemIntensificador(s) ? Severidade.ALTO : (contem(s, "semanas", "ninguem", "de novo", "sempre") ? Severidade.MEDIO : Severidade.BAIXO);
                    analise.getInsights().add(new Risco(TipoRisco.INSATISFACAO, "Insatisfacao relatada pelo cliente", recortar(s), sev, null));
                }
            }
        }
        limitar(analise, Risco.class, 4);
    }

    private String concorrenteNa(String sentenca) {
        for (Map.Entry<String, String> e : CONCORRENTES.entrySet()) {
            if (contemPalavra(sentenca, e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ sentimento
    private void classificarSentimento(List<String> sentencas, Analise analise) {
        int positivas = 0;
        int negativas = 0;
        for (String s : sentencas) {
            if (contemNegativaSemNegacao(s)) {
                negativas += contemIntensificador(s) ? 2 : 1;
            }
            if (contemPositivaSemNegacao(s)) {
                positivas += contemIntensificador(s) ? 2 : 1;
            }
        }
        // riscos de churn e de perda de confianca sao sinais negativos fortes, mesmo sem palavras do lexico
        for (Risco r : analise.getRiscos()) {
            if (r.getTipo() == TipoRisco.CHURN || r.getTipo() == TipoRisco.PERDA_CONFIANCA) {
                negativas += 2;
            }
        }
        int total = positivas + negativas;
        double score = total == 0 ? 0.0 : (positivas - negativas) / (double) total;
        analise.setScoreSentimento(Math.round(score * 100.0) / 100.0);
        if (total == 0) {
            analise.setSentimentoGeral(Sentimento.NEUTRO);
        } else if (positivas > 0 && negativas > 0 && Math.abs(score) < 0.5) {
            analise.setSentimentoGeral(Sentimento.MISTO);
        } else if (score > 0) {
            analise.setSentimentoGeral(Sentimento.POSITIVO);
        } else {
            analise.setSentimentoGeral(Sentimento.NEGATIVO);
        }
    }

    private boolean contemNegativaSemNegacao(String s) {
        return contemSemNegacao(s, NEGATIVAS);
    }

    private boolean contemPositivaSemNegacao(String s) {
        return contemSemNegacao(s, POSITIVAS);
    }

    /** "nao gostei" conta como negativo, e nao como positivo: a negacao inverte o termo. */
    private boolean contemSemNegacao(String s, String[] termos) {
        for (String termo : termos) {
            int i = s.indexOf(termo);
            if (i >= 0) {
                String antes = s.substring(Math.max(0, i - 8), i);
                boolean negado = false;
                for (String n : NEGACOES) {
                    if (antes.contains(n)) {
                        negado = true;
                    }
                }
                if (!negado) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean contemIntensificador(String s) {
        return contem(s, INTENSIFICADORES);
    }

    // ------------------------------------------------------------------ persona, budget, confianca
    String identificarPersona(String texto) {
        if (contem(texto, "diretor", "diretoria", "ceo", "socio", "dono", "presidente")) {
            return "Socio/Executivo (decisor final)";
        }
        if (contem(texto, "cfo", "financeiro", "controller", "contabil")) {
            return "Gestor Financeiro (decisor tecnico)";
        }
        if (contem(texto, " ti ", "tecnologia", "sistemas", "infra", "cto")) {
            return "Gestor de TI (influenciador tecnico)";
        }
        if (contem(texto, "gerente", "coordenador", "supervisor", "operacao")) {
            return "Gestor Operacional (decisor tecnico/influenciador)";
        }
        return null;
    }

    String identificarBudget(String texto) {
        Matcher m = BUDGET.matcher(texto);
        if (!m.find()) {
            return null;
        }
        String numero = m.group(1).replace(".", "").replace(",", ".");
        double valor;
        try {
            valor = Double.parseDouble(numero);
        } catch (NumberFormatException e) {
            return null;
        }
        String unidade = m.group(3) == null ? "" : m.group(3).trim();
        if (unidade.equals("mil") || unidade.equals("k")) {
            valor *= 1_000;
        } else if (unidade.startsWith("milh")) {
            valor *= 1_000_000;
        }
        String formatado = String.format(java.util.Locale.US, "R$ %,.2f", valor).replace(",", "X").replace(".", ",").replace("X", ".");
        return m.group(4) != null ? formatado + " por mes" : formatado;
    }

    /**
     * Confianca no vendedor: parte de 0,5; sobe com sinais de conducao (proposta, demonstracao,
     * proximos passos) e desce com riscos altos e perda de confianca.
     */
    double calcularConfiancaVendedor(String texto, Analise analise) {
        double score = 0.5;
        if (contem(texto, "proposta", "demonstracao", "apresentacao", "cronograma", "proximo passo", "proximos passos", "agendar")) {
            score += 0.2;
        }
        if (contem(texto, "entendo", "vou verificar", "vou escalar", "vou resolver", "pode deixar")) {
            score += 0.1;
        }
        for (Risco r : analise.getRiscos()) {
            if (r.getTipo() == TipoRisco.PERDA_CONFIANCA) {
                score -= 0.3;
            } else if (r.getSeveridade() == Severidade.ALTO) {
                score -= 0.15;
            }
        }
        return Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));
    }

    // ------------------------------------------------------------------ utilitarios de texto
    static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return " " + semAcento.toLowerCase().replaceAll("\\s+", " ").trim() + " ";
    }

    static List<String> dividirSentencas(String textoNormalizado) {
        String semLocutores = LOCUTOR.matcher(textoNormalizado).replaceAll(" | ");
        List<String> sentencas = new ArrayList<>();
        for (String parte : semLocutores.split("[.!?|]")) {
            String s = parte.trim();
            if (s.length() >= 4) {
                sentencas.add(" " + s + " ");
            }
        }
        return sentencas;
    }

    private static boolean contem(String texto, String... termos) {
        for (String t : termos) {
            if (texto.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contemPalavra(String texto, String palavra) {
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(palavra) + "(?![a-z0-9])").matcher(texto).find();
    }

    private static String primeiraSentencaCom(List<String> sentencas, String termo) {
        return sentencas.stream().filter(s -> contemPalavra(s, termo)).findFirst().orElse(termo);
    }

    private static String valorNaSentenca(String sentenca) {
        Matcher m = BUDGET.matcher(sentenca);
        return m.find() ? "R$ " + m.group(1) + (m.group(3) != null ? " " + m.group(3).trim() : "") : "Nao estimado";
    }

    private static String recortar(String s) {
        String t = s.trim();
        return t.length() <= 160 ? t : t.substring(0, 157) + "...";
    }

    private static void limitar(Analise analise, Class<?> tipo, int maximo) {
        int contador = 0;
        var it = analise.getInsights().iterator();
        while (it.hasNext()) {
            if (tipo.isInstance(it.next()) && ++contador > maximo) {
                it.remove();
            }
        }
    }
}
