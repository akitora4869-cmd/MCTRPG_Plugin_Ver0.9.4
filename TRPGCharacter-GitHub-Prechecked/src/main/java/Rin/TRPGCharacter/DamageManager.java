package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.io.File;
import java.util.Random;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DamageManager implements Listener {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final ArmorManager armorManager;
    private final SkillManager skillManager;
    private final Random random = new Random();
    private final File file;
    private YamlConfiguration config;

    // Player UUID -> damage cause -> next allowed timestamp(ms)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private static final Pattern DICE =
            Pattern.compile("^(\\d+)[dD](\\d+)(?:\\+(\\d+))?$");

    public DamageManager(Plugin plugin,
                         CharacterManager characterManager,
                         ArmorManager armorManager,
                         SkillManager skillManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.armorManager = armorManager;
        this.skillManager = skillManager;
        this.file = new File(plugin.getDataFolder(), "damage.yml");

        if (!file.exists()) {
            plugin.saveResource("damage.yml", false);
        }

        reload();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (plugin.getTimeStopManager().isStopped()) {
            event.setCancelled(true);
            return;
        }

        if (!config.getBoolean("enabled", true)) {
            return;
        }

        if (!characterManager.hasConfiguredStats(player)) {
            return;
        }

        if (characterManager.isDeadCharacter(player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            handleCocFallDamage(player, event);
            return;
        }

        // ベリー・サボテン・マグマブロックなど、Minecraft側で非常に短い間隔で
        // 発生する接触系ダメージはTRPG HPへ変換せず、完全に0ダメージとして扱う。
        // 古いdamage.ymlをそのまま使っているサーバーでも安全になるよう、
        // 設定項目が存在しない場合は CONTACT / HOT_FLOOR を既定で無効化する。
        if (isZeroDamageCause(event.getCause())) {
            event.setCancelled(true);
            return;
        }

        if (isOnCooldown(player, event)) {
            event.setCancelled(true);
            return;
        }

        /*
         * 継続ダメージは「ダメージ適用後」ではなく、最初のイベントを
         * 受け付けた時点でクールダウンを開始する。
         *
         * CONTACT（サボテン・スイートベリー等）は非常に短い間隔で
         * EntityDamageEvent が発生するため、後段でクールダウンを
         * 設定すると連続してTRPG HPが減る場合がある。
         *
         * また、装甲で最終ダメージが0になった場合でも、その接触自体は
         * クールダウン対象として扱う。
         */
        armCooldown(player, event);

        DamageDefinition definition = resolveDamage(event);

        int rolledDamage = Math.max(0, roll(definition.expression()));
        boolean armorApplies = resolveArmorApplies(event);
        int armor = armorApplies ? armorManager.getArmor(player) : 0;
        int damage = Math.max(0, rolledDamage - armor);

        event.setCancelled(true);

        // 最終ダメージが0なら表示しない。
        if (damage <= 0) {
            return;
        }

        int before = characterManager.getCurrentHp(player);
        int after = Math.max(0, before - damage);

        characterManager.setCurrentHp(player, after);

        String armorText = armorApplies
                ? " &7/ 装甲 &b" + armor + " &7/ 最終 &c" + damage
                : " &7/ 装甲無効 / 最終 &c" + damage;

        player.sendMessage(color(
                "&6[ダメージ] &f" + definition.name()
                        + " &7" + definition.expression()
                        + " → &c" + rolledDamage
                        + armorText
                        + " &7/ HP &f" + before + " → " + after
        ));

        applyHpAfterDamage(player, after);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNaturalRegeneration(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!config.getBoolean("disable-natural-regeneration", true)) {
            return;
        }

        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    private boolean isZeroDamageCause(EntityDamageEvent.DamageCause cause) {
        List<String> configured = config.getStringList("zero-damage-causes");

        if (configured.isEmpty()) {
            return cause == EntityDamageEvent.DamageCause.CONTACT
                    || cause == EntityDamageEvent.DamageCause.HOT_FLOOR;
        }

        for (String value : configured) {
            if (value != null && value.equalsIgnoreCase(cause.name())) {
                return true;
            }
        }

        return false;
    }

    private boolean isOnCooldown(Player player, EntityDamageEvent event) {
        long seconds = getCooldownSeconds(event);
        if (seconds <= 0) {
            return false;
        }

        String key = cooldownKey(event);
        long now = System.currentTimeMillis();

        Map<String, Long> playerMap = cooldowns.get(player.getUniqueId());
        if (playerMap == null) {
            return false;
        }

        long nextAllowed = playerMap.getOrDefault(key, 0L);
        return now < nextAllowed;
    }

    private void armCooldown(Player player, EntityDamageEvent event) {
        long seconds = getCooldownSeconds(event);
        if (seconds <= 0) {
            return;
        }

        String key = cooldownKey(event);
        long nextAllowed = System.currentTimeMillis() + (seconds * 1000L);

        cooldowns
                .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(key, nextAllowed);
    }

    private long getCooldownSeconds(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();

        // FIRE と FIRE_TICK はMinecraft上で交互に発生することがあるため、
        // 炎上系として同じクールタイムを共有する。
        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            long fireTick = config.getLong("environment.FIRE_TICK.cooldown-seconds", 3L);
            return config.getLong("environment.FIRE.cooldown-seconds", fireTick);
        }

        String env = "environment." + cause.name();
        return config.getLong(env + ".cooldown-seconds", 0L);
    }

    private String cooldownKey(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();

        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            return "BURN";
        }

        return cause.name();
    }

    private void handleCocFallDamage(Player player, EntityDamageEvent event) {
        String base = "environment.FALL";

        double safeBlocks = Math.max(0.0, config.getDouble(base + ".safe-blocks", 3.0));
        double blocksPerDie = Math.max(0.1, config.getDouble(base + ".blocks-per-die", 3.0));
        int maxDice = Math.max(1, config.getInt(base + ".max-dice", 8));
        int jumpReduction = Math.max(0, config.getInt(base + ".jump-success-reduce-dice", 1));

        // Bukkitで着地時にfallDistanceが小さくなる場合に備え、
        // Minecraftの元落下ダメージからも距離を推定する。
        double distance = Math.max(player.getFallDistance(), event.getDamage() + 3.0);

        if (distance <= safeBlocks) {
            return;
        }

        int originalDice = (int) Math.ceil((distance - safeBlocks) / blocksPerDie);
        originalDice = Math.max(1, Math.min(maxDice, originalDice));

        String jumpSkillId = config.getString(base + ".jump-skill", "jump");
        int jumpValue = skillManager.getSkillValue(player, jumpSkillId);

        final double fallDistance = distance;
        final int fallDice = originalDice;

        plugin.getDiceSoundManager().playRollSequence(player, () -> {
            if (!player.isOnline() || characterManager.isDeadCharacter(player)) {
                return;
            }

            int jumpRoll = random.nextInt(100) + 1;
            CheckResult jumpResult = CheckResult.evaluate(jumpRoll, jumpValue);

            SkillDefinition growthSkill = skillManager.getSkill(jumpSkillId);
            plugin.getSkillGrowthManager().tryGrowth(
                    player,
                    jumpSkillId,
                    growthSkill != null ? growthSkill.getName() : jumpSkillId,
                    jumpResult
            );

            player.sendMessage(color(
                    "&e[跳躍判定] &7技能値 &b" + jumpValue
                            + " &7/ 1d100:&e" + jumpRoll
                            + " &7→ " + jumpResult.color() + jumpResult.label()
            ));
            plugin.getDiceSoundManager().playResultSound(player, jumpResult);

            int finalDice = Math.max(
                    0,
                    fallDice - (jumpResult.isSuccess() ? jumpReduction : 0)
            );

            if (jumpResult.isSuccess() && jumpReduction > 0) {
                player.sendMessage(color(
                        "&a跳躍成功！ &7落下ダメージを &f"
                                + fallDice + "d6 → " + finalDice + "d6"
                                + " &7に軽減しました。"
                ));
            }

            // ダメージが0なら「落下ダメージ」表示を出さない。
            if (finalDice <= 0) {
                return;
            }

            int rolledDamage = rollDice(finalDice, 6);
            if (rolledDamage <= 0) {
                return;
            }

            int before = characterManager.getCurrentHp(player);
            int after = Math.max(0, before - rolledDamage);
            characterManager.setCurrentHp(player, after);

            player.sendMessage(color(
                    "&6[落下ダメージ] &f"
                            + String.format("%.1f", fallDistance)
                            + "ブロック &7/ &f" + finalDice + "d6"
                            + " → &c" + rolledDamage
                            + " &7/ HP &f" + before + " → " + after
            ));

            applyHpAfterDamage(player, after);
        });
    }

    private int rollDice(int count, int sides) {
        if (count <= 0 || sides <= 0) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1;
        }
        return total;
    }

    private void applyHpAfterDamage(Player player, int after) {
        plugin.getSidebarManager().updatePlayer(player);

        if (after <= 0) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && !player.isDead()) {
                    player.setHealth(0.0);
                }
            });
        } else {
            plugin.getHealthSyncManager().sync(player);
        }
    }

    private double number(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }

        return fallback;
    }

    private boolean resolveArmorApplies(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity source = resolveSourceEntity(byEntity.getDamager());

            if (source != null) {
                String base = "mobs." + source.getType().name();
                if (config.contains(base + ".armor")) {
                    return config.getBoolean(base + ".armor", true);
                }
            }
        }

        String env = "environment." + event.getCause().name();
        if (config.contains(env + ".armor")) {
            return config.getBoolean(env + ".armor", false);
        }

        // モブの直接/飛び道具攻撃は既定で装甲有効。
        // 環境ダメージはdamage.ymlで明示した場合のみ有効。
        return event instanceof EntityDamageByEntityEvent;
    }

    private DamageDefinition resolveDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity source = resolveSourceEntity(byEntity.getDamager());

            if (source != null) {
                String type = source.getType().name();
                String base = "mobs." + type;

                if (config.contains(base + ".damage")) {
                    return new DamageDefinition(
                            config.getString(base + ".name", type),
                            config.getString(base + ".damage", "1d3")
                    );
                }
            }
        }

        String cause = event.getCause().name();
        String env = "environment." + cause;

        if (config.contains(env + ".damage")) {
            return new DamageDefinition(
                    config.getString(env + ".name", cause),
                    config.getString(env + ".damage", "1d3")
            );
        }

        return new DamageDefinition(
                config.getString("default.name", "攻撃"),
                config.getString("default.damage", "1d3")
        );
    }

    private Entity resolveSourceEntity(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity entity) {
                return entity;
            }
        }

        return damager;
    }

    private int roll(String expression) {
        String value = expression.trim();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        Matcher matcher = DICE.matcher(value);
        if (!matcher.matches()) {
            plugin.getLogger().warning("damage.yml のダイス式を解釈できません: " + expression);
            return 0;
        }

        int count = Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));
        int bonus = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));

        if (count < 1 || count > 100 || sides < 1 || sides > 100000) {
            return 0;
        }

        int total = bonus;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1;
        }

        return total;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
