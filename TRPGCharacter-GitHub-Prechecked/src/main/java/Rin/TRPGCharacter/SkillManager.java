package Rin.TRPGCharacter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final File file;
    private final LinkedHashMap<String, SkillDefinition> skills = new LinkedHashMap<>();

    private static final Pattern FORMULA =
            Pattern.compile("^(STR|CON|POW|DEX|APP|SIZ|INT|EDU)\\s*\\*\\s*(\\d+)$",
                    Pattern.CASE_INSENSITIVE);

    public SkillManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.file = new File(plugin.getDataFolder(), "skills.yml");

        if (!file.exists()) {
            plugin.saveResource("skills.yml", false);
        }

        reload();
    }

    public void reload() {
        skills.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("skills");

        if (section == null) {
            plugin.getLogger().warning("skills.yml に skills: セクションがありません。");
            return;
        }

        for (String id : section.getKeys(false)) {
            String base = "skills." + id;
            String name = config.getString(base + ".name", id);
            Object rawDefault = config.get(base + ".default", 0);
            String defaultValue = String.valueOf(rawDefault);
            String category = config.getString(base + ".category", "その他");

            skills.put(id, new SkillDefinition(id, name, defaultValue, category));
        }

        // 既存サーバーの古い skills.yml でも戦闘技能を利用できるようにする。
        boolean changed = false;
        changed |= ensureBuiltInSkill(config, "dodge", "回避", "DEX*2", "戦闘技能");
        changed |= ensureBuiltInSkill(config, "kick", "キック", "25", "戦闘技能");
        changed |= ensureBuiltInSkill(config, "fist", "こぶし", "50", "戦闘技能");
        changed |= ensureBuiltInSkill(config, "martial_arts", "マーシャルアーツ", "1", "戦闘技能");
        changed |= ensureBuiltInSkill(config, "sword", "刀剣", "20", "戦闘技能");

        if (changed) {
            try {
                config.save(file);
            } catch (Exception ex) {
                plugin.getLogger().warning("skills.yml の戦闘技能自動追加を保存できませんでした: " + ex.getMessage());
            }
        }
    }

    private boolean ensureBuiltInSkill(YamlConfiguration config,
                                       String id,
                                       String name,
                                       String defaultValue,
                                       String category) {
        if (skills.containsKey(id)) {
            return false;
        }

        skills.put(id, new SkillDefinition(id, name, defaultValue, category));
        String base = "skills." + id;
        config.set(base + ".name", name);

        try {
            config.set(base + ".default", Integer.parseInt(defaultValue));
        } catch (NumberFormatException ignored) {
            // DEX*2 などの能力値式は文字列のまま保存する。
            config.set(base + ".default", defaultValue);
        }

        config.set(base + ".category", category);
        return true;
    }

    public Collection<SkillDefinition> getAllSkills() {
        return Collections.unmodifiableCollection(skills.values());
    }

    public SkillDefinition getSkill(String id) {
        return skills.get(id);
    }

    public boolean hasSkill(String id) {
        return skills.containsKey(id);
    }

    public int getSkillBaseValue(Player player, String id) {
        Integer stored = characterManager.getStoredSkill(player, id);
        if (stored != null) {
            return stored;
        }

        SkillDefinition skill = skills.get(id);
        if (skill == null) {
            return 0;
        }

        return resolveDefault(player, skill.getDefaultValue());
    }

    public int getSkillValue(Player player, String id) {
        int base = getSkillBaseValue(player, id);
        int hobby = characterManager.getHobbyAllocation(player, id);
        int occupation = characterManager.getOccupationAllocation(player, id);
        int growth = characterManager.getSkillGrowth(player, id);

        return Math.min(99, Math.max(0, base + hobby + occupation + growth));
    }

    private int resolveDefault(Player player, String raw) {
        String value = raw.trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = FORMULA.matcher(value);
        if (matcher.matches()) {
            String stat = matcher.group(1).toUpperCase(Locale.ROOT);
            int multiplier = Integer.parseInt(matcher.group(2));
            return characterManager.getStat(player, stat) * multiplier;
        }

        plugin.getLogger().warning("解釈できない技能初期値: " + raw);
        return 0;
    }

    public LinkedHashMap<String, List<SkillDefinition>> groupByCategory() {
        LinkedHashMap<String, List<SkillDefinition>> grouped = new LinkedHashMap<>();
        Set<String> added = new HashSet<>();

        // CoC第6版の探索者シートに近いカテゴリ順・技能順。
        // skills.yml が旧バージョンの並びでも、表示時はこちらを優先する。
        String[] orderedIds = {
                // 戦闘技能
                "dodge", "kick", "fist", "martial_arts", "sword",

                // 探索技能
                "first_aid", "locksmith", "conceal", "hide", "listen",
                "sneak", "photography", "psychoanalysis", "track",
                "library_use", "spot_hidden",

                // 行動技能
                "drive_auto", "mechanical_repair", "operate_heavy_machinery",
                "ride", "swim", "art", "craft", "pilot", "jump",
                "electrical_repair", "navigate", "disguise",

                // 交渉技能
                "fast_talk", "credit_rating", "persuade", "bargain",
                "own_language",

                // 知識技能
                "medicine", "occult", "chemistry", "cthulhu_mythos",
                "accounting", "archaeology", "computer_use", "psychology",
                "anthropology", "biology", "geology", "astronomy",
                "natural_history", "physics", "law", "pharmacy",
                "history", "other_language"
        };

        for (String id : orderedIds) {
            SkillDefinition skill = skills.get(id);
            if (skill == null) {
                continue;
            }

            String category = canonicalCategory(id, skill.getCategory());
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(skill);
            added.add(id);
        }

        // 独自追加技能は消さず、skills.yml 側のカテゴリで末尾に表示する。
        for (SkillDefinition skill : skills.values()) {
            if (added.contains(skill.getId())) {
                continue;
            }
            grouped.computeIfAbsent(skill.getCategory(), k -> new ArrayList<>()).add(skill);
        }

        return grouped;
    }

    private String canonicalCategory(String id, String fallback) {
        if (Set.of("dodge", "kick", "fist", "martial_arts", "sword").contains(id)) {
            return "戦闘技能";
        }

        if (Set.of(
                "first_aid", "locksmith", "conceal", "hide", "listen",
                "sneak", "photography", "psychoanalysis", "track",
                "library_use", "spot_hidden"
        ).contains(id)) {
            return "探索技能";
        }

        if (Set.of(
                "drive_auto", "mechanical_repair", "operate_heavy_machinery",
                "ride", "swim", "art", "craft", "pilot", "jump",
                "electrical_repair", "navigate", "disguise"
        ).contains(id)) {
            return "行動技能";
        }

        if (Set.of(
                "fast_talk", "credit_rating", "persuade", "bargain",
                "own_language"
        ).contains(id)) {
            return "交渉技能";
        }

        if (Set.of(
                "medicine", "occult", "chemistry", "cthulhu_mythos",
                "accounting", "archaeology", "computer_use", "psychology",
                "anthropology", "biology", "geology", "astronomy",
                "natural_history", "physics", "law", "pharmacy",
                "history", "other_language"
        ).contains(id)) {
            return "知識技能";
        }

        return fallback;
    }
}
