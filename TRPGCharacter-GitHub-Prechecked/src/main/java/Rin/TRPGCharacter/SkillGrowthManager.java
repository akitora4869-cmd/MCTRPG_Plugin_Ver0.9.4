package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillGrowthManager {

    private static final Pattern DICE =
            Pattern.compile("^(\\d+)[dD](\\d+)(?:([+-])(\\d+))?$");

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final Random random = new Random();

    public SkillGrowthManager(Plugin plugin,
                              CharacterManager characterManager,
                              SkillManager skillManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.skillManager = skillManager;
    }

    public void tryGrowth(Player player,
                          String skillId,
                          String skillName,
                          CheckResult triggerResult) {
        if (!plugin.getConfig().getBoolean("skill-growth.enabled", true)) {
            return;
        }

        boolean triggerCritical = plugin.getConfig().getBoolean(
                "skill-growth.trigger-critical", true
        );
        boolean triggerFumble = plugin.getConfig().getBoolean(
                "skill-growth.trigger-fumble", true
        );

        boolean triggered =
                (triggerCritical && triggerResult == CheckResult.CRITICAL)
                        || (triggerFumble && triggerResult == CheckResult.FUMBLE);

        if (!triggered) {
            return;
        }

        if (!skillManager.hasSkill(skillId)) {
            return;
        }

        int max = Math.max(1, Math.min(
                999,
                plugin.getConfig().getInt("skill-growth.max-skill-value", 99)
        ));

        int current = skillManager.getSkillValue(player, skillId);
        if (current >= max) {
            player.sendMessage(color(
                    "&a[技能成長] &f" + skillName
                            + " &7は上限 " + max + " のため成長判定を行いません。"
            ));
            return;
        }

        int growthRoll = random.nextInt(100) + 1;
        boolean success = growthRoll > current;

        player.sendMessage(color(
                "&a[技能成長判定] &f" + skillName
                        + " &7現在値:&b" + current
                        + " &7/ 1d100:&e" + growthRoll
                        + " &7→ "
                        + (success ? "&a成長成功" : "&c成長失敗")
        ));

        if (!success) {
            return;
        }

        String expression = plugin.getConfig().getString(
                "skill-growth.increase",
                "1d10"
        );

        int rolledIncrease = Math.max(0, roll(expression));
        int actualIncrease = Math.min(rolledIncrease, max - current);

        if (actualIncrease <= 0) {
            return;
        }

        characterManager.addSkillGrowth(player, skillId, actualIncrease);
        int after = skillManager.getSkillValue(player, skillId);

        player.sendMessage(color(
                "&6★ 技能成長！ &f" + skillName
                        + " &b" + current
                        + " &7→ &a" + after
                        + " &7(+" + actualIncrease + ")"
        ));

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_LEVELUP,
                0.8f,
                1.25f
        );
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
                    "skill-growth.increase を解釈できません: " + expression
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

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
