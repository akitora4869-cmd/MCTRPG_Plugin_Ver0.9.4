package Rin.TRPGCharacter;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeaponManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final File file;
    private YamlConfiguration config;
    private final Random random = new Random();

    private static final Pattern DICE =
            Pattern.compile("^(\\d+)[dD](\\d+)(?:([+-])(\\d+))?$");

    public WeaponManager(Plugin plugin,
                         CharacterManager characterManager,
                         SkillManager skillManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.skillManager = skillManager;
        this.file = new File(plugin.getDataFolder(), "weapons.yml");

        if (!file.exists()) {
            plugin.saveResource("weapons.yml", false);
        }

        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    public WeaponDefinition resolve(Player player) {
        Material material = player.getInventory().getItemInMainHand().getType();

        if (material == Material.AIR) {
            return resolveUnarmed(characterManager.getUnarmedAttack(player));
        }

        String key = material.name();
        ConfigurationSection section = config.getConfigurationSection("weapons." + key);
        if (section == null) {
            return null;
        }

        return readDefinition(
                "weapons." + key,
                key,
                "sword",
                "1d6",
                true
        );
    }

    public WeaponDefinition resolveUnarmed(String type) {
        if ("kick".equalsIgnoreCase(type)) {
            return readDefinition("KICK", "キック", "kick", "1d6", true);
        }

        return readDefinition("FIST", "こぶし", "fist", "1d3", true);
    }

    public int getSkillValue(Player player, WeaponDefinition weapon) {
        return skillManager.getSkillValue(player, weapon.skillId());
    }

    public int rollWeaponDamage(WeaponDefinition weapon) {
        return rollExpression(weapon.damage(), false);
    }

    public int maxWeaponDamage(WeaponDefinition weapon) {
        return rollExpression(weapon.damage(), true);
    }

    public int rollDamageBonus(Player player) {
        int total = characterManager.getStat(player, "STR")
                + characterManager.getStat(player, "SIZ");

        if (total <= 12) {
            return -(random.nextInt(6) + 1);
        }
        if (total <= 16) {
            return -(random.nextInt(4) + 1);
        }
        if (total <= 24) {
            return 0;
        }
        if (total <= 32) {
            return random.nextInt(4) + 1;
        }
        if (total <= 40) {
            return random.nextInt(6) + 1;
        }

        int dice = 2 + Math.max(0, (total - 41) / 16);
        int value = 0;
        for (int i = 0; i < dice; i++) {
            value += random.nextInt(6) + 1;
        }
        return value;
    }

    public int maxDamageBonus(Player player) {
        int total = characterManager.getStat(player, "STR")
                + characterManager.getStat(player, "SIZ");

        if (total <= 12) return -1;
        if (total <= 16) return -1;
        if (total <= 24) return 0;
        if (total <= 32) return 4;
        if (total <= 40) return 6;

        int dice = 2 + Math.max(0, (total - 41) / 16);
        return dice * 6;
    }

    public String getDamageBonusLabel(Player player) {
        int total = characterManager.getStat(player, "STR")
                + characterManager.getStat(player, "SIZ");

        if (total <= 12) return "-1d6";
        if (total <= 16) return "-1d4";
        if (total <= 24) return "0";
        if (total <= 32) return "+1d4";
        if (total <= 40) return "+1d6";

        int dice = 2 + Math.max(0, (total - 41) / 16);
        return "+" + dice + "d6";
    }

    public boolean criticalMaxDamage() {
        return config.getBoolean("result-rules.critical-max-damage", true);
    }

    public int specialBonus() {
        return Math.max(0, config.getInt("result-rules.special-flat-bonus", 1));
    }

    public boolean allowPvp() {
        return config.getBoolean("allow-player-vs-player", true);
    }

    public boolean martialArtsEnabled() {
        return config.getBoolean("martial-arts.enabled", true);
    }

    public String martialArtsSkillId() {
        return config.getString("martial-arts.skill", "martial_arts");
    }

    public int martialArtsMultiplier() {
        return Math.max(1, config.getInt("martial-arts.damage-multiplier", 2));
    }

    private WeaponDefinition readDefinition(String path,
                                            String fallbackName,
                                            String fallbackSkill,
                                            String fallbackDamage,
                                            boolean fallbackDb) {
        String base;
        if (path.equals("FIST")) {
            base = "fist";
        } else if (path.equals("KICK")) {
            base = "kick";
        } else {
            base = path;
        }

        return new WeaponDefinition(
                path,
                config.getString(base + ".name", fallbackName),
                config.getString(base + ".skill", fallbackSkill),
                config.getString(base + ".damage", fallbackDamage),
                config.getBoolean(base + ".damage-bonus", fallbackDb),
                config.getBoolean(base + ".martial-arts", true)
        );
    }

    private int rollExpression(String expression, boolean maximum) {
        String value = expression.trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = DICE.matcher(value);
        if (!matcher.matches()) {
            plugin.getLogger().warning("weapons.yml のダイス式を解釈できません: " + expression);
            return 0;
        }

        int count = Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));
        int modifier = 0;

        if (matcher.group(3) != null && matcher.group(4) != null) {
            int raw = Integer.parseInt(matcher.group(4));
            modifier = matcher.group(3).equals("-") ? -raw : raw;
        }

        if (count < 1 || count > 100 || sides < 1 || sides > 100000) {
            return 0;
        }

        int result = modifier;
        for (int i = 0; i < count; i++) {
            result += maximum ? sides : random.nextInt(sides) + 1;
        }

        return result;
    }
}
