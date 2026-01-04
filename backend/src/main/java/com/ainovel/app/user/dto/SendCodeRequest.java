package com.ainovel.app.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendCodeRequest(@Email @NotBlank String email, @NotBlank String captchaToken) {}

