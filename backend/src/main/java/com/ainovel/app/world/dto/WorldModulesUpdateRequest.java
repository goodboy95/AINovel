package com.ainovel.app.world.dto;

import java.util.Map;

public record WorldModulesUpdateRequest(Map<String, Map<String, String>> modules) {}
