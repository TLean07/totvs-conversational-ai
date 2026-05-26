package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioTranscriptionServiceTest {

    private AudioTranscriptionService serviceWithOneMbLimit() {
        return new AudioTranscriptionService(
                new ObjectMapper(),
                new RestTemplateBuilder(),
                "http://localhost:9",
                "",
                1,
                3600,
                "pt"
        );
    }

    @Test
    void rejectsUnsupportedAudioExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "reuniao.txt",
                "text/plain",
                "conteudo".getBytes()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> serviceWithOneMbLimit().transcrever(file, null)
        );

        assertTrue(error.getMessage().contains(".wav, .mp3 ou .m4a"));
    }

    @Test
    void rejectsAudioAboveConfiguredLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "reuniao.wav",
                "audio/wav",
                new byte[2 * 1024 * 1024]
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> serviceWithOneMbLimit().transcrever(file, null)
        );

        assertTrue(error.getMessage().contains("limite de 1 MB"));
    }
}
