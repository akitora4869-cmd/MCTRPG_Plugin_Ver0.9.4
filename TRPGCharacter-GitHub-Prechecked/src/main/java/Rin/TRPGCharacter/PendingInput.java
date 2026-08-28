package Rin.TRPGCharacter;

public record PendingInput(Type type, String id, String displayName) {

    public enum Type {
        STAT,
        SKILL,
        HOBBY_SKILL_ADD,
        CURRENT_SAN,
        CURRENT_HP,
        CURRENT_MP,
        SAN_LOSS,
        HP_DAMAGE,
        HP_HEAL,
        MP_SPEND,
        MP_RECOVER,
        CHARACTER_NAME
    }
}
