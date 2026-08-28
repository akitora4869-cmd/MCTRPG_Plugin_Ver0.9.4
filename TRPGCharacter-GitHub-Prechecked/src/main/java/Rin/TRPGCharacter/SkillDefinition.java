package Rin.TRPGCharacter;

public class SkillDefinition {
    private final String id;
    private final String name;
    private final String defaultValue;
    private final String category;

    public SkillDefinition(String id, String name, String defaultValue, String category) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getCategory() {
        return category;
    }
}
