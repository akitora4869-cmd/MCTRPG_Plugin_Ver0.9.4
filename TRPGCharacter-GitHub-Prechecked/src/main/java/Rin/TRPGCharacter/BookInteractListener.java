package Rin.TRPGCharacter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BookInteractListener implements Listener {

    private final BookManager bookManager;

    public BookInteractListener(BookManager bookManager) {
        this.bookManager = bookManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!bookManager.isCharacterSheet(item)) {
            return;
        }

        event.setCancelled(true);
        bookManager.openSheet(event.getPlayer());
    }
}
