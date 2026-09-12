package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Le o CSV de reunioes da TOTVS (ex.: reunioes_transcricoes_mockado.csv) e entrega cada linha
 * como um objeto JSON com as mesmas chaves do export JSON, para o importador tratar os dois
 * formatos por um unico caminho.
 *
 * Diferencas do CSV tratadas aqui: codificacao (o arquivo de exemplo vem em Windows-1252, nao
 * UTF-8), delimitador (virgula ou ponto-e-virgula), cabecalho com espacos/caixa variavel e a
 * coluna FAIXA_FATURAMENTO_CLIENTE, que no JSON se chama FAIXA_FATURAMENTO_CLIENTE_EC.
 */
@Component
public class TotvsTranscriptionCsvParser {

    private static final String TRANSCRIPTION_FIELD = "ANON_TRANSCRICAO";
    private static final Map<String, String> ALIASES = Map.of(
            "FAIXA_FATURAMENTO_CLIENTE", "FAIXA_FATURAMENTO_CLIENTE_EC"
    );

    private final ObjectMapper objectMapper;

    public TotvsTranscriptionCsvParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TotvsTranscriptionJsonParser.ParseSummary parse(InputStream inputStream,
                                                           TotvsTranscriptionJsonParser.RecordConsumer consumer)
            throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        Charset charset = detectCharset(bytes);
        int offset = startsWithUtf8Bom(bytes) ? 3 : 0;

        char delimiter = detectDelimiter(bytes, offset, charset);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setAllowMissingColumnNames(true)
                .build();

        int found = 0;
        boolean transcriptionFieldFound = false;
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes, offset, bytes.length - offset), charset);
             CSVParser parser = format.parse(reader)) {
            Map<String, Integer> headers = parser.getHeaderMap();
            transcriptionFieldFound = headers.keySet().stream()
                    .anyMatch(h -> normalizeHeader(h).equals(TRANSCRIPTION_FIELD));
            for (CSVRecord record : parser) {
                if (isBlankRecord(record)) {
                    continue;
                }
                ObjectNode node = objectMapper.createObjectNode();
                for (Map.Entry<String, Integer> header : headers.entrySet()) {
                    String key = normalizeHeader(header.getKey());
                    if (key.isEmpty() || header.getValue() >= record.size()) {
                        continue;
                    }
                    String value = record.get(header.getValue());
                    node.put(key, value == null ? "" : value);
                    String alias = ALIASES.get(key);
                    if (alias != null && !node.has(alias)) {
                        node.put(alias, value == null ? "" : value);
                    }
                }
                consumer.accept(found, node);
                found++;
            }
        }
        return new TotvsTranscriptionJsonParser.ParseSummary(found, transcriptionFieldFound);
    }

    /** UTF-8 valido (com ou sem BOM) ou, se houver bytes invalidos, Windows-1252 (superconjunto do ISO-8859-1). */
    static Charset detectCharset(byte[] bytes) {
        if (startsWithUtf8Bom(bytes)) {
            return StandardCharsets.UTF_8;
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return Charset.forName("windows-1252");
        }
    }

    private static boolean startsWithUtf8Bom(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF;
    }

    private static char detectDelimiter(byte[] bytes, int offset, Charset charset) {
        int end = Math.min(bytes.length, offset + 4096);
        String head = new String(bytes, offset, end - offset, charset);
        int newline = head.indexOf('\n');
        String firstLine = newline > 0 ? head.substring(0, newline) : head;
        long commas = firstLine.chars().filter(c -> c == ',').count();
        long semicolons = firstLine.chars().filter(c -> c == ';').count();
        long tabs = firstLine.chars().filter(c -> c == '\t').count();
        if (tabs > commas && tabs > semicolons) {
            return '\t';
        }
        return semicolons > commas ? ';' : ',';
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.replace("﻿", "").trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlankRecord(CSVRecord record) {
        for (String value : record) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
