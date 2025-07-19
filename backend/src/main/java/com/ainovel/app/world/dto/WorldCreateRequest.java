package com.ainovel.app.world.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record WorldCreateRequest(@NotBlank String name, String tagline, List<String> themes, String creativeIntent, String notes) {}
