package Rin.TRPGCharacter;

public record EnemyDefinition(
        String entityType,
        String name,
        int hp,
        int hit,
        String damage,
        int armor,
        boolean playerArmorApplies
) {
}
