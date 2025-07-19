package com.ainovel.app.material.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record MaterialCreateRequest(@NotBlank String title,
                                    @NotBlank String type,
                                    String summary,
                                    @NotBlank String content,
                                    List<String> tags) {}
