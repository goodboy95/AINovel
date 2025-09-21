package com.example.ainovel.service.world;

import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;

import java.util.List;

public record WorldAggregate(World world, List<WorldModule> modules) {
}
