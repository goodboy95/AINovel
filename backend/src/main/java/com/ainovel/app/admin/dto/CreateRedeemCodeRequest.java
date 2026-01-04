package com.ainovel.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateRedeemCodeRequest(@NotBlank String code, @Positive int amount) {}

