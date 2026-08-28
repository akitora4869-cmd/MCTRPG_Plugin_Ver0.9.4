package Rin.TRPGCharacter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class KeeperListener implements Listener {

    private final KeeperManager keeperManager;
    private final KeeperBookManager keeperBookManager;

    public KeeperListener(KeeperManager keeperManager, KeeperBookManager keeperBookManager) {
        this.keeperManager = keeperManager;
        this.keeperBookManager = keeperBookManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        keeperManager.applyPermission(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!keeperBookManager.isKeeperBook(event.getItem())) return;

        event.setCancelled(true);

        if (!keeperManager.isKeeper(event.getPlayer())) {
            event.getPlayer().sendMessage("§cKP権限がありません。");
            return;
        }

        keeperBookManager.openKeeperBook(event.getPlayer());
    }
}
