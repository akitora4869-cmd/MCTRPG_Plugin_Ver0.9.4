package Rin.TRPGCharacter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class JoinListener implements Listener {

    private final Plugin plugin;
    private final BookManager bookManager;
    private final JoinGuideManager joinGuideManager;

    public JoinListener(Plugin plugin, BookManager bookManager, JoinGuideManager joinGuideManager) {
        this.plugin = plugin;
        this.bookManager = bookManager;
        this.joinGuideManager = joinGuideManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> joinGuideManager.sendJoinGuide(event.getPlayer()),
                30L
        );

        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }

        for (ItemStack item : event.getPlayer().getInventory().getContents()) {
            if (bookManager.isCharacterSheet(item)) {
                return;
            }
        }

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    event.getPlayer().getInventory().addItem(bookManager.createSheet(event.getPlayer()));
                    event.getPlayer().sendMessage("§6[TRPG] §a探索者シートを配布しました。");
                },
                20L
        );
    }
}
