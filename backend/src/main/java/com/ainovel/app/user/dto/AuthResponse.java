package com.ainovel.app.user.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(String token, UUID id, String username, String email, Set<String> roles) {}
