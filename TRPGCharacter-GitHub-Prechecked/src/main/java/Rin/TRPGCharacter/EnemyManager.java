package Rin.TRPGCharacter;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.io.File;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnemyManager implements Listener {

    private final Plugin plugin;
    private final File file;
    private final Random random = new Random();
    private YamlConfiguration config;

    private static final Pattern DICE =
            Pattern.compile("^(\\d+)[dD](\\d+)(?:([+-])(\\d+))?$");

    public EnemyManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "enemies.yml");

        if (!file.exists()) {
            plugin.saveResource("enemies.yml", false);
        }

        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);

        // reload時には既に存在するMobにも再適用する。
        if (Bukkit.getServer() != null) {
            Bukkit.getScheduler().runTask(plugin, this::applyToLoadedEntities);
        }
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    public EnemyDefinition getDefinition(Entity entity) {
        if (entity == null || entity instanceof Player) {
            return null;
        }

        MythosManager mythosManager = plugin.getMythosManager();
        if (mythosManager != null) {
            EnemyDefinition mythosDefinition = mythosManager.getEnemyDefinition(entity);
            if (mythosDefinition != null) {
                return mythosDefinition;
            }
        }

        String type = entity.getType().name();
        String base = "enemies." + type;
        ConfigurationSection section = config.getConfigurationSection(base);
        if (section == null) {
            return null;
        }

        return new EnemyDefinition(
                type,
                config.getString(base + ".name", type),
                Math.max(1, config.getInt(base + ".hp", 10)),
                clamp(config.getInt(base + ".hit", 50), 0, 100),
                config.getString(base + ".damage", "1d4"),
                Math.max(0, config.getInt(base + ".armor", 0)),
                config.getBoolean(base + ".player-armor", true)
        );
    }

    public Entity resolveSource(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity entity) {
                return entity;
            }
        }

        return damager;
    }

    public int rollDamage(EnemyDefinition definition) {
        return rollExpression(definition.damage(), false);
    }

    public int maxDamage(EnemyDefinition definition) {
        return rollExpression(definition.damage(), true);
    }

    public boolean criticalMaxDamage() {
        return config.getBoolean("result-rules.critical-max-damage", true);
    }

    public int specialBonus() {
        return Math.max(0, config.getInt("result-rules.special-flat-bonus", 1));
    }

    public boolean showRollsToTarget() {
        return config.getBoolean("show-rolls-to-target", true);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isEnabled()) {
            return;
        }

        applyBalance(event.getEntity());
    }

    public void applyToLoadedEntities() {
        if (!isEnabled()) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                applyBalance(entity);
            }
        }
    }

    public void applyBalance(LivingEntity entity) {
        EnemyDefinition definition = getDefinition(entity);
        if (definition == null) {
            return;
        }

        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double oldMax = maxHealth.getValue();
        double oldHealth = entity.getHealth();
        double ratio = oldMax > 0.0 ? oldHealth / oldMax : 1.0;

        maxHealth.setBaseValue(definition.hp());

        // 新規スポーンは満タン、既存Mobのreloadは割合をなるべく維持。
        double nextHealth = Math.max(
                1.0,
                Math.min(definition.hp(), definition.hp() * ratio)
        );
        entity.setHealth(nextHealth);
    }

    private int rollExpression(String expression, boolean maximum) {
        String value = expression == null ? "0" : expression.trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = DICE.matcher(value);
        if (!matcher.matches()) {
            plugin.getLogger().warning("enemies.yml のダイス式を解釈できません: " + expression);
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
            total += maximum ? sides : random.nextInt(sides) + 1;
        }

        return Math.max(0, total);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
