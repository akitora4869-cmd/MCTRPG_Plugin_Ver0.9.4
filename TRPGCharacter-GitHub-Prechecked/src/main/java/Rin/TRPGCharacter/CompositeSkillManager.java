package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CompositeSkillManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final Random random = new Random();

    public CompositeSkillManager(Plugin plugin,
                                 CharacterManager characterManager,
                                 SkillManager skillManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.skillManager = skillManager;
    }

    public void roll(Player player, String comboId) {
        switch (comboId) {
            case "medicine_firstaid" ->
                    rollPair(player, comboId, "医学", "medicine", "応急手当", "first_aid");
            case "spot_listen" ->
                    rollPair(player, comboId, "目星", "spot_hidden", "聞き耳", "listen");
            case "hide_sneak" ->
                    rollPair(player, comboId, "隠れる", "hide", "忍び歩き", "sneak");
            case "climb_jump" ->
                    rollPair(player, comboId, "登攀", "climb", "跳躍", "jump");
            default -> player.sendMessage(color("&c複合技能が見つかりません。"));
        }
    }

    private void rollPair(Player player,
                          String comboId,
                          String nameA,
                          String skillA,
                          String nameB,
                          String skillB) {
        int targetA = skillManager.getSkillValue(player, skillA);
        int targetB = skillManager.getSkillValue(player, skillB);

        plugin.getDiceSoundManager().playRollSequence(player, () -> {
            int rollA = random.nextInt(100) + 1;
            int rollB = random.nextInt(100) + 1;

            CheckResult resultA = CheckResult.evaluate(rollA, targetA);
            CheckResult resultB = CheckResult.evaluate(rollB, targetB);

            plugin.getSkillGrowthManager().tryGrowth(
                    player, skillA, nameA, resultA
            );
            plugin.getSkillGrowthManager().tryGrowth(
                    player, skillB, nameB, resultB
            );

            boolean successA = resultA.isSuccess();
            boolean successB = resultB.isSuccess();

            player.sendMessage(color("&6[複合技能] &f" + nameA + "＋" + nameB));
            player.sendMessage(color("&b" + nameA + " &f" + rollA + " / " + targetA
                    + " &7→ " + resultA.color() + resultA.label()));
            player.sendMessage(color("&b" + nameB + " &f" + rollB + " / " + targetB
                    + " &7→ " + resultB.color() + resultB.label()));

            plugin.getDiceSoundManager().playResultSound(
                    player,
                    chooseCompositeSound(resultA, resultB)
            );

            applyResult(player, comboId, successA, successB);
        });
    }

    private CheckResult chooseCompositeSound(CheckResult a, CheckResult b) {
        if (a == CheckResult.FUMBLE || b == CheckResult.FUMBLE) {
            return CheckResult.FUMBLE;
        }
        if (a == CheckResult.CRITICAL || b == CheckResult.CRITICAL) {
            return CheckResult.CRITICAL;
        }
        if (a == CheckResult.SPECIAL || b == CheckResult.SPECIAL) {
            return CheckResult.SPECIAL;
        }
        if (a.isSuccess() || b.isSuccess()) {
            return CheckResult.SUCCESS;
        }
        return CheckResult.FAILURE;
    }

    private void applyResult(Player player, String comboId, boolean a, boolean b) {
        if (!a && !b) {
            player.sendMessage(color("&c結果: 両方失敗"));
            return;
        }

        switch (comboId) {
            case "medicine_firstaid" -> medicalEffect(player, a, b);
            case "spot_listen" -> perceptionEffect(player, a, b);
            case "hide_sneak" -> stealthEffect(player, a, b);
            case "climb_jump" -> movementEffect(player, a, b);
        }
    }

    private void medicalEffect(Player player, boolean medicine, boolean firstAid) {
        int amount;

        if (medicine && firstAid) {
            amount = rollDice(2, 3) + 1;
            player.sendMessage(color("&a結果: 両方成功 → HP 2d3+1 回復"));
        } else if (medicine) {
            amount = rollDice(1, 3) + 1;
            player.sendMessage(color("&a結果: 医学のみ成功 → HP 1d3+1 回復"));
        } else {
            amount = rollDice(1, 3);
            player.sendMessage(color("&a結果: 応急手当のみ成功 → HP 1d3 回復"));
        }

        int before = characterManager.getCurrentHp(player);
        int maxHp = characterManager.getHp(player);
        int after = Math.min(maxHp, before + amount);
        characterManager.setCurrentHp(player, after);

        player.sendMessage(color("&fHP " + before + " → " + after
                + " &7(+" + (after - before) + ")"));

        plugin.getHealthSyncManager().sync(player);
        plugin.getSidebarManager().updatePlayer(player);
    }

    private void perceptionEffect(Player player, boolean spot, boolean listen) {
        if (spot && listen) {
            player.sendMessage(color("&a結果: 両方成功 → 広範囲の気配を強調"));
            glowNearby(player, 24.0, 20L * 15L);
            reportNearby(player, 24.0);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
        } else if (spot) {
            player.sendMessage(color("&a結果: 目星のみ成功 → 周囲を強調"));
            glowNearby(player, 16.0, 20L * 8L);
        } else {
            player.sendMessage(color("&a結果: 聞き耳のみ成功 → 周囲の気配を通知"));
            reportNearby(player, 24.0);
        }
    }

    private void stealthEffect(Player player, boolean hide, boolean sneak) {
        if (hide && sneak) {
            player.sendMessage(color("&a結果: 両方成功 → 20秒間、透明化＋移動補助"));
            addPotion(player, PotionEffectType.INVISIBILITY, 20 * 20, 0);
            addPotion(player, PotionEffectType.SPEED, 20 * 20, 0);
        } else if (hide) {
            player.sendMessage(color("&a結果: 隠れるのみ成功 → 10秒間透明化"));
            addPotion(player, PotionEffectType.INVISIBILITY, 20 * 10, 0);
        } else {
            player.sendMessage(color("&a結果: 忍び歩きのみ成功 → 10秒間移動補助"));
            addPotion(player, PotionEffectType.SPEED, 20 * 10, 0);
        }
    }

    private void movementEffect(Player player, boolean climb, boolean jump) {
        if (climb && jump) {
            player.sendMessage(color("&a結果: 両方成功 → 20秒間、強力な移動補助"));
            addPotion(player, PotionEffectType.JUMP, 20 * 20, 1);
            addPotion(player, PotionEffectType.SLOW_FALLING, 20 * 20, 0);
            addPotion(player, PotionEffectType.SPEED, 20 * 20, 0);
        } else if (climb) {
            player.sendMessage(color("&a結果: 登攀のみ成功 → 15秒間登攀補助"));
            addPotion(player, PotionEffectType.JUMP, 20 * 15, 0);
            addPotion(player, PotionEffectType.SLOW_FALLING, 20 * 15, 0);
        } else {
            player.sendMessage(color("&a結果: 跳躍のみ成功 → 15秒間跳躍力上昇"));
            addPotion(player, PotionEffectType.JUMP, 20 * 15, 1);
        }
    }

    private void glowNearby(Player player, double radius, long duration) {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && entity != player) {
                targets.add(living);
                living.setGlowing(true);
            }
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity target : targets) {
                if (target.isValid()) {
                    target.setGlowing(false);
                }
            }
        }, duration);
    }

    private void reportNearby(Player player, double radius) {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : player.getNearbyEntities(radius, radius / 2.0, radius)) {
            if (entity instanceof LivingEntity living && entity != player) {
                targets.add(living);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage(color("&e周囲に生物の気配はありません。"));
            return;
        }

        targets.sort(Comparator.comparingDouble(
                e -> e.getLocation().distanceSquared(player.getLocation())
        ));

        double distance = Math.sqrt(
                targets.get(0).getLocation().distanceSquared(player.getLocation())
        );

        player.sendMessage(color("&e気配: " + targets.size()
                + "体 / 最短 約" + Math.max(1, (int) Math.round(distance)) + "ブロック"));
    }

    private void addPotion(Player player, PotionEffectType type, int ticks, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, ticks, amplifier, true, true, true));
    }

    private int rollDice(int count, int sides) {
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1;
        }
        return total;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
