package com.tlean07.fiap.challenge.controller;

import com.tlean07.fiap.challenge.dto.AlertaResponse;
import com.tlean07.fiap.challenge.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    public ResponseEntity<List<AlertaResponse>> listarTodos() {
        return ResponseEntity.ok(alertaService.listarTodos());
    }

    @GetMapping("/novos")
    public ResponseEntity<List<AlertaResponse>> listarNovos() {
        return ResponseEntity.ok(alertaService.listarNovos());
    }

    @PatchMapping("/{id}/visto")
    public ResponseEntity<AlertaResponse> marcarComoVisto(@PathVariable String id) {
        return ResponseEntity.ok(alertaService.marcarComoVisto(id));
    }

    @PatchMapping("/{id}/resolvido")
    public ResponseEntity<AlertaResponse> marcarComoResolvido(@PathVariable String id) {
        return ResponseEntity.ok(alertaService.marcarComoResolvido(id));
    }
}
