package Rin.TRPGCharacter;

import org.bukkit.GameMode;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DangerEffectManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;

    public DangerEffectManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
    }

    public void start() {
        long interval = plugin.getConfig().getLong("danger-effects.check-interval", 40L);
        if (interval < 20L) {
            interval = 20L;
        }

        plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tick,
                40L,
                interval
        );
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("danger-effects.enabled", true)) {
            return;
        }

        if (plugin.getTimeStopManager().isStopped()) {
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR
                    || characterManager.isDeadCharacter(player)) {
                clearDangerEffects(player);
                continue;
            }

            applyHpEffects(player);
            applySanEffects(player);
        }
    }

    private void applyHpEffects(Player player) {
        int maxHp = characterManager.getHp(player);
        if (maxHp <= 0) {
            return;
        }

        int currentHp = characterManager.getCurrentHp(player);
        double ratio = currentHp / (double) maxHp;

        if (ratio <= 0.10) {
            // 危篤: 強い心拍 + 視界悪化
            heartbeat(player, 0.55f, 0.70f);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DARKNESS, 60, 0, true, false, false
            ));
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW, 60, 0, true, false, false
            ));
        } else if (ratio <= 0.25) {
            // 重傷: 速めの心拍 + 軽い視界悪化
            heartbeat(player, 0.45f, 0.85f);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DARKNESS, 40, 0, true, false, false
            ));
        } else if (ratio <= 0.50) {
            // 負傷: 控えめな心拍
            heartbeat(player, 0.35f, 1.00f);
        }
    }

    private void applySanEffects(Player player) {
        int maxSan = characterManager.getSan(player);
        if (maxSan <= 0) {
            return;
        }

        int currentSan = characterManager.getCurrentSan(player);
        double ratio = currentSan / (double) maxSan;

        if (ratio <= 0.10) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.9f, 0.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.55f);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.CONFUSION, 100, 1, true, false, false
            ));
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DARKNESS, 80, 0, true, false, false
            ));
        } else if (ratio <= 0.25) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.6f, 0.7f);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.CONFUSION, 70, 0, true, false, false
            ));
        } else if (ratio <= 0.50) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.35f, 0.9f);
        }
    }

    private void clearDangerEffects(Player player) {
        player.removePotionEffect(PotionEffectType.CONFUSION);
        player.removePotionEffect(PotionEffectType.DARKNESS);
        player.removePotionEffect(PotionEffectType.SLOW);
    }

    private void heartbeat(Player player, float volume, float pitch) {
        player.playSound(
                player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASEDRUM,
                volume,
                pitch
        );

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (player.isOnline()) {
                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_NOTE_BLOCK_BASEDRUM,
                                volume * 0.85f,
                                pitch * 1.08f
                        );
                    }
                },
                5L
        );
    }
}
