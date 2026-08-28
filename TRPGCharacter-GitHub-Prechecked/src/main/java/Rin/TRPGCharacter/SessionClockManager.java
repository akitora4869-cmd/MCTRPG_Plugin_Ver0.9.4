package Rin.TRPGCharacter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SessionClockManager {

    private final Plugin plugin;
    private final SessionManager sessionManager;
    private final KeeperManager keeperManager;

    private final BossBar bossBar;
    private BukkitTask task;
    private double secondAccumulator = 0.0;

    public SessionClockManager(Plugin plugin,
                               SessionManager sessionManager,
                               KeeperManager keeperManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.keeperManager = keeperManager;
        this.bossBar = Bukkit.createBossBar(
                "TRPG Session Clock",
                BarColor.PURPLE,
                BarStyle.SEGMENTED_12
        );
        bossBar.setVisible(false);
    }

    public void start() {
        if (task != null) return;

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!sessionManager.isActive()) {
                bossBar.removeAll();
                bossBar.setVisible(false);
                return;
            }

            ensureWorldClockRules();

            if (sessionManager.isClockRunning()
                    && !plugin.getTimeStopManager().isStopped()) {
                int speed = sessionManager.getClockSpeed();

                if (speed > 0) {
                    secondAccumulator += speed;
                    int minutesToAdd = (int) (secondAccumulator / 60.0);

                    if (minutesToAdd > 0) {
                        secondAccumulator -= minutesToAdd * 60.0;
                        sessionManager.addScenarioMinutes(minutesToAdd, "AUTO");
                    }
                }
            }

            reapplyWorldClock();
            updateBossBar();
        }, 20L, 20L);

        if (sessionManager.isActive()) {
            reapplyWorldClock();
            updateBossBar();
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        bossBar.removeAll();
        bossBar.setVisible(false);
    }

    public void beginSession(SessionTimePeriod period) {
        secondAccumulator = 0.0;
        sessionManager.setScenarioMinutes(period.startMinutes(), "CREATE");
        sessionManager.setClockRunning(false);
        sessionManager.setClockSpeed(1);
        reapplyWorldClock();
        updateBossBar();
    }

    public void setTimePeriod(SessionTimePeriod period, String actor) {
        secondAccumulator = 0.0;
        sessionManager.setScenarioMinutes(period.startMinutes(), actor);
        reapplyWorldClock();
        updateBossBar();
    }

    public void startClock(String actor) {
        sessionManager.setClockRunning(true);
        sessionManager.recordClockEvent("TIME_START", actor,
                "時刻進行を開始 speed=" + sessionManager.getClockSpeed());
        updateBossBar();
    }

    public void pauseClock(String actor) {
        sessionManager.setClockRunning(false);
        sessionManager.recordClockEvent("TIME_PAUSE", actor, "時刻進行を停止");
        updateBossBar();
    }

    public void resumeClock(String actor) {
        sessionManager.setClockRunning(true);
        sessionManager.recordClockEvent("TIME_RESUME", actor,
                "時刻進行を再開 speed=" + sessionManager.getClockSpeed());
        updateBossBar();
    }

    public void setSpeed(int speed, String actor) {
        sessionManager.setClockSpeed(speed);
        sessionManager.recordClockEvent("TIME_SPEED", actor,
                "進行速度を変更: " + speed + "ゲーム内分/現実1分");
        updateBossBar();
    }

    public void addMinutes(int minutes, String actor) {
        secondAccumulator = 0.0;
        sessionManager.addScenarioMinutes(minutes, actor);
        reapplyWorldClock();
        updateBossBar();
    }

    public void onSessionEnd() {
        secondAccumulator = 0.0;
        bossBar.removeAll();
        bossBar.setVisible(false);

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        }
    }

    public void reapplyWorldClock() {
        if (!sessionManager.isActive()) return;

        long ticks = minutesToMinecraftTicks(sessionManager.getScenarioMinutes());

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setTime(ticks);
        }
    }

    public String getDisplayTime() {
        int minutes = Math.floorMod(sessionManager.getScenarioMinutes(), 1440);
        int hour = minutes / 60;
        int minute = minutes % 60;
        return String.format("%02d:%02d", hour, minute);
    }

    public String getDisplayPeriod() {
        return SessionTimePeriod.fromMinutes(
                sessionManager.getScenarioMinutes()
        ).displayName();
    }

    public String getClockStatusText() {
        if (!sessionManager.isClockRunning()) {
            return "停止中";
        }
        return "進行中 x" + sessionManager.getClockSpeed();
    }

    private void ensureWorldClockRules() {
        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }
    }

    private void updateBossBar() {
        if (!sessionManager.isActive()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            return;
        }

        int minutes = Math.floorMod(sessionManager.getScenarioMinutes(), 1440);
        SessionTimePeriod period = SessionTimePeriod.fromMinutes(minutes);

        int start = period.startMinutes();
        int end = SessionTimePeriod.periodEndMinutes(period);
        int normalized = minutes;

        if (period == SessionTimePeriod.LATE_NIGHT) {
            start = 0;
            end = 5 * 60;
        }

        double progress = (double) (normalized - start) / Math.max(1, end - start);
        progress = Math.max(0.0, Math.min(1.0, progress));

        String status = sessionManager.isClockRunning()
                ? "▶ x" + sessionManager.getClockSpeed()
                : "Ⅱ 停止";

        bossBar.setTitle(ChatColor.LIGHT_PURPLE
                + sessionManager.getSessionName()
                + ChatColor.GRAY + " | "
                + ChatColor.AQUA + period.displayName()
                + ChatColor.WHITE + " " + getDisplayTime()
                + ChatColor.GRAY + " | " + status);

        bossBar.setProgress(progress);
        bossBar.setColor(colorFor(period));
        bossBar.setVisible(true);

        Set<UUID> shouldSee = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessionManager.isParticipant(player)
                    || keeperManager.isKeeper(player)
                    || player.isOp()
                    || player.hasPermission("trpg.admin")) {
                shouldSee.add(player.getUniqueId());
                if (!bossBar.getPlayers().contains(player)) {
                    bossBar.addPlayer(player);
                }
            }
        }

        for (Player player : bossBar.getPlayers()) {
            if (!shouldSee.contains(player.getUniqueId())) {
                bossBar.removePlayer(player);
            }
        }
    }

    private BarColor colorFor(SessionTimePeriod period) {
        return switch (period) {
            case EARLY_MORNING -> BarColor.WHITE;
            case MORNING -> BarColor.YELLOW;
            case NOON -> BarColor.GREEN;
            case EVENING -> BarColor.RED;
            case NIGHT -> BarColor.BLUE;
            case LATE_NIGHT -> BarColor.PURPLE;
        };
    }

    private long minutesToMinecraftTicks(int totalMinutes) {
        int minutes = Math.floorMod(totalMinutes, 1440);
        long ticks = Math.round((minutes - 360) * (1000.0 / 60.0));
        return Math.floorMod(ticks, 24000L);
    }
}
