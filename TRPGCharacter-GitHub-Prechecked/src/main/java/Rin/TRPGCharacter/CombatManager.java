package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Random;

public class CombatManager implements Listener {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final WeaponManager weaponManager;
    private final ArmorManager armorManager;
    private final EnemyManager enemyManager;
    private final DodgeManager dodgeManager;
    private final Random random = new Random();

    public CombatManager(Plugin plugin,
                         CharacterManager characterManager,
                         SkillManager skillManager,
                         WeaponManager weaponManager,
                         ArmorManager armorManager,
                         EnemyManager enemyManager,
                         DodgeManager dodgeManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.skillManager = skillManager;
        this.weaponManager = weaponManager;
        this.armorManager = armorManager;
        this.enemyManager = enemyManager;
        this.dodgeManager = dodgeManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // 手掛かり用ArmorStandなどは既存の仕組みを優先する。
        if (target instanceof ArmorStand) {
            return;
        }

        if (!weaponManager.isEnabled()) {
            return;
        }

        WeaponDefinition weapon = weaponManager.resolve(attacker);
        if (weapon == null) {
            // weapons.yml に登録していないアイテムは従来のMinecraft攻撃を使用。
            return;
        }

        event.setCancelled(true);

        if (plugin.getTimeStopManager().isStopped()) {
            attacker.sendMessage(color("&5[時間停止] &7現在は攻撃できません。"));
            return;
        }

        if (attacker.getGameMode() == GameMode.SPECTATOR
                || characterManager.isDeadCharacter(attacker)) {
            return;
        }

        if (!characterManager.hasConfiguredStats(attacker)) {
            attacker.sendMessage(color("&c探索者能力値が設定されていないため、TRPG戦闘判定を行えません。"));
            return;
        }

        if (target instanceof Player targetPlayer) {
            if (!weaponManager.allowPvp()) {
                attacker.sendMessage(color("&c探索者同士の攻撃は無効です。"));
                return;
            }

            if (targetPlayer.getGameMode() == GameMode.SPECTATOR
                    || characterManager.isDeadCharacter(targetPlayer)) {
                return;
            }
        }

        executeRegisteredAttack(attacker, target, weapon, true, "&6[攻撃判定]");
    }

