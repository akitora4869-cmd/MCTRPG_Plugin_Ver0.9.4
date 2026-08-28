package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SwimManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final Random random = new Random();

    private final Map<UUID, Boolean> wasInWater = new HashMap<>();
    private final Map<UUID, Boolean> swimSucceeded = new HashMap<>();

    private BukkitTask task;

    public SwimManager(Plugin plugin,
                       CharacterManager characterManager,
                       SkillManager skillManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.skillManager = skillManager;
    }

    public void start() {
        shutdown();

        long interval = Math.max(
                5L,
                plugin.getConfig().getLong("auto-swim.check-interval", 10L)
        );

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tick,
                interval,
                interval
        );
    }

    public void reload() {
        wasInWater.clear();
        swimSucceeded.clear();
        start();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("auto-swim.enabled", true)) {
            return;
        }

        int effectTicks = Math.max(
                40,
                plugin.getConfig().getInt("auto-swim.effect-refresh-ticks", 80)
        );

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();

            if (!canUse(player)) {
                wasInWater.put(id, false);
                swimSucceeded.put(id, false);
                continue;
            }

            boolean inWater = player.isInWater();
            boolean previouslyInWater = wasInWater.getOrDefault(id, false);

            // 水から出たら、次回の入水時にもう一度判定できる。
            if (!inWater) {
                wasInWater.put(id, false);
                swimSucceeded.put(id, false);
                continue;
            }

            // 成功済みなら水中にいる間だけ泳ぎ強化を更新する。
            if (swimSucceeded.getOrDefault(id, false)) {
                applySwimBoost(player, effectTicks);
                wasInWater.put(id, true);
                continue;
            }

            // 同じ水中に居続けている間は、失敗しても自動再判定しない。
            if (previouslyInWater) {
                continue;
            }

            wasInWater.put(id, true);
            rollSwim(player, effectTicks);
        }
    }

    private boolean canUse(Player player) {
        return player.isOnline()
                && player.getGameMode() != GameMode.SPECTATOR
                && !characterManager.isDeadCharacter(player)
                && characterManager.hasConfiguredStats(player)
                && !plugin.getTimeStopManager().isStopped();
    }

    private void rollSwim(Player player, int effectTicks) {
        String skillId = plugin.getConfig().getString(
                "auto-swim.skill",
                "swim"
        );

        int value = skillManager.getSkillValue(player, skillId);

        plugin.getDiceSoundManager().playRollSequence(player, () -> {
            if (!canUse(player)) {
                return;
            }

            // ダイス演出中に水から出た場合は中止。
            if (!player.isInWater()) {
                wasInWater.put(player.getUniqueId(), false);
                swimSucceeded.put(player.getUniqueId(), false);
                return;
            }

            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, value);

            SkillDefinition growthSkill = skillManager.getSkill(skillId);
            plugin.getSkillGrowthManager().tryGrowth(
                    player,
                    skillId,
                    growthSkill != null ? growthSkill.getName() : skillId,
                    result
            );

            player.sendMessage(color(
                    "&3[水中] &e水泳判定"
                            + " &7/ 技能値 &b" + value
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            ));

            plugin.getDiceSoundManager().playResultSound(player, result);

            if (!result.isSuccess()) {
                swimSucceeded.put(player.getUniqueId(), false);
                player.sendMessage(color(
                        "&7水泳に失敗しました。水から出て入り直すと再判定できます。"
                ));
                return;
            }

            swimSucceeded.put(player.getUniqueId(), true);
            applySwimBoost(player, effectTicks);

            player.sendMessage(color(
                    "&a水泳成功！ &7水中で素早く泳げるようになりました。"
            ));
        });
    }

    public void onManualSwimSuccess(Player player) {
        if (!plugin.getConfig().getBoolean("auto-swim.enabled", true)
                || !canUse(player)
                || !player.isInWater()) {
            return;
        }

        String configuredSkill = plugin.getConfig().getString(
                "auto-swim.skill",
                "swim"
        );

        if (!"swim".equalsIgnoreCase(configuredSkill)) {
            return;
        }

        UUID id = player.getUniqueId();
        if (swimSucceeded.getOrDefault(id, false)) {
            return;
        }

        int effectTicks = Math.max(
                40,
                plugin.getConfig().getInt("auto-swim.effect-refresh-ticks", 80)
        );

        wasInWater.put(id, true);
        swimSucceeded.put(id, true);
        applySwimBoost(player, effectTicks);

        player.sendMessage(color(
                "&a水泳成功！ &7水中で素早く泳げるようになりました。"
        ));
    }

    private void applySwimBoost(Player player, int durationTicks) {
        int amplifier = Math.max(
                0,
                plugin.getConfig().getInt("auto-swim.dolphins-grace-amplifier", 0)
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.DOLPHINS_GRACE,
                        durationTicks,
                        amplifier,
                        false,
                        false,
                        false
                ),
                true
        );
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
