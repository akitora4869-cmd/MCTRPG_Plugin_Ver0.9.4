package Rin.TRPGCharacter;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class HealthSyncManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;

    public HealthSyncManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
    }

    public void sync(Player player) {
        if (characterManager.isDeadCharacter(player)) {
            prepareDeadSpectator(player);
            return;
        }

        int maxHp = characterManager.getHp(player);

        // 能力値未設定時は安全HPを使用
        if (maxHp <= 0) {
            maxHp = plugin.getConfig().getInt("health-sync.safe-hp", 4);
        }

        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.max(1.0, maxHp));
        }

        int currentHp = characterManager.getCurrentHp(player);

        if (currentHp <= 0 && characterManager.getHp(player) <= 0) {
            currentHp = plugin.getConfig().getInt("health-sync.safe-hp", 4);
        }

        double clamped = Math.max(0.0, Math.min(maxHp, currentHp));

        if (clamped <= 0.0 && characterManager.getHp(player) <= 0) {
            clamped = plugin.getConfig().getInt("health-sync.safe-hp", 4);
        }

        player.setHealth(Math.min(clamped, player.getMaxHealth()));
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
    }

    public void syncFromMinecraft(Player player) {
        int calculatedMax = characterManager.getHp(player);

        // 能力値未設定中はTRPG側へ逆同期しない
        if (calculatedMax <= 0) {
            syncSafeJoin(player);
            return;
        }

        int minecraftHp = (int) Math.ceil(player.getHealth());
        minecraftHp = Math.max(0, Math.min(calculatedMax, minecraftHp));

        characterManager.setCurrentHp(player, minecraftHp);
        plugin.getSidebarManager().updatePlayer(player);
    }

    public void prepareDeadSpectator(Player player) {
        int maxHp = characterManager.getHp(player);
        int safe = Math.max(1, plugin.getConfig().getInt("health-sync.safe-hp", 4));

        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(Math.max(safe, maxHp));
        }

        // Minecraft側で0にすると再死亡するため、スペクテイター中だけ安全な実体力を持たせる。
        double minecraftSafeHealth = Math.min(
                Math.max(1.0, safe),
                player.getMaxHealth()
        );

        player.setHealth(minecraftSafeHealth);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
    }

    public void syncSafeJoin(Player player) {
        if (characterManager.isDeadCharacter(player)) {
            player.setGameMode(GameMode.SPECTATOR);
            prepareDeadSpectator(player);
            return;
        }

        int calculatedMax = characterManager.getHp(player);

        if (calculatedMax <= 0) {
            int safe = plugin.getConfig().getInt("health-sync.safe-hp", 4);

            AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(safe);
            }

            player.setHealth(safe);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            return;
        }

        sync(player);
    }
}
