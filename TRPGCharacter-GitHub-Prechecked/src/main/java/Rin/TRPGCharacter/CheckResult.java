package Rin.TRPGCharacter;

public enum CheckResult {
    CRITICAL("★ 決定的成功 / CRITICAL ★", "&6"),
    SPECIAL("◆ スペシャル / SPECIAL ◆", "&b"),
    SUCCESS("○ 成功", "&a"),
    FAILURE("× 失敗", "&c"),
    FUMBLE("☠ 致命的失敗 / FUMBLE ☠", "&4");

    private final String label;
    private final String color;

    CheckResult(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    public boolean isSuccess() {
        return this == CRITICAL || this == SPECIAL || this == SUCCESS;
    }

    public static CheckResult evaluate(int roll, int target) {
        if (roll >= 1 && roll <= 5) {
            return CRITICAL;
        }

        if (roll >= 96 && roll <= 100) {
            return FUMBLE;
        }

        int specialThreshold = Math.max(0, target / 5);
        if (specialThreshold > 0 && roll <= specialThreshold && roll <= target) {
            return SPECIAL;
        }

        if (roll <= target) {
            return SUCCESS;
        }

        return FAILURE;
    }
}
