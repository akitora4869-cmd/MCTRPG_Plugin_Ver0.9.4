package Rin.TRPGCharacter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class SkillEffectManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final Random random = new Random();

    public SkillEffectManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
    }

    public void applyOnSuccess(Player player, String skillId) {
        switch (skillId) {
            case "first_aid" -> healHp(player, roll(1, 3), "応急手当");
            case "medicine" -> healHp(player, roll(1, 3) + 1, "医学");
            case "psychoanalysis" -> psychoanalysisOther(player, roll(1, 3));
            case "jump" -> applyPotion(player, PotionEffectType.JUMP, 20 * 15, 1, "跳躍力上昇");
            case "swim" -> applyPotion(player, PotionEffectType.WATER_BREATHING, 20 * 30, 0, "水中呼吸");
            case "climb" -> applyClimbEffect(player);
            case "hide" -> applyPotion(player, PotionEffectType.INVISIBILITY, 20 * 10, 0, "透明化");
            case "sneak" -> applyPotion(player, PotionEffectType.SPEED, 20 * 10, 0, "忍び歩き補助");
            case "spot_hidden" -> discoverClues(player);
            case "listen" -> listenNearby(player);
            default -> {
                // この10技能以外は判定のみ。
            }
        }
    }

    private void healHp(Player player, int amount, String source) {
        int before = characterManager.getCurrentHp(player);
        int max = characterManager.getHp(player);
        int after = Math.min(max, before + amount);
        characterManager.setCurrentHp(player, after);

        player.sendMessage(color("&6[TRPG] &a" + source + "成功: HP "
                + before + " → " + after + " (+" + (after - before) + ")"));
        plugin.getSidebarManager().updatePlayer(player);
        plugin.getHealthSyncManager().sync(player);
    }

    private void psychoanalysisOther(Player analyst, int amount) {
        Player target = null;
        double nearest = Double.MAX_VALUE;

        for (Player candidate : analyst.getWorld().getPlayers()) {
            if (candidate.equals(analyst)) {
                continue;
            }

            double distance = candidate.getLocation().distanceSquared(analyst.getLocation());
            if (distance <= 25.0 && distance < nearest) {
                target = candidate;
                nearest = distance;
            }
        }

        if (target == null) {
            analyst.sendMessage(color("&6[TRPG] &e精神分析成功: 5ブロック以内に対象となる他のプレイヤーがいません。"));
            return;
        }

        int before = characterManager.getCurrentSan(target);
        int max = characterManager.getSan(target);
        int after = Math.min(max, before + amount);
        characterManager.setCurrentSan(target, after);

        analyst.sendMessage(color("&6[TRPG] &d精神分析成功: &f" + target.getName()
                + " &dのSAN " + before + " → " + after
                + " (+" + (after - before) + ")"));

        target.sendMessage(color("&6[TRPG] &d" + analyst.getName()
                + " の精神分析を受けました: SAN "
                + before + " → " + after
                + " (+" + (after - before) + ")"));

        plugin.getSidebarManager().updatePlayer(target);
    }

    private void healSan(Player player, int amount) {
        int before = characterManager.getCurrentSan(player);
        int max = characterManager.getSan(player);
        int after = Math.min(max, before + amount);
        characterManager.setCurrentSan(player, after);

        player.sendMessage(color("&6[TRPG] &d精神分析成功: SAN "
                + before + " → " + after + " (+" + (after - before) + ")"));
        plugin.getSidebarManager().updatePlayer(player);
    }

    private void applyPotion(Player player,
                             PotionEffectType type,
                             int durationTicks,
                             int amplifier,
                             String label) {
        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, true, true));
        player.sendMessage(color("&6[TRPG] &a" + label + "の効果を得ました。"));
    }

    private void applyClimbEffect(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 15, 0, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 15, 0, true, true, true));
        player.sendMessage(color("&6[TRPG] &a登攀成功: 15秒間、登攀補助を得ました。"));
    }

    private void discoverClues(Player player) {
        int found = plugin.getClueManager().discoverNearby(player);

        if (found <= 0) {
            player.sendMessage(color("&6[TRPG] &e目星成功: 特に気になる情報は見つかりませんでした。"));
            return;
        }

        player.sendMessage(color("&6[TRPG] &a目星成功: &f" + found + "件 &aの情報ポイントを発見しました。"));
        player.sendMessage(color("&7白いパーティクルが出ている場所を右クリックしてください。"));
    }

    private void glowNearby(Player player) {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : player.getNearbyEntities(16, 16, 16)) {
            if (entity instanceof LivingEntity living && entity != player) {
                targets.add(living);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage(color("&6[TRPG] &e目星成功: 周囲に目立つ対象はありません。"));
            return;
        }

        for (LivingEntity target : targets) {
            target.setGlowing(true);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity target : targets) {
                if (target.isValid()) {
                    target.setGlowing(false);
                }
            }
        }, 20L * 10L);

        player.sendMessage(color("&6[TRPG] &a目星成功: 周囲の対象を10秒間強調表示しました。"));
    }

    private void listenNearby(Player player) {
        List<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : player.getNearbyEntities(24, 12, 24)) {
            if (entity instanceof LivingEntity living && entity != player) {
                targets.add(living);
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage(color("&6[TRPG] &e聞き耳成功: 近くに生物の気配はありません。"));
            return;
        }

        Location origin = player.getLocation();
        targets.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin)));
        LivingEntity nearest = targets.get(0);
        double distance = Math.sqrt(nearest.getLocation().distanceSquared(origin));

        player.sendMessage(color("&6[TRPG] &a聞き耳成功: "
                + targets.size() + "体の気配。最も近い気配は約"
                + Math.max(1, (int) Math.round(distance)) + "ブロック先です。"));
    }

    private int roll(int dice, int sides) {
        int total = 0;
        for (int i = 0; i < dice; i++) {
            total += random.nextInt(sides) + 1;
        }
        return total;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
