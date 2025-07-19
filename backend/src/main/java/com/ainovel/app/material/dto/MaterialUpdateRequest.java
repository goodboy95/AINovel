package com.ainovel.app.material.dto;

import java.util.List;

public record MaterialUpdateRequest(String title,
                                    String type,
                                    String summary,
                                    String content,
                                    List<String> tags,
                                    String status,
                                    String entitiesJson) {}
