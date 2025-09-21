package com.example.ainovel.service.world;

import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Shared helper for computing module field hashes and resolving status transitions.
 */
final class WorldModuleContentHelper {

    private static final ObjectMapper HASH_OBJECT_MAPPER = new ObjectMapper();

    static {
        HASH_OBJECT_MAPPER.findAndRegisterModules();
        HASH_OBJECT_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private WorldModuleContentHelper() {
    }

    static String computeContentHash(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        try {
            byte[] json = HASH_OBJECT_MAPPER.writeValueAsBytes(fields);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法计算内容哈希", e);
        }
    }

    static WorldModuleStatus determineStatus(WorldModuleDefinitionRegistry definitionRegistry,
                                             World world,
                                             String moduleKey,
                                             Map<String, String> fields,
                                             boolean contentChanged) {
        WorldModuleDefinition definition = definitionRegistry.requireModule(moduleKey);
        boolean anyFilled = false;
        boolean allRequiredFilled = true;
        for (WorldModuleDefinition.FieldDefinition field : definition.fields()) {
            String value = fields.get(field.key());
            if (value != null && !value.isBlank()) {
                anyFilled = true;
            } else if (field.required()) {
                allRequiredFilled = false;
            }
        }
        if (!anyFilled) {
            return WorldModuleStatus.EMPTY;
        }
        if (allRequiredFilled) {
            if (world.getVersion() != null && world.getVersion() > 0 && contentChanged) {
                return WorldModuleStatus.AWAITING_GENERATION;
            }
            return WorldModuleStatus.READY;
        }
        return WorldModuleStatus.IN_PROGRESS;
    }

    static List<String> collectMissingRequiredFields(WorldModuleDefinition definition, Map<String, String> fields) {
        List<String> result = new ArrayList<>();
        for (WorldModuleDefinition.FieldDefinition field : definition.fields()) {
            String value = fields == null ? null : fields.get(field.key());
            if (field.required() && (value == null || value.isBlank())) {
                result.add(field.key());
            }
        }
        return result;
    }
}
