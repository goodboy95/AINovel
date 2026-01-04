package com.ainovel.app.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterV2Request(
        @Email @NotBlank String email,
        @NotBlank String code,
        @NotBlank String username,
        @NotBlank String password
) {}

