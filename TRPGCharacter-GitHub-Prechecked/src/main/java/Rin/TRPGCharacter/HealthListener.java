package Rin.TRPGCharacter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class HealthListener implements Listener {

    private final Plugin plugin;
    private final HealthSyncManager healthSyncManager;

    public HealthListener(Plugin plugin, HealthSyncManager healthSyncManager) {
        this.plugin = plugin;
        this.healthSyncManager = healthSyncManager;
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!plugin.getConfig().getBoolean("health-sync.disable-hunger", true)) {
            return;
        }

        event.setCancelled(true);

        if (event.getEntity() instanceof org.bukkit.entity.Player player) {
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
        }
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) {
            return;
        }

        // Minecraft側で回復した場合もTRPG側へ反映
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    if (player.isOnline() && !player.isDead()) {
                        healthSyncManager.syncFromMinecraft(player);
                    }
                },
                1L
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> healthSyncManager.syncSafeJoin(event.getPlayer()),
                1L
        );
    }
}
