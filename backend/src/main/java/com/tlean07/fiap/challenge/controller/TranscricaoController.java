package com.tlean07.fiap.challenge.controller;

import com.tlean07.fiap.challenge.dto.TranscricaoRequest;
import com.tlean07.fiap.challenge.dto.TranscricaoResponse;
import com.tlean07.fiap.challenge.service.TranscricaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transcricoes")
public class TranscricaoController {

    private final TranscricaoService transcricaoService;

    public TranscricaoController(TranscricaoService transcricaoService) {
        this.transcricaoService = transcricaoService;
    }

    @PostMapping
    public ResponseEntity<TranscricaoResponse> criar(@Valid @RequestBody TranscricaoRequest request) {
        TranscricaoResponse response = transcricaoService.criarEAnalisar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TranscricaoResponse>> listarTodas() {
        return ResponseEntity.ok(transcricaoService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TranscricaoResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(transcricaoService.buscarPorId(id));
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
}
