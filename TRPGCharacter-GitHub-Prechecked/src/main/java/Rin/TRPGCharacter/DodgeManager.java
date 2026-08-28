package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class DodgeManager {

    private final Plugin plugin;
    private final SkillManager skillManager;
    private final File file;
    private final Random random = new Random();

    private final Map<UUID, Deque<Long>> recentDodges = new ConcurrentHashMap<>();
    private YamlConfiguration config;

    public DodgeManager(Plugin plugin, SkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.file = new File(plugin.getDataFolder(), "weapons.yml");
        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        recentDodges.clear();
    }

    public DodgeRoll roll(Player player) {
        String skillId = config.getString("dodge.skill", "dodge");
        int baseValue = skillManager.getSkillValue(player, skillId);

        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1000L,
                config.getLong("dodge.chain-window-seconds", 10L) * 1000L);
        int penaltyPerUse = Math.max(0,
                config.getInt("dodge.penalty-per-extra-use", 20));
        int minimum = Math.max(1,
                config.getInt("dodge.minimum-success-value", 1));

        Deque<Long> queue = recentDodges.computeIfAbsent(
                player.getUniqueId(), id -> new ArrayDeque<>()
        );

        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() >= windowMillis) {
                queue.removeFirst();
            }

            int priorAttempts = queue.size();
            int penalty = priorAttempts * penaltyPerUse;
            int effective = Math.max(minimum, baseValue - penalty);

            queue.addLast(now);

            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, effective);

            SkillDefinition growthSkill = skillManager.getSkill(skillId);
            plugin.getSkillGrowthManager().tryGrowth(
                    player,
                    skillId,
                    growthSkill != null ? growthSkill.getName() : skillId,
                    result
            );

            player.sendMessage(color(
                    "&b[回避判定] &7基本 &f" + baseValue
                            + (penalty > 0
                            ? " &7/ 連続ペナルティ &c-" + penalty
                            : "")
                            + " &7/ 成功値 &b" + effective
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            ));

            plugin.getDiceSoundManager().playResultSound(player, result);

            return new DodgeRoll(
                    baseValue,
                    effective,
                    penalty,
                    priorAttempts + 1,
                    roll,
                    result
            );
        }
    }

    public boolean isEnabled() {
        return config.getBoolean("dodge.enabled", true);
    }

    public boolean criticalCounterAttack() {
        return config.getBoolean("dodge.critical-counterattack", true);
    }

    public record DodgeRoll(
            int baseValue,
            int effectiveValue,
            int penalty,
            int chainNumber,
            int roll,
            CheckResult result
    ) {
        public boolean avoided() {
            return result.isSuccess();
        }

        public boolean critical() {
            return result == CheckResult.CRITICAL;
        }
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
