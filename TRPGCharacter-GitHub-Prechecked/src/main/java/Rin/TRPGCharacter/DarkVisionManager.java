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

public class DarkVisionManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final Random random = new Random();

    private final Map<UUID, Boolean> wasDark = new HashMap<>();
    private final Map<UUID, Boolean> adapted = new HashMap<>();

    private BukkitTask task;

    public DarkVisionManager(Plugin plugin,
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
                plugin.getConfig().getLong("dark-vision.check-interval", 10L)
        );

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tick,
                interval,
                interval
        );
    }

    public void reload() {
        wasDark.clear();
        adapted.clear();
        start();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("dark-vision.enabled", true)) {
            return;
        }

        int threshold = Math.max(
                0,
                Math.min(15, plugin.getConfig().getInt("dark-vision.light-level-threshold", 4))
        );

        int effectTicks = Math.max(
                40,
                plugin.getConfig().getInt("dark-vision.effect-refresh-ticks", 80)
        );

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();

            if (!canUse(player)) {
                wasDark.put(id, false);
                adapted.put(id, false);
                continue;
            }

            int light = player.getEyeLocation().getBlock().getLightLevel();
            boolean dark = light <= threshold;
            boolean previouslyDark = wasDark.getOrDefault(id, false);

            // 明るい場所に出たら次の暗所侵入で再判定できるようにする。
            if (!dark) {
                wasDark.put(id, false);
                adapted.put(id, false);
                continue;
            }

            // すでに目が慣れている間は暗視を短時間ずつ更新する。
            // 明るい場所に出れば更新が止まり、数秒で自然に解除される。
            if (adapted.getOrDefault(id, false)) {
                applyNightVision(player, effectTicks);
                wasDark.put(id, true);
                continue;
            }

            // 同じ暗所に居続けている間は失敗しても連続判定しない。
            if (previouslyDark) {
                continue;
            }

            wasDark.put(id, true);
            rollSpotHidden(player, light, effectTicks);
        }
    }

    private boolean canUse(Player player) {
        return player.isOnline()
                && player.getGameMode() != GameMode.SPECTATOR
                && !characterManager.isDeadCharacter(player)
                && characterManager.hasConfiguredStats(player)
                && !plugin.getTimeStopManager().isStopped();
    }

    private void rollSpotHidden(Player player, int lightLevel, int effectTicks) {
        String skillId = plugin.getConfig().getString(
                "dark-vision.skill",
                "spot_hidden"
        );

        int value = skillManager.getSkillValue(player, skillId);

        plugin.getDiceSoundManager().playRollSequence(player, () -> {
            if (!canUse(player)) {
                return;
            }

            // ダイス演出中に明るい場所へ出た場合は判定を中止する。
            int threshold = Math.max(
                    0,
                    Math.min(15, plugin.getConfig().getInt(
                            "dark-vision.light-level-threshold",
                            4
                    ))
            );

            int currentLight = player.getEyeLocation().getBlock().getLightLevel();
            if (currentLight > threshold) {
                wasDark.put(player.getUniqueId(), false);
                adapted.put(player.getUniqueId(), false);
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
                    "&8[暗所] &e目星判定"
                            + " &7/ 技能値 &b" + value
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            ));

            plugin.getDiceSoundManager().playResultSound(player, result);

            if (!result.isSuccess()) {
                adapted.put(player.getUniqueId(), false);
                return;
            }

            adapted.put(player.getUniqueId(), true);
            applyNightVision(player, effectTicks);

            player.sendMessage(color(
                    "&a目星成功！ &7暗闇に目が慣れ、周囲が見えるようになりました。"
            ));
        });
    }

    public void onManualSpotHiddenSuccess(Player player) {
        if (!plugin.getConfig().getBoolean("dark-vision.enabled", true)
                || !canUse(player)) {
            return;
        }

        String configuredSkill = plugin.getConfig().getString(
                "dark-vision.skill",
                "spot_hidden"
        );

        // 現在の暗視用技能が目星でない設定なら、この救済処理は行わない。
        if (!"spot_hidden".equalsIgnoreCase(configuredSkill)) {
            return;
        }

        int threshold = Math.max(
                0,
                Math.min(15, plugin.getConfig().getInt(
                        "dark-vision.light-level-threshold",
                        4
                ))
        );

        int light = player.getEyeLocation().getBlock().getLightLevel();
        if (light > threshold) {
            return;
        }

        UUID id = player.getUniqueId();

        // すでに暗所へ適応済みなら重複メッセージを出さない。
        if (adapted.getOrDefault(id, false)) {
            return;
        }

        int effectTicks = Math.max(
                40,
                plugin.getConfig().getInt(
                        "dark-vision.effect-refresh-ticks",
                        80
                )
        );

        wasDark.put(id, true);
        adapted.put(id, true);
        applyNightVision(player, effectTicks);

        player.sendMessage(color(
                "&a目星成功！ &7暗闇に目が慣れ、周囲が見えるようになりました。"
        ));
    }

    private void applyNightVision(Player player, int durationTicks) {
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.NIGHT_VISION,
                        durationTicks,
                        0,
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
