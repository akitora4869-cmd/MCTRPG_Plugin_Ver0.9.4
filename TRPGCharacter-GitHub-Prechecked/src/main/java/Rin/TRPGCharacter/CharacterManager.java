package Rin.TRPGCharacter;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

public class CharacterManager {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration data;

    private static final String[] STATS = {
            "STR", "CON", "POW", "DEX", "APP", "SIZ", "INT", "EDU"
    };

    public CharacterManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public String[] getStats() {
        return STATS.clone();
    }

    public String getCharacterName(Player player) {
        return data.getString(
                path(player.getUniqueId(), "character-name"),
                player.getName()
        );
    }

    public void setCharacterName(Player player, String name) {
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(path(player.getUniqueId(), "character-name"), name);
        save();
    }

    public int getStat(Player player, String stat) {
        String normalized = stat.toUpperCase(Locale.ROOT);
        int fallback = plugin.getConfig().getInt("default-stat-value", 0);
        return data.getInt(path(player.getUniqueId(), "stats." + normalized), fallback);
    }

    public void setStat(Player player, String stat, int value) {
        String normalized = stat.toUpperCase(Locale.ROOT);
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(path(player.getUniqueId(), "stats." + normalized), value);
        save();
    }

    public void setStatsBulk(Player player, java.util.Map<String, Integer> stats) {
        data.set(path(player.getUniqueId(), "name"), player.getName());
        for (java.util.Map.Entry<String, Integer> entry : stats.entrySet()) {
            data.set(path(player.getUniqueId(), "stats." + entry.getKey()), entry.getValue());
        }
        save();
    }

    public void resetCurrentVitalsToMaximum(Player player) {
        setCurrentHp(player, getHp(player));
        setCurrentMp(player, getMp(player));
        setCurrentSan(player, getSan(player));
    }

    public boolean hasConfiguredStats(Player player) {
        for (String stat : STATS) {
            String p = path(player.getUniqueId(), "stats." + stat);
            if (!data.contains(p)) {
                return false;
            }
        }
        return true;
    }

