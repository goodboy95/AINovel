package com.ainovel.app.user.dto;

import java.util.Set;
import java.util.UUID;

public record ValidateResponse(UUID id, String username, String email, Set<String> permissions) {}
