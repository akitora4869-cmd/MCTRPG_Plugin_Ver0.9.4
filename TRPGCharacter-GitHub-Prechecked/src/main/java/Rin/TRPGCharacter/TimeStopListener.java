package Rin.TRPGCharacter;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class TimeStopListener implements Listener {

    private final TimeStopManager timeStopManager;

    public TimeStopListener(TimeStopManager timeStopManager) {
        this.timeStopManager = timeStopManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!timeStopManager.isStopped()) return;
        if (timeStopManager.canAct(event.getPlayer())) return;
        if (event.getTo() == null) return;

        boolean moved = event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ();

        if (moved) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!timeStopManager.isStopped()) return;
        if (timeStopManager.canAct(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!timeStopManager.isStopped()) return;
        if (timeStopManager.canAct(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!timeStopManager.isStopped()) return;
        if (timeStopManager.canAct(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!timeStopManager.isStopped()) return;

        if (event.getEntity() instanceof Player player && timeStopManager.canAct(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!timeStopManager.isStopped()) return;

        if (event.getEntity() instanceof Player player && timeStopManager.canAct(player)) {
            return;
        }

        event.setCancelled(true);
    }
}
