package Rin.TRPGCharacter;

public record WeaponDefinition(
        String id,
        String name,
        String skillId,
        String damage,
        boolean damageBonus,
        boolean martialArts
) {
}