    public boolean isValidStat(String stat) {
        String normalized = stat.toUpperCase(Locale.ROOT);
        for (String s : STATS) {
            if (s.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public Integer getStoredSkill(Player player, String skillId) {
        String p = path(player.getUniqueId(), "skills." + skillId);
        if (!data.contains(p)) {
            return null;
        }
        return data.getInt(p);
    }

    public void setSkill(Player player, String skillId, int value) {
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(path(player.getUniqueId(), "skills." + skillId), value);
        save();
    }

    /**
     * 趣味技能ポイント。CoC第6版標準の INT×10。
     */
    public int getHobbyPointTotal(Player player) {
        return Math.max(0, getStat(player, "INT") * 10);
    }

    public int getHobbyAllocation(Player player, String skillId) {
        return Math.max(0, data.getInt(
                path(player.getUniqueId(), "skill-points.hobby.allocations." + skillId),
                0
        ));
    }

    public int getHobbyPointUsed(Player player) {
        String base = path(player.getUniqueId(), "skill-points.hobby.allocations");
        org.bukkit.configuration.ConfigurationSection section =
                data.getConfigurationSection(base);

        if (section == null) {
            return 0;
        }

        int total = 0;
        for (String skillId : section.getKeys(false)) {
            total += Math.max(0, section.getInt(skillId, 0));
        }
        return total;
    }

    public int getHobbyPointRemaining(Player player) {
        return Math.max(0, getHobbyPointTotal(player) - getHobbyPointUsed(player));
    }

    public boolean addHobbyAllocation(Player player, String skillId, int amount) {
        if (amount <= 0 || amount > getHobbyPointRemaining(player)) {
            return false;
        }

        int current = getHobbyAllocation(player, skillId);
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(
                path(player.getUniqueId(), "skill-points.hobby.allocations." + skillId),
                current + amount
        );
        save();
        return true;
    }

    public void resetHobbyAllocations(Player player) {
        data.set(
                path(player.getUniqueId(), "skill-points.hobby.allocations"),
                null
        );
        save();
    }

    /**
     * 将来の職業実装用の保存領域。
     * 現時点では値の読取だけ用意し、職業ポイントの配布・割振りUIは未実装。
     */
    public int getOccupationAllocation(Player player, String skillId) {
        return Math.max(0, data.getInt(
                path(player.getUniqueId(), "skill-points.occupation.allocations." + skillId),
                0
        ));
    }

    public int getSkillGrowth(Player player, String skillId) {
        return Math.max(0, data.getInt(
                path(player.getUniqueId(), "skill-growth.allocations." + skillId),
                0
        ));
    }

    public void addSkillGrowth(Player player, String skillId, int amount) {
        if (amount <= 0) {
            return;
        }

        int current = getSkillGrowth(player, skillId);
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(
                path(player.getUniqueId(), "skill-growth.allocations." + skillId),
                current + amount
        );
        save();
    }

    public String getUnarmedAttack(Player player) {
        String value = data.getString(
                path(player.getUniqueId(), "combat.unarmed-attack"),
                "fist"
        );

        if ("kick".equalsIgnoreCase(value)) {
            return "kick";
        }

        return "fist";
    }

    public String getUnarmedAttackName(Player player) {
        return "kick".equals(getUnarmedAttack(player)) ? "キック" : "こぶし";
    }

    public void setUnarmedAttack(Player player, String type) {
        String normalized = "kick".equalsIgnoreCase(type) ? "kick" : "fist";
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(path(player.getUniqueId(), "combat.unarmed-attack"), normalized);
        save();
    }

    public int getHp(Player player) {
        int con = getStat(player, "CON");
        int siz = getStat(player, "SIZ");
        return (con + siz + 1) / 2;
    }

    public int getMp(Player player) {
        return getStat(player, "POW");
    }

    public int getCurrentHp(Player player) {
        String p = path(player.getUniqueId(), "current-hp");
        if (!data.contains(p)) {
            return getHp(player);
        }
        return data.getInt(p);
    }

    public boolean isDeadCharacter(Player player) {
        return hasConfiguredStats(player) && getCurrentHp(player) <= 0;
    }

    public void setCurrentHp(Player player, int value) {
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(path(player.getUniqueId(), "current-hp"), value);
        save();
    }

    public int getCurrentMp(Player player) {
        String p = path(player.getUniqueId(), "current-mp");
        if (!data.contains(p)) {
            return getMp(player);
        }
        return data.getInt(p);
    }

    public void setCurrentMp(Player player, int value) {
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(path(player.getUniqueId(), "current-mp"), value);
        save();
    }

    public int getSan(Player player) {
        return getStat(player, "POW") * 5;
    }

    public int getCurrentSan(Player player) {
        String p = path(player.getUniqueId(), "current-san");
        if (!data.contains(p)) {
            return getSan(player);
        }
        return data.getInt(p);
    }

    public void setCurrentSan(Player player, int value) {
        data.set(path(player.getUniqueId(), "name"), player.getName());
        data.set(path(player.getUniqueId(), "current-san"), value);
        save();
    }

    public int getIdea(Player player) {
        return getStat(player, "INT") * 5;
    }

    public int getLuck(Player player) {
        return getStat(player, "POW") * 5;
    }

    public int getKnowledge(Player player) {
        return getStat(player, "EDU") * 5;
    }

    public int getDerived(Player player, String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "idea" -> getIdea(player);
            case "luck" -> getLuck(player);
            case "knowledge" -> getKnowledge(player);
            default -> 0;
        };
    }

    public String getDerivedName(String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "idea" -> "アイデア";
            case "luck" -> "幸運";
            case "knowledge" -> "知識";
            default -> id;
        };
    }

    private String path(UUID uuid, String suffix) {
        return "players." + uuid + "." + suffix;
    }

    public void resetPlayer(Player player) {
        data.set("players." + player.getUniqueId(), null);
        save();
    }

    public void resetAllPlayers() {
        data.set("players", null);
        save();
    }

    public void reload() {
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("players.yml の保存に失敗しました: " + e.getMessage());
        }
    }
}
