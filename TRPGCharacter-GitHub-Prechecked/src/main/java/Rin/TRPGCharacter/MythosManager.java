package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MythosManager {

    private static final Pattern DICE =
            Pattern.compile("^(\\d+)[dD](\\d+)(?:([+-])(\\d+))?$");

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final File file;
    private final NamespacedKey mythosIdKey;
    private final Random random = new Random();

    private final Map<String, MythosCreatureDefinition> definitions = new HashMap<>();
    private final Map<UUID, Set<String>> encountered = new HashMap<>();

    private YamlConfiguration config;
    private BukkitTask encounterTask;

    public MythosManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.file = new File(plugin.getDataFolder(), "mythos-creatures.yml");
        this.mythosIdKey = new NamespacedKey(plugin, "mythos_creature_id");

        if (!file.exists()) {
            plugin.saveResource("mythos-creatures.yml", false);
        }

        reload();
    }

    public void start() {
        shutdown();

        long interval = Math.max(
                5L,
                config.getLong("encounter-check-interval", 10L)
        );

        encounterTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tickEncounters,
                interval,
                interval
        );
    }

    public void shutdown() {
        if (encounterTask != null) {
            encounterTask.cancel();
            encounterTask = null;
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        definitions.clear();

        ConfigurationSection root = config.getConfigurationSection("mythos-creatures");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                String base = "mythos-creatures." + id;
                String entityType = config.getString(base + ".entity", "DROWNED")
                        .toUpperCase();

                definitions.put(id.toLowerCase(), new MythosCreatureDefinition(
                        id.toLowerCase(),
                        entityType,
                        config.getString(base + ".name", id),
                        Math.max(1, config.getInt(base + ".hp", 10)),
                        clamp(config.getInt(base + ".hit", 50), 0, 100),
                        config.getString(base + ".damage", "1d4"),
                        Math.max(0, config.getInt(base + ".armor", 0)),
                        config.getBoolean(base + ".player-armor", true),
                        config.getString(base + ".san-check.success", "0"),
                        config.getString(base + ".san-check.failure", "1d6"),
                        Math.max(1.0, config.getDouble(base + ".encounter-range", 12.0)),
                        config.getBoolean(base + ".require-line-of-sight", true)
                ));
            }
        }

        if (encounterTask != null) {
            start();
        }
    }

    public Set<String> getIds() {
        return new java.util.TreeSet<>(definitions.keySet());
    }

    public MythosCreatureDefinition getDefinition(String id) {
        if (id == null) {
            return null;
        }
        return definitions.get(id.toLowerCase());
    }

    public MythosCreatureDefinition getDefinition(Entity entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPersistentDataContainer().get(
                mythosIdKey,
                PersistentDataType.STRING
        );

        return getDefinition(id);
    }

    public EnemyDefinition getEnemyDefinition(Entity entity) {
        MythosCreatureDefinition mythos = getDefinition(entity);
        return mythos == null ? null : mythos.asEnemyDefinition();
    }

    public LivingEntity summon(Player summoner, String id) {
        MythosCreatureDefinition definition = getDefinition(id);
        if (definition == null) {
            return null;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(definition.entityType());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning(
                    "mythos-creatures.yml のentityが不正です: "
                            + definition.id() + " -> " + definition.entityType()
            );
            return null;
        }

        if (!type.isAlive()) {
            return null;
        }

        Location location = summoner.getLocation().clone()
                .add(summoner.getLocation().getDirection().setY(0).normalize().multiply(2.5));

        Entity spawned = summoner.getWorld().spawnEntity(location, type);
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            return null;
        }

        living.getPersistentDataContainer().set(
                mythosIdKey,
                PersistentDataType.STRING,
                definition.id()
        );

        living.setCustomName(color("&3" + definition.name()));
        living.setCustomNameVisible(true);
        living.setRemoveWhenFarAway(false);

        applyBalance(living, definition);

        return living;
    }

    public void applyBalance(LivingEntity entity, MythosCreatureDefinition definition) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(definition.hp());
            entity.setHealth(definition.hp());
        }
    }

    private void tickEncounters() {
        if (!config.getBoolean("enabled", true)) {
            return;
        }

        if (plugin.getTimeStopManager() != null
                && plugin.getTimeStopManager().isStopped()) {
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!canCheck(player)) {
                continue;
            }

            for (Entity entity : player.getNearbyEntities(32.0, 32.0, 32.0)) {
                MythosCreatureDefinition definition = getDefinition(entity);
                if (definition == null || !(entity instanceof LivingEntity living)
                        || living.isDead() || !living.isValid()) {
                    continue;
                }

                if (player.getLocation().distanceSquared(entity.getLocation())
                        > definition.encounterRange() * definition.encounterRange()) {
                    continue;
                }

                if (definition.requireLineOfSight() && !player.hasLineOfSight(entity)) {
                    continue;
                }

                Set<String> seen = encountered.computeIfAbsent(
                        player.getUniqueId(),
                        ignored -> new HashSet<>()
                );

                if (!seen.add(definition.id())) {
                    continue;
                }

                rollSanCheck(player, definition);
            }
        }
    }

    private boolean canCheck(Player player) {
        return player.isOnline()
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR
                && !characterManager.isDeadCharacter(player)
                && characterManager.hasConfiguredStats(player);
    }

    private void rollSanCheck(Player player, MythosCreatureDefinition definition) {
        int san = Math.max(0, characterManager.getCurrentSan(player));

        player.sendMessage(color(
                "&5[神話生物] &f" + definition.name()
                        + " &7を目撃した！ SANチェックを行います。"
        ));

        plugin.getDiceSoundManager().playRollSequence(player, () -> {
            if (!player.isOnline() || !canCheck(player)) {
                return;
            }

            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, san);

            player.sendMessage(color(
                    "&5[SANチェック] &f" + definition.name()
                            + " &7/ 現在SAN &b" + san
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            ));

            plugin.getDiceSoundManager().playResultSound(player, result);

            String expression = result.isSuccess()
                    ? definition.sanSuccess()
                    : definition.sanFailure();

            int loss = rollExpression(expression);
            if (loss <= 0) {
                player.sendMessage(color(
                        "&aSAN減少なし &7(" + expression + ")"
                ));
                return;
            }

            int before = characterManager.getCurrentSan(player);
            int after = Math.max(0, before - loss);
            characterManager.setCurrentSan(player, after);

            player.sendMessage(color(
                    "&c[SAN減少] &f" + expression
                            + " &7→ &c" + loss
                            + " &7/ SAN &f" + before + " → " + after
            ));

            plugin.getSidebarManager().updatePlayer(player);
        });
    }

    private int rollExpression(String expression) {
        String value = expression == null ? "0" : expression.trim();

        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = DICE.matcher(value);
        if (!matcher.matches()) {
            plugin.getLogger().warning(
                    "mythos-creatures.yml のダイス式を解釈できません: " + expression
            );
            return 0;
        }

        int count = Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));
        int modifier = 0;

        if (matcher.group(3) != null && matcher.group(4) != null) {
            int raw = Integer.parseInt(matcher.group(4));
            modifier = matcher.group(3).equals("-") ? -raw : raw;
        }

        if (count < 1 || count > 100 || sides < 1 || sides > 100000) {
            return 0;
        }

        int total = modifier;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1;
        }

        return Math.max(0, total);
    }

    public void resetEncounter(Player player) {
        encountered.remove(player.getUniqueId());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
