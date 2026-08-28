package Rin.TRPGCharacter;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class DiceSoundManager {

    private final Plugin plugin;

    public DiceSoundManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void playRollSequence(Player player, Runnable resultAction) {
        playNearby(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.65f, 0.8f);

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> playNearby(player, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.65f, 0.95f),
                4L
        );

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> playNearby(player, Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 0.70f, 1.05f),
                8L
        );

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> playNearby(player, Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 0.75f, 1.15f),
                12L
        );

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> playNearby(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.55f, 0.7f),
                16L
        );

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                resultAction,
                20L
        );
    }

    public void playResultSound(Player player, CheckResult result) {
        switch (result) {
            case CRITICAL -> {
                playNearby(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.15f);
                plugin.getServer().getScheduler().runTaskLater(
                        plugin,
                        () -> playNearby(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.7f),
                        4L
                );
            }
            case SPECIAL -> {
                playNearby(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
            }
            case SUCCESS -> {
                playNearby(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.25f);
            }
            case FAILURE -> {
                playNearby(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.75f);
            }
            case FUMBLE -> {
                playNearby(player, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.55f);
                plugin.getServer().getScheduler().runTaskLater(
                        plugin,
                        () -> playNearby(player, Sound.ENTITY_WITHER_AMBIENT, 0.35f, 0.65f),
                        3L
                );
            }
        }
    }

    private void playNearby(Player source, Sound sound, float volume, float pitch) {
        double radius = 12.0;

        for (Player target : source.getWorld().getPlayers()) {
            if (target.getLocation().distanceSquared(source.getLocation()) <= radius * radius) {
                target.playSound(source.getLocation(), sound, volume, pitch);
            }
        }
    }
}
