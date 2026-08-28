package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClueManager {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration config;
    private final NamespacedKey clueKey;
    private final NamespacedKey hiddenKey;
    private final NamespacedKey protectedKey;

    private final Map<UUID, Map<UUID, Long>> discovered = new ConcurrentHashMap<>();

    public ClueManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clues.yml");
        this.clueKey = new NamespacedKey(plugin, "clue_id");
        this.hiddenKey = new NamespacedKey(plugin, "clue_hidden");
        this.protectedKey = new NamespacedKey(plugin, "clue_protected");

        if (!file.exists()) {
            plugin.saveResource("clues.yml", false);
        }

        reload();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickParticles, 10L, 10L);
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean clueExists(String id) {
        return config.contains(id);
    }

    public void markArmorStand(ArmorStand stand, String clueId) {
        stand.getPersistentDataContainer().set(clueKey, PersistentDataType.STRING, clueId);
    }

    public int hideNearby(Player player, double radius) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof ArmorStand stand)) continue;

            stand.setInvisible(true);
            stand.getPersistentDataContainer().set(hiddenKey, PersistentDataType.BYTE, (byte) 1);
            count++;
        }
        return count;
    }

    public int showNearby(Player player, double radius) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof ArmorStand stand)) continue;

            stand.setInvisible(false);
            stand.getPersistentDataContainer().remove(hiddenKey);
            count++;
        }
        return count;
    }

    public int protectNearby(Player player, double radius) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof ArmorStand stand)) continue;

            stand.getPersistentDataContainer().set(protectedKey, PersistentDataType.BYTE, (byte) 1);
            stand.setInvulnerable(true);
            count++;
        }
        return count;
    }

    public int unprotectNearby(Player player, double radius) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof ArmorStand stand)) continue;

            stand.getPersistentDataContainer().remove(protectedKey);
            stand.setInvulnerable(false);
            count++;
        }
        return count;
    }

    public int setupNearby(Player player, double radius) {
        int hidden = hideNearby(player, radius);
        protectNearby(player, radius);
        return hidden;
    }

    public boolean isProtected(Entity entity) {
        Byte value = entity.getPersistentDataContainer().get(protectedKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public void restorePersistentState(ArmorStand stand) {
        Byte hidden = stand.getPersistentDataContainer().get(hiddenKey, PersistentDataType.BYTE);
        Byte protect = stand.getPersistentDataContainer().get(protectedKey, PersistentDataType.BYTE);

        if (hidden != null && hidden == (byte) 1) {
            stand.setInvisible(true);
        }

        if (protect != null && protect == (byte) 1) {
            stand.setInvulnerable(true);
        }
    }

    public String getClueId(Entity entity) {
        return entity.getPersistentDataContainer().get(clueKey, PersistentDataType.STRING);
    }

    public boolean hasClue(Entity entity) {
        return getClueId(entity) != null;
    }

    public int discoverNearby(Player player) {
        int found = 0;
        long now = System.currentTimeMillis();

        for (Entity entity : player.getNearbyEntities(16, 16, 16)) {
            if (!(entity instanceof ArmorStand stand)) continue;

            String id = getClueId(stand);
            if (id == null || !config.contains(id)) continue;

            double range = config.getDouble(id + ".range", 10.0);
            if (stand.getLocation().distanceSquared(player.getLocation()) > range * range) continue;

            long duration = config.getLong(id + ".duration", 30L) * 1000L;
            discovered.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                    .put(stand.getUniqueId(), now + duration);
            found++;
        }
        return found;
    }

    public boolean canInspect(Player player, Entity entity) {
        Map<UUID, Long> map = discovered.get(player.getUniqueId());
        if (map == null) return false;
        long until = map.getOrDefault(entity.getUniqueId(), 0L);
        return until > System.currentTimeMillis();
    }

    public void showInfo(Player player, Entity entity) {
        if (!canInspect(player, entity)) return;

        String id = getClueId(entity);
        if (id == null || !config.contains(id)) return;

        player.sendMessage(color("&f------------------------------"));
        player.sendMessage(color("&b[情報] &f" + config.getString(id + ".display-name", id)));
        for (String line : config.getStringList(id + ".text")) {
            player.sendMessage(color("&7" + line));
        }
        player.sendMessage(color("&f------------------------------"));
    }

    private void tickParticles() {
        long now = System.currentTimeMillis();
        Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 1.0f);

        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                restorePersistentState(stand);
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Map<UUID, Long> map = discovered.get(player.getUniqueId());
            if (map == null || map.isEmpty()) continue;

            Iterator<Map.Entry<UUID, Long>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> e = it.next();
                if (e.getValue() <= now) {
                    it.remove();
                    continue;
                }

                Entity target = null;
                for (Entity entity : player.getWorld().getEntities()) {
                    if (entity.getUniqueId().equals(e.getKey())) {
                        target = entity;
                        break;
                    }
                }

                if (target == null || !target.isValid()) {
                    it.remove();
                    continue;
                }

                Location loc = target.getLocation().clone().add(0, 1.4, 0);
                player.spawnParticle(Particle.REDSTONE, loc, 8, 0.35, 0.45, 0.35, 0.0, white);
            }
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
