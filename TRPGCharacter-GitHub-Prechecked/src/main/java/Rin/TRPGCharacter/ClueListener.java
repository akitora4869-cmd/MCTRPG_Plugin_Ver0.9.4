package Rin.TRPGCharacter;

import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class ClueListener implements Listener {

    private final ClueManager clueManager;

    public ClueListener(ClueManager clueManager) {
        this.clueManager = clueManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;
        if (!clueManager.hasClue(stand)) return;
        if (!clueManager.canInspect(event.getPlayer(), stand)) return;

        event.setCancelled(true);
        clueManager.showInfo(event.getPlayer(), stand);
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) {
            return;
        }

        if (clueManager.isProtected(stand)) {
            event.setCancelled(true);
        }
    }

}
