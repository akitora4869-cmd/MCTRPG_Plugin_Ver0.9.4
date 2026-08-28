package Rin.TRPGCharacter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RollManager {

    private final Plugin plugin;
    private final SkillEffectManager skillEffectManager;
    private final Random random = new Random();

    private static final Pattern DICE =
            Pattern.compile("^(\\d+)[dD](\\d+)$");

    public RollManager(Plugin plugin, SkillEffectManager skillEffectManager) {
        this.plugin = plugin;
        this.skillEffectManager = skillEffectManager;
    }

    public boolean rollDice(CommandSender sender, String expression) {
        Matcher matcher = DICE.matcher(expression);

        if (!matcher.matches()) {
            sender.sendMessage(color("&c使い方: /roll <XdY>  例: /roll 1d100"));
            return false;
        }

        int count;
        int sides;

        try {
            count = Integer.parseInt(matcher.group(1));
            sides = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&c数字が大きすぎます。"));
            return false;
        }

        if (count < 1 || count > 100) {
            sender.sendMessage(color("&cダイス数は1～100にしてください。"));
            return false;
        }

        if (sides < 1 || sides > 100000) {
            sender.sendMessage(color("&c面数は1～100000にしてください。"));
            return false;
        }

        if (sender instanceof Player player) {
            plugin.getDiceSoundManager().playRollSequence(player, () -> {
                long total = 0L;
                for (int i = 0; i < count; i++) {
                    total += random.nextInt(sides) + 1L;
                }

                String message = color("&6[ROLL] &f" + sender.getName()
                        + " &7が &b" + count + "d" + sides
                        + " &7を振った → &e" + total);

                plugin.getDiceSoundManager().playResultSound(player, CheckResult.SUCCESS);
                sendRollMessage(sender, message);
            });
            return true;
        }

        long total = 0L;
        for (int i = 0; i < count; i++) {
            total += random.nextInt(sides) + 1L;
        }

        String message = color("&6[ROLL] &f" + sender.getName()
                + " &7が &b" + count + "d" + sides
                + " &7を振った → &e" + total);

        sendRollMessage(sender, message);
        return true;
    }

    public void rollCheck(Player player, String label, int target) {
        plugin.getDiceSoundManager().playRollSequence(player, () -> {
            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, target);

            String message = color(
                    "&6[CoC判定] &f" + player.getName()
                            + " &7- &b" + label
                            + " &7目標値:&e" + target
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            );

            plugin.getDiceSoundManager().playResultSound(player, result);
            sendRollMessage(player, message);
        });
    }

    public void rollSkillCheck(Player player, String skillId, String label, int target) {
        plugin.getDiceSoundManager().playRollSequence(player, () -> {
            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, target);

            plugin.getSkillGrowthManager().tryGrowth(
                    player, skillId, label, result
            );

            String message = color(
                    "&6[CoC判定] &f" + player.getName()
                            + " &7- &b" + label
                            + " &7目標値:&e" + target
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            );

            plugin.getDiceSoundManager().playResultSound(player, result);
            sendRollMessage(player, message);

            if (result.isSuccess()) {
                skillEffectManager.applyOnSuccess(player, skillId);

                if ("spot_hidden".equalsIgnoreCase(skillId)) {
                    DarkVisionManager darkVisionManager = plugin.getDarkVisionManager();
                    if (darkVisionManager != null) {
                        darkVisionManager.onManualSpotHiddenSuccess(player);
                    }
                }

                if ("swim".equalsIgnoreCase(skillId)) {
                    SwimManager swimManager = plugin.getSwimManager();
                    if (swimManager != null) {
                        swimManager.onManualSwimSuccess(player);
                    }
                }
            }
        });
    }

    private void sendRollMessage(CommandSender sender, String message) {
        if (plugin.getConfig().getBoolean("broadcast-rolls", true)) {
            Bukkit.broadcastMessage(message);
        } else {
            sender.sendMessage(message);
        }
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
