package com.ainovel.app.world.dto;

import java.util.List;

public record WorldUpdateRequest(String name, String tagline, List<String> themes, String creativeIntent, String notes) {}
