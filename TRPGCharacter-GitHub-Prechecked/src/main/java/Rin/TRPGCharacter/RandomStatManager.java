package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class RandomStatManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final Random random = new Random();

    public RandomStatManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
    }

    public void generate(Player player) {
        Map<String, Integer> stats = new LinkedHashMap<>();

        stats.put("STR", roll(3, 6, 0));
        stats.put("CON", roll(3, 6, 0));
        stats.put("POW", roll(3, 6, 0));
        stats.put("DEX", roll(3, 6, 0));
        stats.put("APP", roll(3, 6, 0));
        stats.put("SIZ", roll(2, 6, 6));
        stats.put("INT", roll(2, 6, 6));
        stats.put("EDU", roll(3, 6, 3));

        characterManager.setStatsBulk(player, stats);
        characterManager.resetCurrentVitalsToMaximum(player);

        player.sendMessage(color("&6[TRPG] &aCoC第6版標準式で能力値を一括生成しました。"));
        player.sendMessage(color("&fSTR " + stats.get("STR")
                + "  CON " + stats.get("CON")
                + "  POW " + stats.get("POW")
                + "  DEX " + stats.get("DEX")));
        player.sendMessage(color("&fAPP " + stats.get("APP")
                + "  SIZ " + stats.get("SIZ")
                + "  INT " + stats.get("INT")
                + "  EDU " + stats.get("EDU")));

        plugin.getSidebarManager().updatePlayer(player);
        plugin.getHealthSyncManager().sync(player);

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> plugin.getBookManager().openSheet(player),
                2L
        );
    }

    private int roll(int dice, int sides, int bonus) {
        int total = bonus;
        for (int i = 0; i < dice; i++) {
            total += random.nextInt(sides) + 1;
        }
        return total;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
