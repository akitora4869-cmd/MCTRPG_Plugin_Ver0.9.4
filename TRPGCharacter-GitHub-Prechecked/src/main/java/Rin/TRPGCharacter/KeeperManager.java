package Rin.TRPGCharacter;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KeeperManager {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration data;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public KeeperManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "keepers.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isKeeper(Player player) {
        return data.getBoolean("keepers." + player.getUniqueId() + ".enabled", false);
    }

    public void grantKeeper(Player player) {
        data.set("keepers." + player.getUniqueId() + ".enabled", true);
        data.set("keepers." + player.getUniqueId() + ".name", player.getName());
        save();
        applyPermission(player);
    }

    public void applyPermission(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            player.removeAttachment(old);
        }

        if (!isKeeper(player)) {
            return;
        }

        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission("trpg.keeper", true);
        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("keepers.yml の保存に失敗しました: " + e.getMessage());
        }
    }
}
