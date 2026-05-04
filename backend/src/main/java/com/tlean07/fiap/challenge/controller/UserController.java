package com.tlean07.fiap.challenge.controller;

import com.tlean07.fiap.challenge.dto.CreateUserRequest;
import com.tlean07.fiap.challenge.dto.UserResponse;
import com.tlean07.fiap.challenge.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listarTodos() {
        return ResponseEntity.ok(userService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<UserResponse> criar(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.criar(request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> desativar(@PathVariable String id) {
        return ResponseEntity.ok(userService.desativar(id));
    }
}
