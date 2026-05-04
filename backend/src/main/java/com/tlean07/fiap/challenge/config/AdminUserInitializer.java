package com.tlean07.fiap.challenge.config;

import com.tlean07.fiap.challenge.enums.UserRole;
import com.tlean07.fiap.challenge.model.AppUser;
import com.tlean07.fiap.challenge.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class AdminUserInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    @Bean
    CommandLineRunner createInitialAdmin(AppUserRepository userRepository,
                                         PasswordEncoder passwordEncoder,
                                         @Value("${app.admin.email:}") String email,
                                         @Value("${app.admin.password:}") String password,
                                         @Value("${app.admin.name:Administrador}") String name) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }
            if (email == null || email.isBlank() || password == null || password.length() < 12) {
                log.warn("Nenhum usuario existe. Configure APP_ADMIN_EMAIL e APP_ADMIN_PASSWORD com 12+ caracteres para criar o admin inicial.");
                return;
            }

            AppUser admin = AppUser.builder()
                    .email(email.trim().toLowerCase())
                    .name(name)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(UserRole.ADMIN)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
            log.info("Usuario admin inicial criado: {}", admin.getEmail());
        };
    }
}
