package com.tlean07.fiap.challenge.dto;

import com.tlean07.fiap.challenge.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @Email
    @NotBlank
    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(min = 12, max = 128)
    private String password;

    @NotNull
    private UserRole role;
}
