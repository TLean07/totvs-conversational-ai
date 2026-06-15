package com.tlean07.fiap.challenge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlean07.fiap.challenge.dto.TranscricaoImportResponse;
import com.tlean07.fiap.challenge.model.Transcricao;
import com.tlean07.fiap.challenge.repository.TranscricaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TranscricaoImportServiceTest {

    @Test
    void importsSingleJsonObject() {
        TestContext context = context();
        MultipartFile file = jsonFile("""
                {
                  "ID_MEETING": "1247082",
                  "DT_MEETING": "2026-03-18 16:00:00",
                  "FORMATO_MEETING": "VIDEO",
                  "STATUS_MEETING": "COMPLETED",
                  "DURACAO_MEETING": "01:39:13",
                  "CODT": "T27261",
                  "FLG_EXTERNO": true,
                  "DT_CRIACAO": "2026-03-13 16:47:22",
                  "ANON_TRANSCRICAO": "[LOCUTOR 1]: Texto da reuniao"
                }
                """);

        TranscricaoImportResponse response = context.service.importar(file, "owner-1");

        assertEquals(1, response.getFound());
        assertEquals(1, response.getImported());
        assertEquals(0, response.getSkipped());
        assertEquals(0, response.getFailed());
        assertEquals(1, context.saved.size());
        Transcricao saved = context.saved.get(0);
        assertEquals("1247082", saved.getMeetingId());
        assertEquals("[LOCUTOR 1]: Texto da reuniao", saved.getTextoOriginal());
        assertEquals(saved.getTextoOriginal(), saved.getTranscriptionText());
        assertEquals("VIDEO", saved.getFormat());
        assertEquals("COMPLETED", saved.getMeetingStatus());
        assertEquals("T27261", saved.getCodt());
        assertTrue(saved.getExternal());
        assertNotNull(saved.getImportBatchId());
        assertNotNull(saved.getImportedAt());
        assertFalse(saved.isMissingMeetingId());
    }

    @Test
    void importsThreeObjectsFromRootArray() {
        TestContext context = context();
        MultipartFile file = jsonFile("""
                [
                  {"ID_MEETING":"1","ANON_TRANSCRICAO":"Primeira"},
                  {"ID_MEETING":"2","ANON_TRANSCRICAO":"Segunda"},
                  {"ID_MEETING":"3","ANON_TRANSCRICAO":"Terceira"}
                ]
                """);

        TranscricaoImportResponse response = context.service.importar(file, "owner-1");

        assertEquals(3, response.getFound());
        assertEquals(3, response.getImported());
        assertEquals(List.of("1", "2", "3"), context.saved.stream().map(Transcricao::getMeetingId).toList());
    }

    @Test
    void importsItemsFromListInsideArbitraryRootKey() {
        TestContext context = context();
        MultipartFile file = jsonFile("""
                {
                  "payload": {
                    "reunioes_totvs": [
                      {"ID_MEETING":"10","ANON_TRANSCRICAO":"Primeira"},
                      {"ID_MEETING":"11","ANON_TRANSCRICAO":"Segunda"}
                    ]
                  }
                }
                """);

        TranscricaoImportResponse response = context.service.importar(file, "owner-1");

        assertEquals(2, response.getFound());
        assertEquals(2, response.getImported());
        assertEquals(List.of("10", "11"), context.saved.stream().map(Transcricao::getMeetingId).toList());
    }

    @Test
    void skipsMeetingIdAlreadyStored() {
        TestContext context = context();
        when(context.repository.existsByMeetingId("2")).thenReturn(true);
        MultipartFile file = jsonFile("""
                [
                  {"ID_MEETING":"1","ANON_TRANSCRICAO":"Primeira"},
                  {"ID_MEETING":"2","ANON_TRANSCRICAO":"Duplicada"},
                  {"ID_MEETING":"3","ANON_TRANSCRICAO":"Terceira"}
                ]
                """);

        TranscricaoImportResponse response = context.service.importar(file, "owner-1");

        assertEquals(3, response.getFound());
        assertEquals(2, response.getImported());
        assertEquals(1, response.getSkipped());
        assertEquals("SKIPPED_DUPLICATE", response.getItems().get(1).getStatus());
        assertEquals(List.of("1", "3"), context.saved.stream().map(Transcricao::getMeetingId).toList());
    }

    @Test
    void invalidItemDoesNotStopOtherImports() {
        TestContext context = context();
        MultipartFile file = jsonFile("""
                [
                  {"ID_MEETING":"1","ANON_TRANSCRICAO":"Primeira"},
                  {"ID_MEETING":"2","STATUS_MEETING":"COMPLETED"},
                  {"ID_MEETING":"3","ANON_TRANSCRICAO":"Terceira"}
                ]
                """);

        TranscricaoImportResponse response = context.service.importar(file, "owner-1");

        assertEquals(3, response.getFound());
        assertEquals(2, response.getImported());
        assertEquals(1, response.getFailed());
        assertEquals("MISSING_ANON_TRANSCRICAO", response.getItems().get(1).getReason());
        assertEquals(List.of("1", "3"), context.saved.stream().map(Transcricao::getMeetingId).toList());
    }

    @Test
    void importsRealAnonTranscricaoFileWhenPathIsProvided() {
        String configuredPath = System.getProperty("totvs.real-json");
        assumeTrue(configuredPath != null && Files.isRegularFile(Path.of(configuredPath)));

        TranscricaoRepository repository = mock(TranscricaoRepository.class);
        TranscricaoAnalysisJob analysisJob = mock(TranscricaoAnalysisJob.class);
        AuditService auditService = mock(AuditService.class);
        AtomicInteger ids = new AtomicInteger();
        when(repository.existsByMeetingId(anyString())).thenReturn(false);
        when(repository.save(any(Transcricao.class))).thenAnswer(invocation -> {
            Transcricao entity = invocation.getArgument(0);
            entity.setId("real-" + ids.incrementAndGet());
            return entity;
        });
        TranscricaoImportService service = new TranscricaoImportService(
                new TotvsTranscriptionJsonParser(new ObjectMapper()),
                repository,
                analysisJob,
                auditService
        );

        TranscricaoImportResponse response = service.importar(new PathMultipartFile(Path.of(configuredPath)), "owner-real");

        assertEquals(1174, response.getFound());
        assertEquals(1126, response.getImported());
        assertEquals(48, response.getSkipped());
        assertEquals(0, response.getFailed());
        assertEquals(1174, response.getItems().size());
        assertEquals(1126, ids.get());
        verify(repository, times(1126)).save(any(Transcricao.class));
        verify(analysisJob, times(1126)).processar(anyString());
    }

    private TestContext context() {
        TranscricaoRepository repository = mock(TranscricaoRepository.class);
        TranscricaoAnalysisJob analysisJob = mock(TranscricaoAnalysisJob.class);
        AuditService auditService = mock(AuditService.class);
        List<Transcricao> saved = new ArrayList<>();
        AtomicInteger ids = new AtomicInteger();

        when(repository.existsByMeetingId(anyString())).thenReturn(false);
        when(repository.save(any(Transcricao.class))).thenAnswer(invocation -> {
            Transcricao entity = invocation.getArgument(0);
            entity.setId("saved-" + ids.incrementAndGet());
            saved.add(entity);
            return entity;
        });

        TranscricaoImportService service = new TranscricaoImportService(
                new TotvsTranscriptionJsonParser(new ObjectMapper()),
                repository,
                analysisJob,
                auditService
        );
        return new TestContext(service, repository, saved);
    }

    private MultipartFile jsonFile(String json) {
        return new MockMultipartFile("file", "ANON_transcricao.json", "application/json", json.getBytes());
    }

    private record TestContext(TranscricaoImportService service,
                               TranscricaoRepository repository,
                               List<Transcricao> saved) {
    }

    private static class PathMultipartFile implements MultipartFile {
        private final Path path;

        private PathMultipartFile(Path path) {
            this.path = path;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return path.getFileName().toString();
        }

        @Override
        public String getContentType() {
            return "application/json";
        }

        @Override
        public boolean isEmpty() {
            try {
                return Files.size(path) == 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(path, dest.toPath());
        }
    }
}
