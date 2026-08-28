package Rin.TRPGCharacter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class SidebarManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;

    public SidebarManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
    }

    public void start() {
        long interval = plugin.getConfig().getLong("sidebar.update-interval", 20L);
        if (interval < 20L) {
            interval = 20L;
        }

        Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateAll,
                20L,
                interval
        );
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("sidebar.enabled", true)) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        if (!plugin.getConfig().getBoolean("sidebar.enabled", true)) {
            return;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        String serverName = plugin.getConfig().getString("sidebar.server-name", "MCTRPG Sever");
        Objective objective = scoreboard.registerNewObjective(
                "trpg_status",
                "dummy",
                color("&6&l" + serverName)
        );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(color("&fオンライン &a"
                + Bukkit.getOnlinePlayers().size()
                + "&7 / &f"
                + Bukkit.getMaxPlayers())).setScore(7);

        objective.getScore(" ").setScore(6);

        objective.getScore(color("&e" + characterManager.getCharacterName(player))).setScore(5);

        objective.getScore(color("&cHP &f"
                + characterManager.getCurrentHp(player)
                + "&7 / &f"
                + characterManager.getHp(player))).setScore(4);

        objective.getScore(color("&9MP &f"
                + characterManager.getCurrentMp(player)
                + "&7 / &f"
                + characterManager.getMp(player))).setScore(3);

        objective.getScore(color("&dSAN &f"
                + characterManager.getCurrentSan(player)
                + "&7 / &f"
                + characterManager.getSan(player))).setScore(2);

        objective.getScore(color("&b装甲 &f"
                + plugin.getArmorManager().getArmor(player))).setScore(1);

        player.setScoreboard(scoreboard);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
