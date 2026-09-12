package com.tlean07.fiap.challenge.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.tlean07.fiap.challenge.config.CacheConfig.CacheNames;
import com.tlean07.fiap.challenge.dto.CreateUserRequest;
import com.tlean07.fiap.challenge.dto.UserResponse;
import com.tlean07.fiap.challenge.model.AppUser;
import com.tlean07.fiap.challenge.repository.AppUserRepository;
import com.tlean07.fiap.challenge.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Cacheable(cacheNames = CacheNames.USUARIOS, key = "'todos'")
    public List<UserResponse> listarTodos() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @CacheEvict(cacheNames = CacheNames.USUARIOS, allEntries = true)
    public UserResponse criar(CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("E-mail ja cadastrado");
        }

        AppUser user = AppUser.builder()
                .email(email)
                .name(request.getName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        AppUser saved = userRepository.save(user);
        auditService.record("user.create", SecurityUtils.currentUser().id(), saved.getId(), "success");
        return toResponse(saved);
    }

    @CacheEvict(cacheNames = CacheNames.USUARIOS, allEntries = true)
    public UserResponse desativar(String id) {
        String actorId = SecurityUtils.currentUser().id();
        if (actorId.equals(id)) {
            throw new AccessDeniedException("Nao e permitido desativar o proprio usuario");
        }
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new AccessDeniedException("Usuario nao encontrado"));
        user.setActive(false);
        AppUser saved = userRepository.save(user);
        auditService.record("user.deactivate", actorId, id, "success");
        return toResponse(saved);
    }

    @CacheEvict(cacheNames = CacheNames.USUARIOS, allEntries = true)
    public UserResponse ativar(String id) {
        String actorId = SecurityUtils.currentUser().id();
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new AccessDeniedException("Usuario nao encontrado"));
        user.setActive(true);
        AppUser saved = userRepository.save(user);
        auditService.record("user.activate", actorId, id, "success");
        return toResponse(saved);
    }

    private UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
