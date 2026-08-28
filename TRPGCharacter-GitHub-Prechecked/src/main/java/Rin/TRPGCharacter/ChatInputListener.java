package Rin.TRPGCharacter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputListener implements Listener {

    private final Plugin plugin;
    private final InputManager inputManager;

    public ChatInputListener(Plugin plugin, InputManager inputManager) {
        this.plugin = plugin;
        this.inputManager = inputManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!inputManager.hasPending(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> inputManager.handleInput(event.getPlayer(), message)
        );
    }
}
