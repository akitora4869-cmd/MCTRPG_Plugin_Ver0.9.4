package Rin.TRPGCharacter;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public class Plugin extends JavaPlugin {

    private CharacterManager characterManager;
    private SkillManager skillManager;
    private SkillGrowthManager skillGrowthManager;
    private RollManager rollManager;
    private SkillEffectManager skillEffectManager;
    private HealthSyncManager healthSyncManager;
    private DangerEffectManager dangerEffectManager;
    private CompositeSkillManager compositeSkillManager;
    private JoinGuideManager joinGuideManager;
    private DeathManager deathManager;
    private DamageManager damageManager;
    private WeaponManager weaponManager;
    private DodgeManager dodgeManager;
    private DarkVisionManager darkVisionManager;
    private SwimManager swimManager;
    private WallKickManager wallKickManager;
    private MythosManager mythosManager;
    private ArtifactManager artifactManager;
    private CombatManager combatManager;
    private EnemyManager enemyManager;
    private EnemyCombatManager enemyCombatManager;
    private ArmorManager armorManager;
    private KeeperManager keeperManager;
    private KeeperBookManager keeperBookManager;
    private SessionManager sessionManager;
    private SessionClockManager sessionClockManager;
    private InputManager inputManager;
    private BookManager bookManager;
    private SidebarManager sidebarManager;
    private RandomStatManager randomStatManager;
    private DiceSoundManager diceSoundManager;
    private TimeStopManager timeStopManager;
    private ClueManager clueManager;
    private final java.util.Map<java.util.UUID, Long> resetPlayersConfirmUntil = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        characterManager = new CharacterManager(this);
        armorManager = new ArmorManager(this);
        keeperManager = new KeeperManager(this);
        sessionManager = new SessionManager(this);
        keeperBookManager = new KeeperBookManager(this, characterManager, keeperManager, sessionManager);
        skillManager = new SkillManager(this, characterManager);
        skillGrowthManager = new SkillGrowthManager(this, characterManager, skillManager);
        damageManager = new DamageManager(this, characterManager, armorManager, skillManager);
        weaponManager = new WeaponManager(this, characterManager, skillManager);
        dodgeManager = new DodgeManager(this, skillManager);
        mythosManager = new MythosManager(this, characterManager);
        artifactManager = new ArtifactManager(this, characterManager);
        enemyManager = new EnemyManager(this);
        combatManager = new CombatManager(this, characterManager, skillManager, weaponManager, armorManager, enemyManager, dodgeManager);
        enemyCombatManager = new EnemyCombatManager(this, characterManager, armorManager, enemyManager, dodgeManager, combatManager);
        healthSyncManager = new HealthSyncManager(this, characterManager);
        dangerEffectManager = new DangerEffectManager(this, characterManager);
        compositeSkillManager = new CompositeSkillManager(this, characterManager, skillManager);
        joinGuideManager = new JoinGuideManager(this, characterManager);
        skillEffectManager = new SkillEffectManager(this, characterManager);
        rollManager = new RollManager(this, skillEffectManager);
        inputManager = new InputManager(this, characterManager, skillManager);
        bookManager = new BookManager(this, characterManager, skillManager);
        deathManager = new DeathManager(this, characterManager, bookManager);
        sidebarManager = new SidebarManager(this, characterManager);
        randomStatManager = new RandomStatManager(this, characterManager);
        diceSoundManager = new DiceSoundManager(this);
        timeStopManager = new TimeStopManager(this, keeperManager);
        sessionClockManager = new SessionClockManager(this, sessionManager, keeperManager);
        clueManager = new ClueManager(this);
        darkVisionManager = new DarkVisionManager(this, characterManager, skillManager);
        swimManager = new SwimManager(this, characterManager, skillManager);
        wallKickManager = new WallKickManager(this, characterManager, skillManager);
        sidebarManager.start();
        dangerEffectManager.start();
        sessionClockManager.start();
        darkVisionManager.start();
        swimManager.start();
        mythosManager.start();

        getServer().getPluginManager().registerEvents(
                new ChatInputListener(this, inputManager), this
        );
        getServer().getPluginManager().registerEvents(
                new HealthListener(this, healthSyncManager), this
        );
        getServer().getPluginManager().registerEvents(
                deathManager, this
        );
        getServer().getPluginManager().registerEvents(
                combatManager, this
        );
        getServer().getPluginManager().registerEvents(
                enemyManager, this
        );
        getServer().getPluginManager().registerEvents(
                enemyCombatManager, this
        );
        getServer().getPluginManager().registerEvents(
                damageManager, this
        );
        getServer().getPluginManager().registerEvents(
                new KeeperListener(keeperManager, keeperBookManager), this
        );
        getServer().getPluginManager().registerEvents(
                new JoinListener(this, bookManager, joinGuideManager), this
        );
        getServer().getPluginManager().registerEvents(
                new BookInteractListener(bookManager), this
        );
        getServer().getPluginManager().registerEvents(
                new TimeStopListener(timeStopManager), this
        );
        getServer().getPluginManager().registerEvents(
                new TimeStopCommandListener(timeStopManager), this
        );

        getServer().getPluginManager().registerEvents(
                new ClueListener(clueManager), this
        );
        getServer().getPluginManager().registerEvents(
                artifactManager, this
        );
        getServer().getPluginManager().registerEvents(
                wallKickManager, this
        );

        registerCommands();

        getLogger().info("TRPGCharacter enabled!");
    }

    @Override
    public void onDisable() {
        if (sessionClockManager != null) {
            sessionClockManager.shutdown();
        }

        if (darkVisionManager != null) {
            darkVisionManager.shutdown();
        }

        if (swimManager != null) {
            swimManager.shutdown();
        }

        if (mythosManager != null) {
            mythosManager.shutdown();
        }

        if (characterManager != null) {
            characterManager.save();
        }

        getLogger().info("TRPGCharacter disabled!");
    }

    private void registerCommands() {
        PluginCommand status = getCommand("status");
        PluginCommand roll = getCommand("roll");
        PluginCommand trpgedit = getCommand("trpgedit");
        PluginCommand trpgroll = getCommand("trpgroll");
        PluginCommand trpgcombo = getCommand("trpgcombo");
        PluginCommand kp = getCommand("kp");
        PluginCommand kpbook = getCommand("kpbook");
        PluginCommand session = getCommand("session");
        PluginCommand create = getCommand("create");
        PluginCommand reset = getCommand("reset");
        PluginCommand stop = getCommand("stop");
        PluginCommand clue = getCommand("clue");
        PluginCommand trpgattack = getCommand("trpgattack");
        PluginCommand mythos = getCommand("mythos");
        PluginCommand trpgskill = getCommand("trpgskill");
        PluginCommand artifact = getCommand("artifact");

        if (status != null) {
            status.setExecutor(this::handleStatus);
        }

        if (roll != null) {
            roll.setExecutor(this::handleRoll);
        }

        if (trpgedit != null) {
            trpgedit.setExecutor(this::handleEdit);
        }

        if (trpgroll != null) {
            trpgroll.setExecutor(this::handleSheetRoll);
        }

        if (trpgcombo != null) {
            trpgcombo.setExecutor(this::handleCompositeRoll);
        }

        if (kp != null) {
            kp.setExecutor(this::handleKeeper);
        }

        if (kpbook != null) {
            kpbook.setExecutor(this::handleKeeperBook);
        }

        if (session != null) {
            session.setExecutor(this::handleSession);
        }

        if (create != null) {
            create.setExecutor(this::handleCreate);
        }

        if (reset != null) {
            reset.setExecutor(this::handleReset);
        }

        if (stop != null) {
            stop.setExecutor(this::handleStop);
        }

        if (clue != null) {
            clue.setExecutor(this::handleClue);
        }

        if (trpgattack != null) {
            trpgattack.setExecutor(this::handleTrpgAttack);
        }

        if (mythos != null) {
            mythos.setExecutor(this::handleMythos);
        }

        if (trpgskill != null) {
            trpgskill.setExecutor(this::handleTrpgSkill);
        }

        if (artifact != null) {
            artifact.setExecutor(this::handleArtifact);
        }
    }

    private boolean handleArtifact(CommandSender sender,
                                   Command command,
                                   String label,
                                   String[] args) {
        boolean canManage;

        if (sender instanceof Player player) {
            canManage = player.isOp()
                    || player.hasPermission("trpg.admin")
                    || keeperManager.isKeeper(player);
        } else {
            canManage = true;
        }

        if (!canManage) {
            sender.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&5[アーティファクト] &f登録一覧:"));

            for (String id : artifactManager.getIds()) {
                ArtifactDefinition definition = artifactManager.getDefinition(id);
                if (definition != null) {
                    sender.sendMessage(color(
                            "&7- &d" + id + " &7: &f" + definition.name()
                    ));
                }
            }
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&c対象プレイヤーがオンラインではありません。"));
                return true;
            }

            org.bukkit.inventory.ItemStack item =
                    artifactManager.createItem(args[2]);

            if (item == null) {
                sender.sendMessage(color("&cアーティファクトIDが見つかりません。"));
                return true;
            }

            java.util.Map<Integer, org.bukkit.inventory.ItemStack> overflow =
                    target.getInventory().addItem(item);

            for (org.bukkit.inventory.ItemStack extra : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), extra);
            }

            ArtifactDefinition definition = artifactManager.getDefinition(args[2]);

            sender.sendMessage(color(
                    "&a" + target.getName() + " に &d"
                            + definition.name() + " &aを渡しました。"
            ));
            target.sendMessage(color(
                    "&5[アーティファクト] &f"
                            + definition.name() + " &dを入手しました。"
            ));
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(color("&c対象プレイヤーがオンラインではありません。"));
                return true;
            }

            String id = args[2].toLowerCase(Locale.ROOT);
            int removed = 0;

            org.bukkit.inventory.ItemStack[] contents =
                    target.getInventory().getContents();

            for (int i = 0; i < contents.length; i++) {
                org.bukkit.inventory.ItemStack item = contents[i];
                ArtifactDefinition definition = artifactManager.getDefinition(item);

                if (definition != null && definition.id().equals(id)) {
                    target.getInventory().setItem(i, null);
                    removed++;
                }
            }

            sender.sendMessage(color(
                    "&a" + target.getName() + " から "
                            + removed + " 個のアーティファクトを削除しました。"
            ));
            return true;
        }

        sender.sendMessage(color("&e/artifact list"));
        sender.sendMessage(color("&e/artifact give <player> <id>"));
        sender.sendMessage(color("&e/artifact remove <player> <id>"));
        return true;
    }

    private boolean handleTrpgSkill(CommandSender sender,
                                    Command command,
                                    String label,
                                    String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("hobby")) {
            String skillId = args[1];

            if (!skillManager.hasSkill(skillId)) {
                player.sendMessage(color("&c技能が見つかりません。"));
                return true;
            }

            inputManager.beginHobbySkillAdd(player, skillId);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("hobby-reset")) {
            characterManager.resetHobbyAllocations(player);
            player.sendMessage(color(
                    "&a趣味ポイントの割り振りをすべてリセットしました。"
                            + " &7残り: &b"
                            + characterManager.getHobbyPointRemaining(player)
            ));

            getServer().getScheduler().runTaskLater(
                    this,
                    () -> bookManager.openSheet(player),
                    2L
            );
            return true;
        }

        player.sendMessage(color("&e/trpgskill hobby <技能ID>"));
        player.sendMessage(color("&e/trpgskill hobby-reset"));
        return true;
    }

    private boolean handleMythos(CommandSender sender,
                                 Command command,
                                 String label,
                                 String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        boolean canManage = player.isOp()
                || player.hasPermission("trpg.admin")
                || keeperManager.isKeeper(player);

        if (!canManage) {
            player.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            player.sendMessage(color("&5[神話生物] &f登録一覧:"));
            for (String id : mythosManager.getIds()) {
                MythosCreatureDefinition def = mythosManager.getDefinition(id);
                if (def != null) {
                    player.sendMessage(color(
                            "&7- &d" + id + " &7: &f" + def.name()
                    ));
                }
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            org.bukkit.entity.LivingEntity entity =
                    mythosManager.summon(player, args[1]);

            if (entity == null) {
                player.sendMessage(color(
                        "&c神話生物IDが見つからないか、召喚できないEntityです。"
                ));
                return true;
            }

            MythosCreatureDefinition def =
                    mythosManager.getDefinition(args[1]);

            player.sendMessage(color(
                    "&5[神話生物] &f" + def.name() + " &aを召喚しました。"
            ));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("sanreset")) {
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(color("&c対象プレイヤーがオンラインではありません。"));
                return true;
            }

            mythosManager.resetEncounter(target);
            player.sendMessage(color(
                    "&a" + target.getName() + " の神話生物遭遇記録をリセットしました。"
            ));
            return true;
        }

        player.sendMessage(color("&e/mythos list"));
        player.sendMessage(color("&e/mythos summon <id>"));
        player.sendMessage(color("&e/mythos sanreset <player>"));
        return true;
    }

    private boolean handleTrpgAttack(CommandSender sender,
                                     Command command,
                                     String label,
                                     String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (args.length != 1
                || (!args[0].equalsIgnoreCase("fist")
                && !args[0].equalsIgnoreCase("kick"))) {
            player.sendMessage(color("&c素手攻撃は「こぶし」または「キック」を選択してください。"));
            return true;
        }

        characterManager.setUnarmedAttack(player, args[0]);

        String selectedName = characterManager.getUnarmedAttackName(player);
        player.sendMessage(color(
                "&a素手攻撃を「" + selectedName + "」に設定しました。"
        ));

        bookManager.openSheet(player);
        return true;
    }

    private boolean handleStatus(CommandSender sender,
                                 Command command,
                                 String label,
                                 String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (args.length == 0) {
            bookManager.openSheet(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("revive")) {
            if (!player.hasPermission("trpg.admin")) {
                player.sendMessage(color("&c権限がありません。"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(color("&c使い方: /status revive <プレイヤー名>"));
                return true;
            }

            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(color("&c対象プレイヤーがオンラインではありません。"));
                return true;
            }

            deathManager.revive(target);
            player.sendMessage(color("&a" + target.getName() + " を復活させました。"));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            player.getInventory().addItem(bookManager.createSheet(player));
            player.sendMessage(color("&6[TRPG] &a探索者シートの本を渡しました。"));
            return true;
        }

        if (args[0].equalsIgnoreCase("random-confirm")) {
            bookManager.openRandomConfirm(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("random")) {
            randomStatManager.generate(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("trpg.admin")) {
                player.sendMessage(color("&c権限がありません。"));
                return true;
            }

            reloadConfig();
            skillManager.reload();
            darkVisionManager.reload();
            swimManager.reload();
            mythosManager.reload();
            artifactManager.reload();
            weaponManager.reload();
            dodgeManager.reload();
            enemyManager.reload();
            damageManager.reload();
            armorManager.reload();
            keeperManager.reload();
            sessionManager.reload();
            characterManager.reload();
            clueManager.reload();

            player.sendMessage(color("&6[TRPG] &a設定を再読み込みしました。"));
            return true;
        }

        player.sendMessage(color("&e/status &7- 探索者シートを開く"));
        player.sendMessage(color("&e/status give &7- 本を受け取る"));
        if (player.hasPermission("trpg.admin")) {
            player.sendMessage(color("&e/status revive <player> &7- 死亡した探索者を復活"));
        }
        player.sendMessage(color("&e/status random &7- CoC第6版標準式で能力値を一括生成"));
        if (player.hasPermission("trpg.admin")) {
            player.sendMessage(color("&e/status reload &7- 設定を再読み込み"));
        }
        return true;
    }

    private boolean handleRoll(CommandSender sender,
                               Command command,
                               String label,
                               String[] args) {
        if (args.length != 1) {
            sender.sendMessage(color("&c使い方: /roll <XdY>  例: /roll 1d100"));
            return true;
        }

        rollManager.rollDice(sender, args[0]);
        return true;
    }

    private boolean handleEdit(CommandSender sender,
                               Command command,
                               String label,
                               String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length != 2) {
            return true;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        String id = args[1];

        if (type.equals("stat")) {
            if (!characterManager.isValidStat(id)) {
                player.sendMessage(color("&c能力値が見つかりません。"));
                return true;
            }

            inputManager.beginStat(player, id.toUpperCase(Locale.ROOT));
            return true;
        }

        if (type.equals("skill")) {
            boolean canDirectEdit = player.isOp()
                    || player.hasPermission("trpg.admin")
                    || keeperManager.isKeeper(player);

            if (!canDirectEdit) {
                player.sendMessage(color(
                        "&c技能値の直接変更はKP/管理者のみ使用できます。"
                                + " &7探索者は技能ページの [趣味+] を使用してください。"
                ));
                return true;
            }

            if (!skillManager.hasSkill(id)) {
                player.sendMessage(color("&c技能が見つかりません。"));
                return true;
            }

            inputManager.beginSkill(player, id);
            return true;
        }

        if (type.equals("name") && id.equalsIgnoreCase("character")) {
            inputManager.beginCharacterName(player);
            return true;
        }

        if (type.equals("san") && id.equalsIgnoreCase("current")) {
            inputManager.beginCurrentSan(player);
            return true;
        }

        if (type.equals("hp") && id.equalsIgnoreCase("current")) {
            inputManager.beginCurrentHp(player);
            return true;
        }
        if (type.equals("hp") && id.equalsIgnoreCase("damage")) {
            inputManager.beginHpDamage(player);
            return true;
        }
        if (type.equals("hp") && id.equalsIgnoreCase("heal")) {
            inputManager.beginHpHeal(player);
            return true;
        }

        if (type.equals("mp") && id.equalsIgnoreCase("current")) {
            inputManager.beginCurrentMp(player);
            return true;
        }
        if (type.equals("mp") && id.equalsIgnoreCase("spend")) {
            inputManager.beginMpSpend(player);
            return true;
        }
        if (type.equals("mp") && id.equalsIgnoreCase("recover")) {
            inputManager.beginMpRecover(player);
            return true;
        }

        if (type.equals("sanloss") && id.equalsIgnoreCase("apply")) {
            inputManager.beginSanLoss(player);
            return true;
        }

        return true;
    }

    private boolean handleSheetRoll(CommandSender sender,
                                    Command command,
                                    String label,
                                    String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length != 2) {
            return true;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        String id = args[1];

        if (type.equals("stat")) {
            if (!characterManager.isValidStat(id)) {
                return true;
            }

            String stat = id.toUpperCase(Locale.ROOT);
            int target = characterManager.getStat(player, stat) * 5;
            rollManager.rollCheck(player, stat + "×5", target);
            return true;
        }

        if (type.equals("skill")) {
            SkillDefinition skill = skillManager.getSkill(id);
            if (skill == null) {
                return true;
            }

            int target = skillManager.getSkillValue(player, id);
            rollManager.rollSkillCheck(player, id, skill.getName(), target);
            return true;
        }

        if (type.equals("derived")) {
            int target = characterManager.getDerived(player, id);
            String name = characterManager.getDerivedName(id);
            rollManager.rollCheck(player, name, target);
            return true;
        }

        if (type.equals("san") && id.equalsIgnoreCase("current")) {
            int target = characterManager.getCurrentSan(player);
            rollManager.rollCheck(player, "SANチェック", target);
            return true;
        }

        return true;
    }

    private boolean handleClue(CommandSender sender,
                               Command command,
                               String label,
                               String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.isOp() && !player.hasPermission("trpg.admin") && !keeperManager.isKeeper(player)) {
            player.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("mark")) {
            String clueId = args[1];

            if (!clueManager.clueExists(clueId)) {
                player.sendMessage(color("&cclues.yml にその clue-id がありません。"));
                return true;
            }

            org.bukkit.entity.Entity target = player.getTargetEntity(5);

            if (!(target instanceof org.bukkit.entity.ArmorStand stand)) {
                player.sendMessage(color("&c5ブロック以内のアーマースタンドを見て実行してください。"));
                return true;
            }

            clueManager.markArmorStand(stand, clueId);
            player.sendMessage(color("&a情報ポイントを登録しました: &f" + clueId));
            return true;
        }

        if (args.length == 2) {
            double radius;
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(color("&c範囲は数字で指定してください。"));
                return true;
            }

            if (radius <= 0 || radius > 100) {
                player.sendMessage(color("&c範囲は1～100で指定してください。"));
                return true;
            }

            switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
                case "hide" -> {
                    int count = clueManager.hideNearby(player, radius);
                    player.sendMessage(color("&a周囲のアーマースタンド &f" + count + "体 &aを透明化しました。"));
                    return true;
                }
                case "show" -> {
                    int count = clueManager.showNearby(player, radius);
                    player.sendMessage(color("&a周囲のアーマースタンド &f" + count + "体 &aを再表示しました。"));
                    return true;
                }
                case "protect" -> {
                    int count = clueManager.protectNearby(player, radius);
                    player.sendMessage(color("&a周囲のアーマースタンド &f" + count + "体 &aを破壊不能にしました。"));
                    return true;
                }
                case "unprotect" -> {
                    int count = clueManager.unprotectNearby(player, radius);
                    player.sendMessage(color("&e周囲のアーマースタンド &f" + count + "体 &eの保護を解除しました。"));
                    return true;
                }
                case "setup" -> {
                    int count = clueManager.setupNearby(player, radius);
                    player.sendMessage(color("&d周囲のアーマースタンド &f" + count + "体 &dを透明化＋破壊不能にしました。"));
                    return true;
                }
            }
        }

        player.sendMessage(color("&e/clue mark <clue-id> &7- 情報ポイント登録"));
        player.sendMessage(color("&e/clue hide <範囲> &7- 一括透明化"));
        player.sendMessage(color("&e/clue show <範囲> &7- 一括再表示"));
        player.sendMessage(color("&e/clue protect <範囲> &7- 一括保護"));
        player.sendMessage(color("&e/clue unprotect <範囲> &7- 保護解除"));
        player.sendMessage(color("&e/clue setup <範囲> &7- 透明化＋保護"));
        return true;
    }

    private boolean handleStop(CommandSender sender,
                               Command command,
                               String label,
                               String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (!timeStopManager.canAct(player)) {
            player.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("time")) {
            player.sendMessage(color("&c使い方: /stop time"));
            return true;
        }

        timeStopManager.toggle(player);
        return true;
    }

    private boolean handleReset(CommandSender sender,
                                Command command,
                                String label,
                                String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (!player.isOp() && !player.hasPermission("trpg.admin")) {
            player.sendMessage(color("&cこのコマンドは管理者のみ使用できます。"));
            return true;
        }

        // players.yml 内の探索者データを全初期化
        if (args.length == 1 && args[0].equalsIgnoreCase("players")) {
            long now = System.currentTimeMillis();
            long until = resetPlayersConfirmUntil.getOrDefault(player.getUniqueId(), 0L);

            if (now > until) {
                resetPlayersConfirmUntil.put(player.getUniqueId(), now + 15000L);
                player.sendMessage(color("&c警告: players.yml の探索者情報を全員分初期化します。"));
                player.sendMessage(color("&e15秒以内にもう一度 &f/reset players &eを実行すると確定します。"));
                return true;
            }

            resetPlayersConfirmUntil.remove(player.getUniqueId());
            characterManager.resetAllPlayers();

            for (Player target : getServer().getOnlinePlayers()) {
                healthSyncManager.sync(target);
                sidebarManager.updatePlayer(target);
                target.sendMessage(color("&6[TRPG] &e探索者情報が全体初期化されました。"));
            }

            player.sendMessage(color("&aplayers.yml の探索者情報を全員分初期化しました。"));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("pc")) {
            Player target = getServer().getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(color("&c対象プレイヤーがオンラインではありません。"));
                return true;
            }

            characterManager.resetPlayer(target);
            healthSyncManager.sync(target);
            sidebarManager.updatePlayer(target);

            target.sendMessage(color("&6[TRPG] &e探索者情報が初期化されました。"));
            target.sendMessage(color("&7探索者シートから能力値を再設定してください。"));
            player.sendMessage(color("&a" + target.getName() + " の探索者情報を初期化しました。"));
            return true;
        }

        player.sendMessage(color("&c使い方: /reset pc <プレイヤー名> または /reset players"));
        return true;
    }

    private boolean handleCreate(CommandSender sender,
                                 Command command,
                                 String label,
                                 String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (!keeperManager.isKeeper(player)
                && !player.hasPermission("trpg.admin")
                && !player.isOp()) {
            player.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
            return true;
        }

        if (args.length < 3 || !args[0].equalsIgnoreCase("session")) {
            player.sendMessage(color("&c使い方: /create session <セッション名> <時間帯>"));
            player.sendMessage(color("&7時間帯: 早朝 / 朝 / 昼 / 夕方 / 夜 / 深夜"));
            return true;
        }

        if (sessionManager.isActive()) {
            player.sendMessage(color("&cすでにセッション「&f"
                    + sessionManager.getSessionName()
                    + "&c」が開催中です。"));
            player.sendMessage(color("&7終了する場合は /session end を使用してください。"));
            return true;
        }

        String periodInput = args[args.length - 1];
        SessionTimePeriod period = SessionTimePeriod.fromInput(periodInput);

        if (period == null) {
            player.sendMessage(color("&c最後の引数に時間帯を指定してください。"));
            player.sendMessage(color("&7早朝 / 朝 / 昼 / 夕方 / 夜 / 深夜"));
            player.sendMessage(color("&7例: /create session 悪霊の家 夜"));
            return true;
        }

        String sessionName = String.join(" ",
                java.util.Arrays.copyOfRange(args, 1, args.length - 1)).trim();

        if (sessionName.isBlank()) {
            player.sendMessage(color("&cセッション名を入力してください。"));
            return true;
        }

        if (sessionName.length() > 50) {
            player.sendMessage(color("&cセッション名は50文字以内にしてください。"));
            return true;
        }

        if (!sessionManager.createSession(sessionName, player, period)) {
            player.sendMessage(color("&cセッションを作成できませんでした。"));
            return true;
        }

        sessionClockManager.beginSession(period);

        getServer().broadcastMessage(color("&6[SESSION] &dセッション「&f"
                + sessionName + "&d」を開始しました。"));
        getServer().broadcastMessage(color("&7開始時刻: &f"
                + period.displayName() + " " + sessionClockManager.getDisplayTime()));
        getServer().broadcastMessage(color("&7PLは &f/session join &7で参加してください。"));
        getServer().broadcastMessage(color("&7シナリオ時計は停止状態です。KPが &f/session time start &7で進行できます。"));
        return true;
    }

    private boolean handleSession(CommandSender sender,
                                  Command command,
                                  String label,
                                  String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        boolean canManage = keeperManager.isKeeper(player)
                || player.hasPermission("trpg.admin")
                || player.isOp();

        if (args.length == 0) {
            if (sessionManager.isActive()) {
                player.sendMessage(color("&6[SESSION] &f現在: &d"
                        + sessionManager.getSessionName()
                        + " &7(参加 " + sessionManager.getParticipantCount() + "人)"));
                player.sendMessage(color("&7時間: &b"
                        + sessionClockManager.getDisplayPeriod() + " "
                        + sessionClockManager.getDisplayTime()
                        + " &7/ " + sessionClockManager.getClockStatusText()));
            } else {
                player.sendMessage(color("&6[SESSION] &7現在開催中のセッションはありません。"));
            }

            player.sendMessage(color("&e/session join &7- 現在のセッションに参加"));
            player.sendMessage(color("&e/session leave &7- セッションから退出"));

            if (canManage) {
                player.sendMessage(color("&e/session list &7- 参加者一覧"));
                player.sendMessage(color("&e/session time <時間帯> &7- 時間帯を変更"));
                player.sendMessage(color("&e/session time start|pause|resume &7- 時計操作"));
                player.sendMessage(color("&e/session time speed <1-600> &7- 進行速度"));
                player.sendMessage(color("&e/session time add <分> &7- 時間を手動で進める"));
                player.sendMessage(color("&e/session end &7- セッション終了＋ログ保存"));
                player.sendMessage(color("&e/create session <名前> <時間帯> &7- 新しいセッションを作成"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (!sessionManager.isActive()) {
                player.sendMessage(color("&c現在開催中のセッションはありません。"));
                player.sendMessage(color("&7KPが /create session <セッション名> <時間帯> で作成してください。"));
                return true;
            }

            if (keeperManager.isKeeper(player)) {
                player.sendMessage(color("&cKPは探索者参加者として登録できません。"));
                return true;
            }

            if (sessionManager.isParticipant(player)) {
                player.sendMessage(color("&eすでにセッション参加中です。"));
                return true;
            }

            if (!sessionManager.join(player)) {
                player.sendMessage(color("&cセッションに参加できませんでした。"));
                return true;
            }

            player.sendMessage(color("&aセッション「&f"
                    + sessionManager.getSessionName()
                    + "&a」に参加しました。"));
            getServer().broadcastMessage(color("&6[SESSION] &f"
                    + characterManager.getCharacterName(player)
                    + " &7がセッションに参加しました。"));
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            if (!sessionManager.isParticipant(player)) {
                player.sendMessage(color("&e現在セッションに参加していません。"));
                return true;
            }

            sessionManager.leave(player);
            player.sendMessage(color("&7セッションから退出しました。"));
            getServer().broadcastMessage(color("&6[SESSION] &f"
                    + characterManager.getCharacterName(player)
                    + " &7がセッションから退出しました。"));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!canManage) {
                player.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
                return true;
            }

            if (!sessionManager.isActive()) {
                player.sendMessage(color("&e現在開催中のセッションはありません。"));
                return true;
            }

            player.sendMessage(color("&5[KP] &dセッション: &f" + sessionManager.getSessionName()));
            player.sendMessage(color("&7時間: &b"
                    + sessionClockManager.getDisplayPeriod() + " "
                    + sessionClockManager.getDisplayTime()
                    + " &7/ " + sessionClockManager.getClockStatusText()));
            player.sendMessage(color("&d現在オンライン中の参加探索者"));
            boolean found = false;

            for (Player target : getServer().getOnlinePlayers()) {
                if (sessionManager.isParticipant(target)) {
                    found = true;
                    player.sendMessage(color("&f- "
                            + characterManager.getCharacterName(target)
                            + " &7(" + target.getName() + ")"
                            + " HP " + characterManager.getCurrentHp(target)
                            + "/" + characterManager.getHp(target)
                            + " MP " + characterManager.getCurrentMp(target)
                            + "/" + characterManager.getMp(target)
                            + " SAN " + characterManager.getCurrentSan(target)
                            + "/" + characterManager.getSan(target)));
                }
            }

            if (!found) {
                player.sendMessage(color("&7現在オンラインの参加者はいません。"));
            }

            return true;
        }

        if (args[0].equalsIgnoreCase("time")) {
            if (!canManage) {
                player.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
                return true;
            }

            if (!sessionManager.isActive()) {
                player.sendMessage(color("&e現在開催中のセッションはありません。"));
                return true;
            }

            if (args.length == 1) {
                player.sendMessage(color("&6[SESSION TIME] &b"
                        + sessionClockManager.getDisplayPeriod() + " "
                        + sessionClockManager.getDisplayTime()
                        + " &7/ " + sessionClockManager.getClockStatusText()));
                player.sendMessage(color("&7/session time <早朝|朝|昼|夕方|夜|深夜>"));
                player.sendMessage(color("&7/session time start|pause|resume"));
                player.sendMessage(color("&7/session time speed <1-600>"));
                player.sendMessage(color("&7/session time add <分>"));
                return true;
            }

            SessionTimePeriod period = SessionTimePeriod.fromInput(args[1]);
            if (period != null && args.length == 2) {
                sessionClockManager.setTimePeriod(period, player.getName());
                getServer().broadcastMessage(color("&6[SESSION TIME] &f"
                        + period.displayName() + " "
                        + sessionClockManager.getDisplayTime()
                        + " &7へ変更しました。"));
                return true;
            }

            if (args[1].equalsIgnoreCase("start") && args.length == 2) {
                sessionClockManager.startClock(player.getName());
                getServer().broadcastMessage(color("&6[SESSION TIME] &aシナリオ時計を開始しました。"));
                return true;
            }

            if (args[1].equalsIgnoreCase("pause") && args.length == 2) {
                sessionClockManager.pauseClock(player.getName());
                getServer().broadcastMessage(color("&6[SESSION TIME] &eシナリオ時計を停止しました。"));
                return true;
            }

            if (args[1].equalsIgnoreCase("resume") && args.length == 2) {
                sessionClockManager.resumeClock(player.getName());
                getServer().broadcastMessage(color("&6[SESSION TIME] &aシナリオ時計を再開しました。"));
                return true;
            }

            if (args[1].equalsIgnoreCase("speed") && args.length == 3) {
                int speed;
                try {
                    speed = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(color("&c速度は数字で指定してください。"));
                    return true;
                }

                if (speed < 1 || speed > 600) {
                    player.sendMessage(color("&c速度は1～600で指定してください。"));
                    return true;
                }

                sessionClockManager.setSpeed(speed, player.getName());
                player.sendMessage(color("&a進行速度を &f" + speed
                        + "ゲーム内分 / 現実1分 &aに変更しました。"));
                return true;
            }

            if (args[1].equalsIgnoreCase("add") && args.length == 3) {
                int minutes;
                try {
                    minutes = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(color("&c追加時間は数字で指定してください。"));
                    return true;
                }

                if (minutes < -1440 || minutes > 1440 || minutes == 0) {
                    player.sendMessage(color("&c追加時間は -1440～1440 の範囲（0以外）で指定してください。"));
                    return true;
                }

                sessionClockManager.addMinutes(minutes, player.getName());
                getServer().broadcastMessage(color("&6[SESSION TIME] &f"
                        + (minutes > 0 ? "+" : "") + minutes + "分 &7→ &b"
                        + sessionClockManager.getDisplayPeriod() + " "
                        + sessionClockManager.getDisplayTime()));
                return true;
            }

            player.sendMessage(color("&c時間コマンドの指定が正しくありません。"));
            player.sendMessage(color("&7/session time <早朝|朝|昼|夕方|夜|深夜>"));
            player.sendMessage(color("&7/session time start|pause|resume"));
            player.sendMessage(color("&7/session time speed <1-600>"));
            player.sendMessage(color("&7/session time add <分>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("end")) {
            if (!canManage) {
                player.sendMessage(color("&cKPまたは管理者のみ使用できます。"));
                return true;
            }

            if (!sessionManager.isActive()) {
                player.sendMessage(color("&e現在開催中のセッションはありません。"));
                return true;
            }

            String endedName = sessionManager.getSessionName();
            java.io.File logFile = sessionManager.endSession(player, characterManager);

            if (logFile == null) {
                player.sendMessage(color("&cセッションログを保存できなかったため、終了処理を中止しました。"));
                return true;
            }

            sessionClockManager.onSessionEnd();

            getServer().broadcastMessage(color("&6[SESSION] &dセッション「&f"
                    + endedName + "&d」を終了しました。"));
            player.sendMessage(color("&aセッションログを保存しました。"));
            player.sendMessage(color("&7plugins/TRPGCharacter/session-logs/"
                    + logFile.getName()));
            return true;
        }

        player.sendMessage(color("&c使い方: /session <join|leave|list|time|end>"));
        return true;
    }

    private boolean handleKeeper(CommandSender sender,
                                 Command command,
                                 String label,
                                 String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (!player.isOp() && !player.hasPermission("trpg.admin")) {
            player.sendMessage(color("&cこのコマンドは管理者のみ使用できます。"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(color("&c使い方: /kp <プレイヤー名>"));
            return true;
        }

        Player target = getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(color("&c対象プレイヤーがオンラインではありません。"));
            return true;
        }

        keeperManager.grantKeeper(target);
        target.getInventory().addItem(keeperBookManager.createKeeperBook(target));
        target.sendMessage(color("&5[KP] &dキーパー権限が付与されました。"));
        target.sendMessage(color("&7KPブックを右クリックすると最新情報を確認できます。"));
        player.sendMessage(color("&a" + target.getName() + " にKP権限を付与しました。"));
        return true;
    }

    private boolean handleKeeperBook(CommandSender sender,
                                     Command command,
                                     String label,
                                     String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!keeperManager.isKeeper(player)) {
            player.sendMessage(color("&cKP権限がありません。"));
            return true;
        }

        keeperBookManager.openKeeperBook(player);
        return true;
    }

    private boolean handleCompositeRoll(CommandSender sender,
                                        Command command,
                                        String label,
                                        String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length != 1) {
            return true;
        }

        compositeSkillManager.roll(player, args[0]);
        return true;
    }

    public ClueManager getClueManager() {
        return clueManager;
    }

    public TimeStopManager getTimeStopManager() {
        return timeStopManager;
    }

    public SessionClockManager getSessionClockManager() {
        return sessionClockManager;
    }


    public DiceSoundManager getDiceSoundManager() {
        return diceSoundManager;
    }

    public DarkVisionManager getDarkVisionManager() {
        return darkVisionManager;
    }

    public SwimManager getSwimManager() {
        return swimManager;
    }

    public MythosManager getMythosManager() {
        return mythosManager;
    }

    public ArtifactManager getArtifactManager() {
        return artifactManager;
    }


    public ArmorManager getArmorManager() {
        return armorManager;
    }

    public KeeperManager getKeeperManager() {
        return keeperManager;
    }

    public BookManager getBookManager() {
        return bookManager;
    }

    public HealthSyncManager getHealthSyncManager() {
        return healthSyncManager;
    }

    public EnemyManager getEnemyManager() {
        return enemyManager;
    }


    public SidebarManager getSidebarManager() {
        return sidebarManager;
    }

    public SkillGrowthManager getSkillGrowthManager() {
        return skillGrowthManager;
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
