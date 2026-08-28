package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class EnemyCombatManager implements Listener {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final ArmorManager armorManager;
    private final EnemyManager enemyManager;
    private final DodgeManager dodgeManager;
    private final CombatManager combatManager;
    private final Random random = new Random();

    public EnemyCombatManager(Plugin plugin,
                              CharacterManager characterManager,
                              ArmorManager armorManager,
                              EnemyManager enemyManager,
                              DodgeManager dodgeManager,
                              CombatManager combatManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.armorManager = armorManager;
        this.enemyManager = enemyManager;
        this.dodgeManager = dodgeManager;
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnemyAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        Entity source = enemyManager.resolveSource(event.getDamager());
        EnemyDefinition enemy = enemyManager.getDefinition(source);
        if (enemy == null || !enemyManager.isEnabled()) {
            return;
        }

        // この攻撃はTRPG方式で処理するためMinecraft標準ダメージを止める。
        event.setCancelled(true);

        if (plugin.getTimeStopManager().isStopped()) {
            return;
        }

        if (!characterManager.hasConfiguredStats(target)
                || characterManager.isDeadCharacter(target)
                || target.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        plugin.getDiceSoundManager().playRollSequence(target, () -> {
            if (!target.isOnline()
                    || characterManager.isDeadCharacter(target)
                    || target.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, enemy.hit());

            if (enemyManager.showRollsToTarget()) {
                target.sendMessage(color(
                        "&4[敵攻撃] &f" + enemy.name()
                                + " &7/ 命中 &b" + enemy.hit()
                                + " &7/ 1d100:&e" + roll
                                + " &7→ " + result.color() + result.label()
                ));
            }

            plugin.getDiceSoundManager().playResultSound(target, result);

            if (!result.isSuccess()) {
                return;
            }

            if (dodgeManager.isEnabled()) {
                DodgeManager.DodgeRoll dodge = dodgeManager.roll(target);

                if (dodge.avoided()) {
                    target.sendMessage(color(
                            "&a回避成功！ &7" + enemy.name() + " の攻撃を回避しました。"
                    ));

                    if (dodge.critical() && dodgeManager.criticalCounterAttack()
                            && source instanceof org.bukkit.entity.LivingEntity livingSource) {
                        target.sendMessage(color(
                                "&6★ 回避の決定的成功！ &f手に持っている武器で自動反撃します。"
                        ));
                        combatManager.performCounterAttack(target, livingSource);
                    }
                    return;
                }
            }

            int rolledDamage;
            if (result == CheckResult.CRITICAL && enemyManager.criticalMaxDamage()) {
                rolledDamage = enemyManager.maxDamage(enemy);
            } else {
                rolledDamage = enemyManager.rollDamage(enemy);
            }

            if (result == CheckResult.SPECIAL) {
                rolledDamage += enemyManager.specialBonus();
            }

            int armor = enemy.playerArmorApplies()
                    ? armorManager.getArmor(target)
                    : 0;

            int artifactReduction = 0;
            if (plugin.getMythosManager().getDefinition(source) != null
                    && plugin.getArtifactManager() != null) {
                artifactReduction =
                        plugin.getArtifactManager().getMythosDamageReduction(target);
            }

            int finalDamage = Math.max(
                    0,
                    rolledDamage - armor - artifactReduction
            );

            if (finalDamage <= 0) {
                if (artifactReduction > 0) {
                    target.sendMessage(color(
                            "&5[アーティファクト] &7神話生物からのダメージを軽減しました。"
                    ));
                }
                return;
            }

            int before = characterManager.getCurrentHp(target);
            int after = Math.max(0, before - finalDamage);
            characterManager.setCurrentHp(target, after);

            String armorText = enemy.playerArmorApplies()
                    ? " &7/ 装甲 &b" + armor
                    : " &7/ 装甲無効";

            String artifactText = artifactReduction > 0
                    ? " &7/ アーティファクト軽減 &d" + artifactReduction
                    : "";

            target.sendMessage(color(
                    "&c[敵ダメージ] &f" + enemy.name()
                            + " &7" + enemy.damage()
                            + " → &c" + rolledDamage
                            + armorText
                            + artifactText
                            + " &7/ 最終 &c" + finalDamage
                            + " &7/ HP &f" + before + " → " + after
            ));

            plugin.getSidebarManager().updatePlayer(target);

            if (after <= 0) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (target.isOnline() && !target.isDead()) {
                        target.setHealth(0.0);
                    }
                });
            } else {
                plugin.getHealthSyncManager().sync(target);
            }
        });
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
