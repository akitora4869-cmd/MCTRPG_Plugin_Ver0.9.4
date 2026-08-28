package Rin.TRPGCharacter;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimeStopManager {

    private final Plugin plugin;
    private final KeeperManager keeperManager;

    private boolean stopped = false;
    private final Map<UUID, Boolean> mobAiStates = new HashMap<>();
    private final Map<String, Long> worldTimes = new HashMap<>();

    public TimeStopManager(Plugin plugin, KeeperManager keeperManager) {
        this.plugin = plugin;
        this.keeperManager = keeperManager;
    }

    public boolean isStopped() {
        return stopped;
    }

    public boolean canAct(Player player) {
        return player.isOp()
                || player.hasPermission("trpg.admin")
                || keeperManager.isKeeper(player);
    }

    public void toggle(Player executor) {
        if (stopped) {
            resume(executor);
        } else {
            stop(executor);
        }
    }

    public void stop(Player executor) {
        if (stopped) {
            return;
        }

        stopped = true;
        worldTimes.clear();
        mobAiStates.clear();

        for (World world : Bukkit.getWorlds()) {
            worldTimes.put(world.getName(), world.getTime());
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);

            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Mob mob) {
                    mobAiStates.put(mob.getUniqueId(), mob.hasAI());
                    mob.setAI(false);
                }
            }
        }

        Bukkit.broadcastMessage("§5[KP] §d時間を停止しました。§7 ―― PL会議中 ――");
    }

    public void resume(Player executor) {
        if (!stopped) {
            return;
        }

        stopped = false;

        for (World world : Bukkit.getWorlds()) {
            Long time = worldTimes.get(world.getName());
            if (time != null) {
                world.setTime(time);
            }

            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);

            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Mob mob) {
                    Boolean ai = mobAiStates.get(mob.getUniqueId());
                    if (ai != null) {
                        mob.setAI(ai);
                    }
                }
            }
        }

        worldTimes.clear();
        mobAiStates.clear();

        if (plugin.getSessionClockManager() != null) {
            plugin.getSessionClockManager().reapplyWorldClock();
        }

        Bukkit.broadcastMessage("§5[KP] §a時間を再開しました。");
    }
}
