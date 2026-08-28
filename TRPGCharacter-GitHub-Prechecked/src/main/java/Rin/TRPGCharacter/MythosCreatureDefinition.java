package Rin.TRPGCharacter;

public record MythosCreatureDefinition(
        String id,
        String entityType,
        String name,
        int hp,
        int hit,
        String damage,
        int armor,
        boolean playerArmorApplies,
        String sanSuccess,
        String sanFailure,
        double encounterRange,
        boolean requireLineOfSight
) {
    public EnemyDefinition asEnemyDefinition() {
        return new EnemyDefinition(
                entityType,
                name,
                hp,
                hit,
                damage,
                armor,
                playerArmorApplies
        );
    }
}
