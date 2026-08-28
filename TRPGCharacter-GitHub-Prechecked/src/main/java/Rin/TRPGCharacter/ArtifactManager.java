package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactManager implements Listener {

    private static final Pattern DICE =
            Pattern.compile("^(\\d+)[dD](\\d+)(?:([+-])(\\d+))?$");

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final File file;
    private final NamespacedKey artifactIdKey;
    private final Map<String, ArtifactDefinition> definitions = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Random random = new Random();

    private YamlConfiguration config;

    public ArtifactManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.file = new File(plugin.getDataFolder(), "artifacts.yml");
        this.artifactIdKey = new NamespacedKey(plugin, "artifact_id");

        if (!file.exists()) {
            plugin.saveResource("artifacts.yml", false);
        }

        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        definitions.clear();

        ConfigurationSection root = config.getConfigurationSection("artifacts");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            String base = "artifacts." + id;

            List<String> lore = config.getStringList(base + ".description");

            definitions.put(id.toLowerCase(), new ArtifactDefinition(
                    id.toLowerCase(),
                    config.getString(base + ".name", id),
                    config.getString(base + ".material", "PAPER"),
                    lore,
                    config.getBoolean(base + ".active.enabled", false),
                    config.getString(base + ".active.effect.type", "NONE"),
                    Math.max(0, config.getInt(base + ".active.cooldown-seconds", 0)),
                    config.getString(base + ".active.san-cost", "0"),
                    Math.max(0.0, config.getDouble(base + ".active.effect.range", 0.0)),
                    Math.max(0, config.getInt(base + ".active.effect.duration-seconds", 0)),
                    Math.max(0, config.getInt(base + ".passive.mythos-damage-reduction", 0))
            ));
        }
    }

    public TreeSet<String> getIds() {
        return new TreeSet<>(definitions.keySet());
    }

    public ArtifactDefinition getDefinition(String id) {
        if (id == null) {
            return null;
        }
        return definitions.get(id.toLowerCase());
    }

    public ArtifactDefinition getDefinition(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        String id = item.getItemMeta().getPersistentDataContainer().get(
                artifactIdKey,
                PersistentDataType.STRING
        );

        return getDefinition(id);
    }

    public ItemStack createItem(String id) {
        ArtifactDefinition definition = getDefinition(id);
        if (definition == null) {
            return null;
        }

        Material material = Material.matchMaterial(definition.material());
        if (material == null) {
            plugin.getLogger().warning(
                    "artifacts.yml のmaterialが不正です: "
                            + definition.id() + " -> " + definition.material()
            );
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(color("&5" + definition.name()));

        List<String> lore = new ArrayList<>();
        for (String line : definition.lore()) {
            lore.add(color("&7" + line));
        }

        if (definition.activeEnabled()) {
            lore.add("");
            lore.add(color("&d右クリックで使用"));
            if (definition.cooldownSeconds() > 0) {
                lore.add(color("&8クールダウン: " + definition.cooldownSeconds() + "秒"));
            }
            if (!"0".equals(definition.sanCost())) {
                lore.add(color("&8SAN消費: " + definition.sanCost()));
            }
        }

        if (definition.mythosDamageReduction() > 0) {
            lore.add(color(
                    "&8所持中: 神話生物からのダメージ -"
                            + definition.mythosDamageReduction()
            ));
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(
                artifactIdKey,
                PersistentDataType.STRING,
                definition.id()
        );

        item.setItemMeta(meta);
        return item;
    }

    public int getMythosDamageReduction(Player player) {
        int best = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            ArtifactDefinition definition = getDefinition(item);
            if (definition == null) {
                continue;
            }

            best = Math.max(best, definition.mythosDamageReduction());
        }

        return best;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        ArtifactDefinition definition = getDefinition(item);
        if (definition == null || !definition.activeEnabled()) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!characterManager.hasConfiguredStats(player)
                || characterManager.isDeadCharacter(player)) {
            player.sendMessage(color("&cこの状態ではアーティファクトを使用できません。"));
            return;
        }

        if (plugin.getTimeStopManager().isStopped()
                && !plugin.getTimeStopManager().canAct(player)) {
            player.sendMessage(color("&c時間停止中はアーティファクトを使用できません。"));
            return;
        }

        long remaining = getRemainingCooldown(player, definition.id());
        if (remaining > 0) {
            player.sendMessage(color(
                    "&c" + definition.name()
                            + " はあと " + remaining + " 秒で使用できます。"
            ));
            return;
        }

        int sanCost = roll(definition.sanCost());
        int currentSan = characterManager.getCurrentSan(player);

        if (sanCost > currentSan) {
            player.sendMessage(color("&cSANが足りないため使用できません。"));
            return;
        }

        if (sanCost > 0) {
            int afterSan = Math.max(0, currentSan - sanCost);
            characterManager.setCurrentSan(player, afterSan);
            plugin.getSidebarManager().updatePlayer(player);

            player.sendMessage(color(
                    "&5[アーティファクト] &fSAN "
                            + currentSan + " → " + afterSan
                            + " &7(-" + sanCost + ")"
            ));
        }

        boolean activated = activate(player, definition);
        if (!activated) {
            return;
        }

        armCooldown(player, definition);

        player.sendMessage(color(
                "&5[アーティファクト] &f"
                        + definition.name() + " &dが発動した。"
        ));
    }

    private boolean activate(Player player, ArtifactDefinition definition) {
        String type = definition.activeEffectType().toUpperCase();

        if ("REPEL_MYTHOS".equals(type)) {
            return repelMythos(player, definition);
        }

        player.sendMessage(color(
                "&cこのアーティファクトの効果タイプは未実装です: "
                        + definition.activeEffectType()
        ));
        return false;
    }

    private boolean repelMythos(Player player, ArtifactDefinition definition) {
        double range = definition.range();
        int affected = 0;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }

            if (plugin.getMythosManager().getDefinition(entity) == null) {
                continue;
            }

            Vector away = living.getLocation().toVector()
                    .subtract(player.getLocation().toVector());

            if (away.lengthSquared() < 0.0001) {
                away = player.getLocation().getDirection().clone();
            }

            away.normalize().multiply(1.15);
            away.setY(Math.max(0.25, away.getY() + 0.25));

            living.setVelocity(away);
            living.setGlowing(true);
            affected++;

            int ticks = Math.max(1, definition.durationSeconds()) * 20;
            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        if (living.isValid() && !living.isDead()) {
                            living.setGlowing(false);
                        }
                    },
                    ticks
            );
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_BEACON_ACTIVATE,
                1.0f,
                1.4f
        );

        player.sendMessage(color(
                "&d古き印の力が周囲へ広がった。 &7対象: &f" + affected + "体"
        ));

        return true;
    }

    private long getRemainingCooldown(Player player, String artifactId) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long next = map.getOrDefault(artifactId, 0L);

        if (now >= next) {
            return 0;
        }

        return Math.max(1L, (next - now + 999L) / 1000L);
    }

    private void armCooldown(Player player, ArtifactDefinition definition) {
        if (definition.cooldownSeconds() <= 0) {
            return;
        }

        long next = System.currentTimeMillis()
                + definition.cooldownSeconds() * 1000L;

        cooldowns
                .computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(definition.id(), next);
    }

    private int roll(String expression) {
        if (expression == null || expression.isBlank()) {
            return 0;
        }

        String value = expression.trim();

        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = DICE.matcher(value);
        if (!matcher.matches()) {
            plugin.getLogger().warning(
                    "artifacts.yml のダイス式を解釈できません: " + expression
            );
            return 0;
        }

        int count = Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));

        if (count < 1 || count > 100 || sides < 1 || sides > 100000) {
            return 0;
        }

        int modifier = 0;
        if (matcher.group(3) != null && matcher.group(4) != null) {
            int raw = Integer.parseInt(matcher.group(4));
            modifier = "-".equals(matcher.group(3)) ? -raw : raw;
        }

        int total = modifier;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1;
        }

        return Math.max(0, total);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
