package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

@Component
public class TotvsTranscriptionJsonParser {

    private static final String TRANSCRIPTION_FIELD = "ANON_TRANSCRICAO";

    private final ObjectMapper objectMapper;

    public TotvsTranscriptionJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParseSummary parse(InputStream inputStream, RecordConsumer consumer) throws IOException {
        ParseState state = new ParseState(consumer);

        try (JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.START_ARRAY) {
                    parseRootArray(parser, state);
                } else if (token == JsonToken.START_OBJECT) {
                    JsonNode root = objectMapper.readTree(parser);
                    processRootObject(root, state);
                } else if (token.isStructStart()) {
                    parser.skipChildren();
                }
            }
        }

        return new ParseSummary(state.found, state.transcriptionFieldFound);
    }

    private void parseRootArray(JsonParser parser, ParseState state) throws IOException {
        JsonToken token;
        while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
            if (token == null) {
                throw new IOException("Array JSON nao foi finalizado");
            }
            if (token == JsonToken.START_OBJECT) {
                JsonNode item = objectMapper.readTree(parser);
                state.addCandidate(item);
            } else if (token == JsonToken.START_ARRAY) {
                parseRootArray(parser, state);
            } else if (token.isStructStart()) {
                parser.skipChildren();
            }
        }
    }

    private void processRootObject(JsonNode object, ParseState state) {
        if (hasTranscriptionField(object)) {
            state.addCandidate(object);
            return;
        }
        findNestedMeetingArrays(object, state);
    }

    private void findNestedMeetingArrays(JsonNode node, ParseState state) {
        if (node == null || !node.isContainerNode()) {
            return;
        }

        if (node.isArray()) {
            processNestedArray(node, state);
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            JsonNode child = fields.next().getValue();
            if (child.isArray()) {
                processNestedArray(child, state);
            } else if (child.isObject()) {
                findNestedMeetingArrays(child, state);
            }
        }
    }

    private void processNestedArray(JsonNode array, ParseState state) {
        boolean meetingArray = false;
        for (JsonNode item : array) {
            if (item.isObject() && hasTranscriptionField(item)) {
                meetingArray = true;
                break;
            }
        }

        if (meetingArray) {
            for (JsonNode item : array) {
                if (item.isObject()) {
                    state.addCandidate(item);
                }
            }
            return;
        }

        for (JsonNode item : array) {
            findNestedMeetingArrays(item, state);
        }
    }

    private boolean hasTranscriptionField(JsonNode node) {
        return node != null && node.isObject() && node.has(TRANSCRIPTION_FIELD);
    }

    @FunctionalInterface
    public interface RecordConsumer {
        void accept(int index, JsonNode record);
    }

    public record ParseSummary(int found, boolean transcriptionFieldFound) {
    }

    private class ParseState {
        private final RecordConsumer consumer;
        private int found;
        private boolean transcriptionFieldFound;

        private ParseState(RecordConsumer consumer) {
            this.consumer = consumer;
        }

        private void addCandidate(JsonNode record) {
            found++;
            transcriptionFieldFound = transcriptionFieldFound || hasTranscriptionField(record);
            consumer.accept(found - 1, record);
        }
    }
}
