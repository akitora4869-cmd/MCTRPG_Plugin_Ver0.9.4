package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WallKickManager implements Listener {
    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final Random random = new Random();
    private final Map<UUID, Long> firstJumpAt = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<UUID, Double> lastY = new HashMap<>();

    public WallKickManager(Plugin plugin, CharacterManager characterManager, SkillManager skillManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.skillManager = skillManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (event.getTo() == null || !plugin.getConfig().getBoolean("wall-kick.enabled", true) || !canUse(p)) {
            firstJumpAt.remove(p.getUniqueId());
            lastY.remove(p.getUniqueId());
            return;
        }

        long now = System.currentTimeMillis();
        long window = Math.max(150L, plugin.getConfig().getLong("wall-kick.double-jump-window-ms", 450L));
        double vy = p.getVelocity().getY();
        double oldVy = lastY.getOrDefault(p.getUniqueId(), vy);
        lastY.put(p.getUniqueId(), vy);

        boolean firstJump = p.isOnGround() && event.getTo().getY() > event.getFrom().getY() && vy > 0.15;
        if (firstJump) {
            if (wallNormal(p) != null) firstJumpAt.put(p.getUniqueId(), now);
            else firstJumpAt.remove(p.getUniqueId());
            return;
        }

        Long first = firstJumpAt.get(p.getUniqueId());
        if (first == null) return;
        if (now - first > window) {
            firstJumpAt.remove(p.getUniqueId());
            return;
        }
        if (now < cooldownUntil.getOrDefault(p.getUniqueId(), 0L) || p.isOnGround()) return;

        Vector normal = wallNormal(p);
        if (normal == null) return;

        // Bukkitにはスペースキー押下イベントがないため、壁際の空中上向き速度変化を2回目入力相当として扱う。
        boolean secondSignal = vy > oldVy + 0.035 || (oldVy <= 0.08 && vy > 0.08);
        if (!secondSignal) return;

        firstJumpAt.remove(p.getUniqueId());
        attempt(p);
    }

    private void attempt(Player p) {
        String skillId = plugin.getConfig().getString("wall-kick.skill", "climb");
        SkillDefinition def = skillManager.getSkill(skillId);
        String name = def != null ? def.getName() : "登攀";
        int value = skillManager.getSkillValue(p, skillId);

        plugin.getDiceSoundManager().playRollSequence(p, () -> {
            if (!canUse(p)) return;
            Vector normal = wallNormal(p);
            if (normal == null) return;

            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, value);
            p.sendMessage(color("&6[壁キック] &e" + name + "判定 &7/ 技能値 &b" + value
                    + " &7/ 1d100:&e" + roll + " &7→ " + result.color() + result.label()));
            plugin.getDiceSoundManager().playResultSound(p, result);
            plugin.getSkillGrowthManager().tryGrowth(p, skillId, name, result);

            cooldownUntil.put(p.getUniqueId(), System.currentTimeMillis()
                    + Math.max(0L, plugin.getConfig().getLong("wall-kick.cooldown-ms", 900L)));

            if (!result.isSuccess()) {
                if (result == CheckResult.FUMBLE) {
                    Vector v = p.getVelocity();
                    v.setY(-Math.max(0.0, plugin.getConfig().getDouble("wall-kick.fumble-downward-velocity", 0.35)));
                    p.setVelocity(v);
                    p.sendMessage(color("&c壁キックに失敗し、体勢を崩しました。"));
                }
                return;
            }

            double h = Math.max(0.1, plugin.getConfig().getDouble("wall-kick.horizontal-velocity", 0.65));
            double y = Math.max(0.1, plugin.getConfig().getDouble("wall-kick.vertical-velocity", 0.72));
            double mult = result == CheckResult.CRITICAL
                    ? plugin.getConfig().getDouble("wall-kick.critical-multiplier", 1.30)
                    : result == CheckResult.SPECIAL
                    ? plugin.getConfig().getDouble("wall-kick.special-multiplier", 1.15) : 1.0;

            Vector launch = normal.normalize().multiply(h * Math.max(1.0, mult));
            launch.setY(y * Math.max(1.0, mult));
            p.setVelocity(launch);
            p.setFallDistance(0.0f);
            p.sendMessage(color("&a壁キック成功！"));
        });
    }

    private Vector wallNormal(Player p) {
        double d = Math.max(0.25, plugin.getConfig().getDouble("wall-kick.wall-check-distance", 0.55));
        Vector[] dirs = {new Vector(1,0,0), new Vector(-1,0,0), new Vector(0,0,1), new Vector(0,0,-1)};
        for (Vector dir : dirs) {
            Vector q = p.getLocation().toVector().add(dir.clone().multiply(d));
            Block feet = p.getWorld().getBlockAt(q.getBlockX(), p.getLocation().getBlockY(), q.getBlockZ());
            Block head = feet.getRelative(0, 1, 0);
            if (feet.getType().isSolid() || head.getType().isSolid()) return dir.clone().multiply(-1);
        }
        return null;
    }

    private boolean canUse(Player p) {
        return p.isOnline() && p.getGameMode() != GameMode.SPECTATOR && !p.isFlying()
                && !p.isInsideVehicle() && !p.isInWater()
                && characterManager.hasConfiguredStats(p) && !characterManager.isDeadCharacter(p)
                && (plugin.getTimeStopManager() == null || plugin.getTimeStopManager().canAct(p));
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
