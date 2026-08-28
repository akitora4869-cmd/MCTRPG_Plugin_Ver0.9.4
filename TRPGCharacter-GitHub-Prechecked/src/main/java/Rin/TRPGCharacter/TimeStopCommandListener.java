package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TimeStopCommandListener implements Listener {

    private final TimeStopManager timeStopManager;

    public TimeStopCommandListener(TimeStopManager timeStopManager) {
        this.timeStopManager = timeStopManager;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim();

        // Vanilla/Paper の /stop と競合するため、/stop time だけ先に横取りする
        if (!message.equalsIgnoreCase("/stop time")) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();

        if (!timeStopManager.canAct(player)) {
            player.sendMessage(ChatColor.RED + "KPまたは管理者のみ使用できます。");
            return;
        }

        timeStopManager.toggle(player);
    }
}
