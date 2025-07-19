package com.ainovel.app.material.dto;

public record MaterialReviewRequest(String title, String summary, String tags, String type, String entitiesJson, String reviewNotes) {}
