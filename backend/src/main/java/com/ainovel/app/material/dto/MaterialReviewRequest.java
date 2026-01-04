package com.ainovel.app.material.dto;

import java.util.List;

public record MaterialReviewRequest(String title, String summary, List<String> tags, String type, String entitiesJson, String reviewNotes) {}
