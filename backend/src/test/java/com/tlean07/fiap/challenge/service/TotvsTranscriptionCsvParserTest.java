package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotvsTranscriptionCsvParserTest {

    private static final String CABECALHO = "ID_MEETING,DT_MEETING,FORMATO_MEETING,ID_STATUS_MEETING,STATUS_MEETING,"
            + "DURACAO_MEETING,CODT,TP_RECURSO,FLG_EXTERNO,DT_CRIACAO,ANON_TRANSCRICAO,UF,CNAE,NOME_UNIDADE,NOME_SEGMENTO,"
            + "FAIXA_FATURAMENTO_CLIENTE,DT_ULTIMA_PESQUISA,NOTA_NPS\n";

    private final TotvsTranscriptionCsvParser parser = new TotvsTranscriptionCsvParser(new ObjectMapper());

    @Test
    void leCsvEmWindows1252ComQuebrasDeLinhaDentroDaTranscricao() throws IOException {
        String csv = CABECALHO
                + "1000000,2025-03-23 00:00:00,ÁUDIO,1,IN_PROGRESS,01:37:17,T76640,OPERACIONAL,False,2025-03-19 00:00:00,"
                + "\"[LOCUTOR 1]: A nossa solução conecta todas as lojas.\n[LOCUTOR 2]: Como funciona?\",SC,6311,Tech Solutions,"
                + "Varejo,100K-500K,2025-04-02,9\n";
        byte[] bytes = csv.getBytes(Charset.forName("windows-1252"));

        List<JsonNode> registros = parse(bytes);

        assertEquals(1, registros.size());
        JsonNode r = registros.get(0);
        assertEquals("1000000", r.get("ID_MEETING").asText());
        assertEquals("ÁUDIO", r.get("FORMATO_MEETING").asText());
        assertTrue(r.get("ANON_TRANSCRICAO").asText().contains("solução"));
        assertTrue(r.get("ANON_TRANSCRICAO").asText().contains("\n[LOCUTOR 2]"));
        assertEquals("100K-500K", r.get("FAIXA_FATURAMENTO_CLIENTE").asText());
        assertEquals("100K-500K", r.get("FAIXA_FATURAMENTO_CLIENTE_EC").asText(), "alias do nome usado no JSON");
        assertEquals("9", r.get("NOTA_NPS").asText());
    }

    @Test
    void leCsvUtf8ComBomEPontoEVirgula() throws IOException {
        String csv = "﻿ID_MEETING;ANON_TRANSCRICAO;UF\n7;\"[LOCUTOR 1]: Olá, tudo bem?\";SP\n\n";
        List<JsonNode> registros = parse(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, registros.size());
        assertEquals("7", registros.get(0).get("ID_MEETING").asText());
        assertEquals("[LOCUTOR 1]: Olá, tudo bem?", registros.get(0).get("ANON_TRANSCRICAO").asText());
    }

    @Test
    void reportaQuandoNaoHaColunaDeTranscricao() throws IOException {
        List<JsonNode> registros = new ArrayList<>();
        var summary = parser.parse(new ByteArrayInputStream("A,B\n1,2\n".getBytes(StandardCharsets.UTF_8)),
                (i, node) -> registros.add(node));
        assertEquals(1, summary.found());
        assertTrue(!summary.transcriptionFieldFound());
    }

    private List<JsonNode> parse(byte[] bytes) throws IOException {
        List<JsonNode> registros = new ArrayList<>();
        var summary = parser.parse(new ByteArrayInputStream(bytes), (i, node) -> registros.add(node));
        assertTrue(summary.transcriptionFieldFound());
        return registros;
    }
}
