package com.ainovel.app.world;

import com.ainovel.app.world.dto.WorldDefinitionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/world-building")
public class WorldDefinitionController {
    @Autowired
    private WorldService worldService;

    @GetMapping("/definitions")
    public List<WorldDefinitionDto> definitions() { return worldService.definitions(); }
}
