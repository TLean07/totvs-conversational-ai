package com.tlean07.fiap.challenge.controller;

import com.tlean07.fiap.challenge.dto.AnaliseFeedbackRequest;
import com.tlean07.fiap.challenge.dto.AnaliseFeedbackResponse;
import com.tlean07.fiap.challenge.dto.TranscricaoRequest;
import com.tlean07.fiap.challenge.dto.TranscricaoImportResponse;
import com.tlean07.fiap.challenge.dto.TranscricaoResponse;
import com.tlean07.fiap.challenge.dto.PageResponse;
import com.tlean07.fiap.challenge.service.RateLimitService;
import com.tlean07.fiap.challenge.service.TranscricaoService;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/transcricoes")
public class TranscricaoController {

    private final TranscricaoService transcricaoService;
    private final RateLimitService rateLimitService;

    public TranscricaoController(TranscricaoService transcricaoService,
                                 RateLimitService rateLimitService) {
        this.transcricaoService = transcricaoService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<TranscricaoResponse> criar(@Valid @RequestBody TranscricaoRequest request,
                                                     HttpServletRequest httpRequest) {
        rateLimitService.check("transcricao:create:user:" + SecurityUtils.currentUser().id(), 20, Duration.ofHours(1));
        rateLimitService.check("transcricao:create:ip:" + clientKey(httpRequest), 40, Duration.ofHours(1));
        TranscricaoResponse response = transcricaoService.criarEAnalisar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/import/totvs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscricaoImportResponse> importarTotvsJson(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest httpRequest) {
        rateLimitService.check("transcricao:import:user:" + SecurityUtils.currentUser().id(), 5, Duration.ofHours(1));
        rateLimitService.check("transcricao:import:ip:" + clientKey(httpRequest), 10, Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.CREATED).body(transcricaoService.importarTotvsJson(file, limit, offset));
    }

    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscricaoResponse> importarAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String vendedor,
            @RequestParam(required = false) String participantes,
            HttpServletRequest httpRequest) {
        rateLimitService.check("transcricao:audio:user:" + SecurityUtils.currentUser().id(), 3, Duration.ofHours(1));
        rateLimitService.check("transcricao:audio:ip:" + clientKey(httpRequest), 6, Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transcricaoService.importarAudio(file, cliente, vendedor, participantes));
    }

    @GetMapping
    public ResponseEntity<List<TranscricaoResponse>> listarTodas() {
        return ResponseEntity.ok(transcricaoService.listarTodas());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<TranscricaoResponse>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transcricaoService.listarPaginado(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TranscricaoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(transcricaoService.buscarPorId(id));
    }

    @PostMapping("/{id}/reprocessar")
    public ResponseEntity<TranscricaoResponse> reprocessar(@PathVariable String id) {
        return ResponseEntity.accepted().body(transcricaoService.reprocessar(id));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<AnaliseFeedbackResponse> registrarFeedback(
            @PathVariable String id,
            @Valid @RequestBody AnaliseFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transcricaoService.registrarFeedback(id, request));
    }

    @GetMapping("/{id}/feedback")
    public ResponseEntity<List<AnaliseFeedbackResponse>> listarFeedbacks(@PathVariable String id) {
        return ResponseEntity.ok(transcricaoService.listarFeedbacks(id));
    }

    @GetMapping("/cliente/{cliente}")
    public ResponseEntity<List<TranscricaoResponse>> buscarPorCliente(@PathVariable String cliente) {
        return ResponseEntity.ok(transcricaoService.buscarPorCliente(cliente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        transcricaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
