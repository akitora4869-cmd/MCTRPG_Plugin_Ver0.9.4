package Rin.TRPGCharacter;

import java.util.List;

public record ArtifactDefinition(
        String id,
        String name,
        String material,
        List<String> lore,
        boolean activeEnabled,
        String activeEffectType,
        int cooldownSeconds,
        String sanCost,
        double range,
        int durationSeconds,
        int mythosDamageReduction
) {
}