    private void executeRegisteredAttack(Player attacker,
                                         LivingEntity target,
                                         WeaponDefinition weapon,
                                         boolean allowDodge,
                                         String label) {
        int skillValue = weaponManager.getSkillValue(attacker, weapon);

        plugin.getDiceSoundManager().playRollSequence(attacker, () -> {
            if (!attacker.isOnline() || target.isDead() || !target.isValid()) {
                return;
            }

            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, skillValue);

            plugin.getSkillGrowthManager().tryGrowth(
                    attacker,
                    weapon.skillId(),
                    getSkillName(weapon.skillId()),
                    result
            );

            attacker.sendMessage(color(
                    label + " &f" + weapon.name()
                            + " &7/ 技能 &b" + getSkillName(weapon.skillId())
                            + " &7" + skillValue
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            ));

            plugin.getDiceSoundManager().playResultSound(attacker, result);

            boolean martialApplicable = weapon.martialArts()
                    && weaponManager.martialArtsEnabled();

            CheckResult martialResult = null;
            int martialValue = 0;
            int martialRoll = 0;

            if (martialApplicable) {
                String martialSkillId = weaponManager.martialArtsSkillId();
                martialValue = skillManager.getSkillValue(attacker, martialSkillId);
                martialRoll = random.nextInt(100) + 1;
                martialResult = CheckResult.evaluate(martialRoll, martialValue);

                SkillDefinition growthSkill =
                        skillManager.getSkill(martialSkillId);
                plugin.getSkillGrowthManager().tryGrowth(
                        attacker,
                        martialSkillId,
                        growthSkill != null
                                ? growthSkill.getName()
                                : martialSkillId,
                        martialResult
                );

                attacker.sendMessage(color(
                        "&d[マーシャルアーツ] &7技能値 &b" + martialValue
                                + " &7/ 1d100:&e" + martialRoll
                                + " &7→ " + martialResult.color() + martialResult.label()
                ));
            }

            if (!result.isSuccess()) {
                if (result == CheckResult.FUMBLE) {
                    attacker.playSound(attacker.getLocation(),
                            Sound.ENTITY_ITEM_BREAK, 0.8f, 0.7f);
                }

                if (martialResult != null && martialResult.isSuccess()) {
                    attacker.sendMessage(color(
                            "&7マーシャルアーツには成功しましたが、攻撃自体が失敗したため効果は発動しません。"
                    ));
                }
                return;
            }

            boolean martialSuccess = martialResult != null && martialResult.isSuccess();

            if (martialSuccess) {
                attacker.sendMessage(color(
                        "&d★ マーシャルアーツ成功！ &fダメージ×"
                                + weaponManager.martialArtsMultiplier()
                ));
            }

            DamageRoll damageRoll = calculateDamage(attacker, weapon, result, martialSuccess);

            if (target instanceof Player targetPlayer) {
                applyToPlayer(attacker, targetPlayer, weapon, result, damageRoll, allowDodge);
            } else {
                applyToMob(attacker, target, weapon, result, damageRoll);
            }
        });
    }

    public void performCounterAttack(Player attacker, LivingEntity target) {
        if (!weaponManager.isEnabled()
                || plugin.getTimeStopManager().isStopped()
                || attacker.getGameMode() == GameMode.SPECTATOR
                || characterManager.isDeadCharacter(attacker)
                || !characterManager.hasConfiguredStats(attacker)
                || target == null
                || !target.isValid()
                || target.isDead()) {
            return;
        }

        WeaponDefinition weapon = weaponManager.resolve(attacker);
        if (weapon == null) {
            attacker.sendMessage(color(
                    "&7手に持っているアイテムがTRPG武器として登録されていないため、自動反撃は行われません。"
            ));
            return;
        }

        int skillValue = weaponManager.getSkillValue(attacker, weapon);

        plugin.getDiceSoundManager().playRollSequence(attacker, () -> {
            if (!attacker.isOnline()
                    || target.isDead()
                    || !target.isValid()) {
                return;
            }

            int roll = random.nextInt(100) + 1;
            CheckResult result = CheckResult.evaluate(roll, skillValue);

            plugin.getSkillGrowthManager().tryGrowth(
                    attacker,
                    weapon.skillId(),
                    getSkillName(weapon.skillId()),
                    result
            );

            attacker.sendMessage(color(
                    "&6[自動反撃] &f" + weapon.name()
                            + " &7/ 技能 &b" + getSkillName(weapon.skillId())
                            + " &7" + skillValue
                            + " &7/ 1d100:&e" + roll
                            + " &7→ " + result.color() + result.label()
            ));
            plugin.getDiceSoundManager().playResultSound(attacker, result);

            boolean martialApplicable = weapon.martialArts()
                    && weaponManager.martialArtsEnabled();

            CheckResult martialResult = null;
            int martialValue = 0;

            if (martialApplicable) {
                String martialSkillId = weaponManager.martialArtsSkillId();
                martialValue = skillManager.getSkillValue(attacker, martialSkillId);
                int martialRoll = random.nextInt(100) + 1;
                martialResult = CheckResult.evaluate(martialRoll, martialValue);

                SkillDefinition growthSkill =
                        skillManager.getSkill(martialSkillId);
                plugin.getSkillGrowthManager().tryGrowth(
                        attacker,
                        martialSkillId,
                        growthSkill != null
                                ? growthSkill.getName()
                                : martialSkillId,
                        martialResult
                );

                attacker.sendMessage(color(
                        "&d[反撃マーシャルアーツ] &7技能値 &b" + martialValue
                                + " &7/ 1d100:&e" + martialRoll
                                + " &7→ " + martialResult.color() + martialResult.label()
                ));
            }

            if (!result.isSuccess()) {
                return;
            }

            boolean martialSuccess = martialResult != null && martialResult.isSuccess();
            DamageRoll damageRoll = calculateDamage(attacker, weapon, result, martialSuccess);

            if (target instanceof Player targetPlayer) {
                // 反撃に対してさらに回避→反撃が連鎖しないよう、反撃時は再回避させない。
                applyToPlayer(attacker, targetPlayer, weapon, result, damageRoll, false);
            } else {
                applyToMob(attacker, target, weapon, result, damageRoll);
            }
        });
    }

    private DamageRoll calculateDamage(Player attacker,
                                       WeaponDefinition weapon,
                                       CheckResult result,
                                       boolean martialArtsSuccess) {
        int base;
        int db = 0;

        if (result == CheckResult.CRITICAL && weaponManager.criticalMaxDamage()) {
            base = weaponManager.maxWeaponDamage(weapon);
            if (weapon.damageBonus()) {
                db = weaponManager.maxDamageBonus(attacker);
            }
        } else {
            base = weaponManager.rollWeaponDamage(weapon);
            if (weapon.damageBonus()) {
                db = weaponManager.rollDamageBonus(attacker);
            }
        }

        int special = result == CheckResult.SPECIAL
                ? weaponManager.specialBonus()
                : 0;

        int subtotal = Math.max(0, base + db + special);
        int multiplier = martialArtsSuccess
                ? weaponManager.martialArtsMultiplier()
                : 1;
        int total = Math.max(0, subtotal * multiplier);

        return new DamageRoll(base, db, special, multiplier, total);
    }

    private void applyToPlayer(Player attacker,
                               Player target,
                               WeaponDefinition weapon,
                               CheckResult result,
                               DamageRoll damageRoll,
                               boolean allowDodge) {
        if (allowDodge && dodgeManager.isEnabled()) {
            DodgeManager.DodgeRoll dodge = dodgeManager.roll(target);

            if (dodge.avoided()) {
                target.sendMessage(color("&a回避成功！ &7攻撃ダメージを受けませんでした。"));
                attacker.sendMessage(color("&7" + target.getName() + " が攻撃を回避しました。"));

                if (dodge.critical() && dodgeManager.criticalCounterAttack()) {
                    target.sendMessage(color("&6★ 回避の決定的成功！ &f自動反撃を行います。"));
                    attacker.sendMessage(color("&6" + target.getName() + " の回避が決定的成功。反撃判定が発生します。"));
                    performCounterAttack(target, attacker);
                }
                return;
            }
        }

        int armor = armorManager.getArmor(target);
        int finalDamage = Math.max(0, damageRoll.total() - armor);

        if (finalDamage <= 0) {
            return;
        }

        int before = characterManager.getCurrentHp(target);
        int after = Math.max(0, before - finalDamage);

        characterManager.setCurrentHp(target, after);

        String formula = damageFormula(attacker, weapon, result, damageRoll);
        attacker.sendMessage(color(
                "&6[攻撃ダメージ] &f" + weapon.name()
                        + " &7" + formula
                        + " &7→ &c" + damageRoll.total()
                        + " &7/ 相手装甲 &b" + armor
                        + " &7/ 最終 &c" + finalDamage
        ));

        target.sendMessage(color(
                "&c[被ダメージ] &f" + attacker.getName()
                        + " &7の" + weapon.name()
                        + " → &c" + finalDamage
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
    }

    private void applyToMob(Player attacker,
                            LivingEntity target,
                            WeaponDefinition weapon,
                            CheckResult result,
                            DamageRoll damageRoll) {
        EnemyDefinition enemy = enemyManager.getDefinition(target);
        int armor = enemy == null ? 0 : enemy.armor();
        int finalDamage = Math.max(0, damageRoll.total() - armor);

        double before = target.getHealth();
        double after = Math.max(0.0, before - finalDamage);

        target.setHealth(after);

        String enemyName = enemy == null ? target.getType().name() : enemy.name();

        attacker.sendMessage(color(
                "&6[攻撃ダメージ] &f" + weapon.name()
                        + " &7" + damageFormula(attacker, weapon, result, damageRoll)
                        + " &7→ &c" + damageRoll.total()
                        + " &7/ " + enemyName + "の装甲 &b" + armor
                        + " &7/ 最終 &c" + finalDamage
        ));
    }

    private String damageFormula(Player attacker,
                                 WeaponDefinition weapon,
                                 CheckResult result,
                                 DamageRoll damageRoll) {
        StringBuilder text = new StringBuilder();

        if (result == CheckResult.CRITICAL && weaponManager.criticalMaxDamage()) {
            text.append(weapon.damage()).append("(最大値)");
        } else {
            text.append(weapon.damage());
        }

        if (weapon.damageBonus()) {
            text.append(" + DB(")
                    .append(weaponManager.getDamageBonusLabel(attacker))
                    .append(")");
        }

        if (damageRoll.specialBonus() > 0) {
            text.append(" + Special(")
                    .append(damageRoll.specialBonus())
                    .append(")");
        }

        if (damageRoll.martialArtsMultiplier() > 1) {
            text.append(" ×")
                    .append(damageRoll.martialArtsMultiplier())
                    .append("(マーシャルアーツ)");
        }

        return text.toString();
    }

    private String getSkillName(String skillId) {
        SkillDefinition skill = skillManager.getSkill(skillId);
        return skill == null ? skillId : skill.getName();
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private record DamageRoll(
            int base,
            int db,
            int specialBonus,
            int martialArtsMultiplier,
            int total
    ) {
    }
}
