package Rin.TRPGCharacter;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionManager {

    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Plugin plugin;
    private final File file;
    private final File logFolder;
    private YamlConfiguration data;

    public SessionManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "session.yml");
        this.logFolder = new File(plugin.getDataFolder(), "session-logs");
        this.data = YamlConfiguration.loadConfiguration(file);

        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        if (isActive()) {
            if (!data.contains("session.scenario-minutes")) {
                data.set("session.scenario-minutes", 12 * 60);
            }
            if (!data.contains("session.clock-running")) {
                data.set("session.clock-running", false);
            }
            if (!data.contains("session.clock-speed")) {
                data.set("session.clock-speed", 1);
            }
            save();
        }
    }

    public boolean isActive() {
        return data.getBoolean("session.active", false);
    }

    public String getSessionName() {
        return data.getString("session.name", "未作成");
    }

    public String getStartedAt() {
        return data.getString("session.started-at", "");
    }

    public int getParticipantCount() {
        if (data.getConfigurationSection("participants") == null) {
            return 0;
        }
        return data.getConfigurationSection("participants").getKeys(false).size();
    }

    public boolean createSession(String name, Player creator, SessionTimePeriod period) {
        if (isActive()) {
            return false;
        }

        data.set("session", null);
        data.set("participants", null);
        data.set("history", null);
        data.set("events", null);

        data.set("session.active", true);
        data.set("session.name", name);
        data.set("session.started-at", now());
        data.set("session.created-by.uuid", creator.getUniqueId().toString());
        data.set("session.created-by.name", creator.getName());
        data.set("session.scenario-minutes", period.startMinutes());
        data.set("session.clock-running", false);
        data.set("session.clock-speed", 1);

        addEvent("CREATE", creator.getName(),
                "セッション作成: " + name + " / 開始時間帯: " + period.displayName());
        save();
        return true;
    }

    public boolean isParticipant(Player player) {
        return isParticipant(player.getUniqueId());
    }

    public boolean isParticipant(UUID uuid) {
        return data.getBoolean("participants." + uuid + ".joined", false);
    }

    public boolean join(Player player) {
        if (!isActive()) {
            return false;
        }

        String uuid = player.getUniqueId().toString();
        String time = now();

        data.set("participants." + uuid + ".joined", true);
        data.set("participants." + uuid + ".name", player.getName());
        data.set("participants." + uuid + ".joined-at", time);

        data.set("history." + uuid + ".name", player.getName());
        if (!data.contains("history." + uuid + ".first-joined-at")) {
            data.set("history." + uuid + ".first-joined-at", time);
        }
        data.set("history." + uuid + ".last-joined-at", time);
        data.set("history." + uuid + ".currently-joined", true);

        addEvent("JOIN", player.getName(), "セッション参加");
        save();
        return true;
    }

    public void leave(Player player) {
        String uuid = player.getUniqueId().toString();

        data.set("participants." + uuid, null);
        data.set("history." + uuid + ".name", player.getName());
        data.set("history." + uuid + ".last-left-at", now());
        data.set("history." + uuid + ".currently-joined", false);

        addEvent("LEAVE", player.getName(), "セッション退出");
        save();
    }

    public int getScenarioMinutes() {
        return Math.floorMod(data.getInt("session.scenario-minutes", 12 * 60), 1440);
    }

    public void setScenarioMinutes(int minutes, String actor) {
        int normalized = Math.floorMod(minutes, 1440);
        data.set("session.scenario-minutes", normalized);

        if (!"CREATE".equalsIgnoreCase(actor)) {
            addEvent("TIME_SET", actor,
                    "シナリオ時刻を " + formatTime(normalized) + " に変更");
        }

        save();
    }

    public void addScenarioMinutes(int minutes, String actor) {
        int before = getScenarioMinutes();
        int after = Math.floorMod(before + minutes, 1440);
        data.set("session.scenario-minutes", after);

        if (!"AUTO".equalsIgnoreCase(actor)) {
            addEvent("TIME_ADD", actor,
                    minutes + "分進行: " + formatTime(before) + " -> " + formatTime(after));
        }

        save();
    }

    public boolean isClockRunning() {
        return data.getBoolean("session.clock-running", false);
    }

    public void setClockRunning(boolean running) {
        data.set("session.clock-running", running);
        save();
    }

    public int getClockSpeed() {
        return Math.max(1, data.getInt("session.clock-speed", 1));
    }

    public void setClockSpeed(int speed) {
        data.set("session.clock-speed", Math.max(1, speed));
        save();
    }

    public void recordClockEvent(String type, String actor, String detail) {
        addEvent(type, actor, detail);
        save();
    }

    public String getTimePeriodName() {
        return SessionTimePeriod.fromMinutes(getScenarioMinutes()).displayName();
    }

    public String getScenarioTimeText() {
        return formatTime(getScenarioMinutes());
    }

    public File endSession(Player ender, CharacterManager characterManager) {
        if (!isActive()) {
            return null;
        }

        String sessionName = getSessionName();
        String startedAt = getStartedAt();
        String endedAt = now();

        YamlConfiguration log = new YamlConfiguration();
        log.set("session.name", sessionName);
        log.set("session.started-at", startedAt);
        log.set("session.ended-at", endedAt);
        log.set("session.created-by.uuid", data.getString("session.created-by.uuid", ""));
        log.set("session.created-by.name", data.getString("session.created-by.name", ""));
        log.set("session.ended-by.uuid", ender.getUniqueId().toString());
        log.set("session.ended-by.name", ender.getName());
        log.set("session.final-time-period", getTimePeriodName());
        log.set("session.final-scenario-time", getScenarioTimeText());
        log.set("session.clock-running-at-end", isClockRunning());
        log.set("session.clock-speed", getClockSpeed());

        if (data.getConfigurationSection("history") != null) {
            for (String uuidString : data.getConfigurationSection("history").getKeys(false)) {
                String base = "participants." + uuidString;
                String historyBase = "history." + uuidString;
                String mcName = data.getString(historyBase + ".name", "unknown");

                log.set(base + ".minecraft-name", mcName);
                log.set(base + ".first-joined-at",
                        data.getString(historyBase + ".first-joined-at", ""));
                log.set(base + ".last-joined-at",
                        data.getString(historyBase + ".last-joined-at", ""));
                log.set(base + ".last-left-at",
                        data.getString(historyBase + ".last-left-at", ""));
                log.set(base + ".joined-at-end",
                        data.getBoolean(historyBase + ".currently-joined", false));

                try {
                    UUID uuid = UUID.fromString(uuidString);
                    Player online = plugin.getServer().getPlayer(uuid);
                    if (online != null) {
                        log.set(base + ".online-at-end", true);
                        log.set(base + ".character-name", characterManager.getCharacterName(online));
                        log.set(base + ".hp.current", characterManager.getCurrentHp(online));
                        log.set(base + ".hp.max", characterManager.getHp(online));
                        log.set(base + ".mp.current", characterManager.getCurrentMp(online));
                        log.set(base + ".mp.max", characterManager.getMp(online));
                        log.set(base + ".san.current", characterManager.getCurrentSan(online));
                        log.set(base + ".san.max", characterManager.getSan(online));
                    } else {
                        log.set(base + ".online-at-end", false);
                    }
                } catch (IllegalArgumentException ignored) {
                    log.set(base + ".online-at-end", false);
                }
            }
        }

        List<String> events = new ArrayList<>(data.getStringList("events"));
        events.add("[" + endedAt + "] END | " + ender.getName()
                + " | セッション終了 / 最終時刻 " + getScenarioTimeText());
        log.set("events", events);

        String safeName = sanitizeFileName(sessionName);
        File logFile = new File(logFolder,
                FILE_TIME.format(LocalDateTime.now()) + "-" + safeName + ".yml");

        try {
            log.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().severe("セッションログの保存に失敗しました: " + e.getMessage());
            return null;
        }

        data.set("session", null);
        data.set("participants", null);
        data.set("history", null);
        data.set("events", null);
        data.set("last-log", logFile.getName());
        save();

        return logFile;
    }

    public String getLastLogName() {
        return data.getString("last-log", "");
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(file);
    }

    private void addEvent(String type, String playerName, String detail) {
        List<String> events = new ArrayList<>(data.getStringList("events"));
        events.add("[" + now() + "] " + type + " | " + playerName + " | " + detail);
        data.set("events", events);
    }

    private String formatTime(int totalMinutes) {
        int normalized = Math.floorMod(totalMinutes, 1440);
        return String.format("%02d:%02d", normalized / 60, normalized % 60);
    }

    private String now() {
        return DISPLAY_TIME.format(LocalDateTime.now());
    }

    private String sanitizeFileName(String input) {
        String safe = input.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safe.isEmpty()) {
            safe = "session";
        }
        if (safe.length() > 50) {
            safe = safe.substring(0, 50);
        }
        return safe;
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("session.yml の保存に失敗しました: " + e.getMessage());
        }
    }
}
