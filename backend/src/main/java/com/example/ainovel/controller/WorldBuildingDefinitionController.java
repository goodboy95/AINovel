package com.example.ainovel.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainovel.dto.world.WorldBuildingDefinitionResponse;
import com.example.ainovel.worldbuilding.WorldBuildingDefinitionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/world-building/definitions")
@RequiredArgsConstructor
public class WorldBuildingDefinitionController {

    private final WorldBuildingDefinitionService definitionService;

    @GetMapping
    public WorldBuildingDefinitionResponse getDefinitions() {
        return definitionService.getDefinitions();
    }
}
