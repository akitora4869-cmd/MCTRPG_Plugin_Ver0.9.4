package Rin.TRPGCharacter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeathManager implements Listener {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final BookManager bookManager;
    private final ConcurrentHashMap<UUID, Boolean> returnBook = new ConcurrentHashMap<>();

    public DeathManager(Plugin plugin,
                        CharacterManager characterManager,
                        BookManager bookManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.bookManager = bookManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        boolean hadSheet = false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (bookManager.isCharacterSheet(item)) {
                hadSheet = true;
                break;
            }
        }

        // 探索者シートは死亡ドロップから除外
        event.getDrops().removeIf(bookManager::isCharacterSheet);

        if (hadSheet) {
            returnBook.put(player.getUniqueId(), true);
        }

        // TRPG側は死亡状態として0を維持
        if (characterManager.hasConfiguredStats(player)) {
            characterManager.setCurrentHp(player, 0);
            plugin.getSidebarManager().updatePlayer(player);
        }

        String characterName = characterManager.getCharacterName(player);
        plugin.getServer().broadcast(
                Component.text("探索者 ", NamedTextColor.GRAY)
                        .append(Component.text(characterName, NamedTextColor.RED))
                        .append(Component.text(" は死亡しました。", NamedTextColor.GRAY))
        );
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (characterManager.isDeadCharacter(player)) {
                player.setGameMode(GameMode.SPECTATOR);

                // スペクテイターとして存在するためMinecraft側の体力は安全値を持たせる。
                // TRPG側の現在HPは0のまま。
                plugin.getHealthSyncManager().prepareDeadSpectator(player);

                if (returnBook.remove(player.getUniqueId()) != null && !hasSheet(player)) {
                    player.getInventory().addItem(bookManager.createSheet(player));
                }

                player.sendMessage(Component.text(
                        "探索者は死亡状態です。GMの復活処理を待ってください。",
                        NamedTextColor.RED
                ));
            }
        }, 2L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!characterManager.isDeadCharacter(player)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            player.setGameMode(GameMode.SPECTATOR);
            plugin.getHealthSyncManager().prepareDeadSpectator(player);

            if (!hasSheet(player)) {
                player.getInventory().addItem(bookManager.createSheet(player));
            }

            player.sendMessage(Component.text(
                    "この探索者は死亡状態のため、スペクテイターモードです。",
                    NamedTextColor.RED
            ));
        }, 5L);
    }

    public void revive(Player player) {
        int maxHp = characterManager.getHp(player);

        if (maxHp <= 0) {
            player.sendMessage(Component.text(
                    "能力値が未設定のため復活できません。",
                    NamedTextColor.RED
            ));
            return;
        }

        characterManager.setCurrentHp(player, maxHp);
        player.setGameMode(GameMode.SURVIVAL);
        plugin.getHealthSyncManager().sync(player);
        plugin.getSidebarManager().updatePlayer(player);

        if (!hasSheet(player)) {
            player.getInventory().addItem(bookManager.createSheet(player));
        }

        player.sendMessage(Component.text(
                "探索者が復活しました。HPは最大値まで回復しています。",
                NamedTextColor.GREEN
        ));
    }

    private boolean hasSheet(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (bookManager.isCharacterSheet(item)) {
                return true;
            }
        }
        return false;
    }
}
