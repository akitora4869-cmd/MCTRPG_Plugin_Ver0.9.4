package Rin.TRPGCharacter;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Locale;

public class ArmorManager {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration config;

    public ArmorManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "armor.yml");

        if (!file.exists()) {
            plugin.saveResource("armor.yml", false);
        }

        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    public int getArmor(Player player) {
        if (!config.getBoolean("enabled", true)) {
            return 0;
        }

        double total = 0.0;
        total += value(player.getInventory().getHelmet(), "HELMET");
        total += value(player.getInventory().getChestplate(), "CHESTPLATE");
        total += value(player.getInventory().getLeggings(), "LEGGINGS");
        total += value(player.getInventory().getBoots(), "BOOTS");

        return Math.max(0, (int) Math.round(total));
    }

    private double value(ItemStack item, String slot) {
        if (item == null || item.getType().isAir()) {
            return 0.0;
        }

        String material = item.getType().name();
        String family = family(material);
        if (family == null) {
            return 0.0;
        }

        double fullSetArmor = config.getDouble("material-values." + family, 0.0);
        double weight = config.getDouble("slot-weights." + slot, 0.0);
        return fullSetArmor * weight;
    }

    private String family(String material) {
        String value = material.toUpperCase(Locale.ROOT);

        if (value.startsWith("LEATHER_")) return "LEATHER";
        if (value.startsWith("GOLDEN_")) return "GOLDEN";
        if (value.startsWith("CHAINMAIL_")) return "CHAINMAIL";
        if (value.startsWith("IRON_")) return "IRON";
        if (value.startsWith("DIAMOND_")) return "DIAMOND";
        if (value.startsWith("NETHERITE_")) return "NETHERITE";

        return null;
    }
}
