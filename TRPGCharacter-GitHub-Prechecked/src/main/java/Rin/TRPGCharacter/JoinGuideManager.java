package Rin.TRPGCharacter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class JoinGuideManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;

    public JoinGuideManager(Plugin plugin, CharacterManager characterManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
    }

    public void sendJoinGuide(Player player) {
        if (!plugin.getConfig().getBoolean("join-guide.enabled", true)) {
            return;
        }

        if (characterManager.hasConfiguredStats(player)) {
            sendReturningGuide(player);
        } else {
            sendFirstSetupGuide(player);
        }
    }

    private void sendFirstSetupGuide(Player player) {
        String serverName = plugin.getConfig().getString("sidebar.server-name", "MCTRPG Sever");

        player.sendMessage(Component.text("================================", NamedTextColor.DARK_GRAY));
        player.sendMessage(
                Component.text(serverName, NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
        );
        player.sendMessage(Component.text("MinecraftでCoC第6版を遊ぶTRPGサーバーです。", NamedTextColor.WHITE));
        player.sendMessage(Component.empty());

        player.sendMessage(
                Component.text("探索者のステータスがまだ設定されていません。", NamedTextColor.YELLOW)
                        .decorate(TextDecoration.BOLD)
        );

        player.sendMessage(Component.text("最初にすること", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("1. 手持ちの「探索者シート」を開く", NamedTextColor.WHITE));
        player.sendMessage(Component.text("2. キャラクター名を設定する", NamedTextColor.WHITE));
        player.sendMessage(Component.text("3. 「能力値一括生成」で能力値を決める", NamedTextColor.WHITE));
        player.sendMessage(Component.text("4. 技能値を設定する", NamedTextColor.WHITE));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("主な機能", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("・探索者シート / HP・MP・SAN管理", NamedTextColor.GRAY));
        player.sendMessage(Component.text("・1d100技能判定 / 複合技能判定", NamedTextColor.GRAY));
        player.sendMessage(Component.text("・技能成功時のMinecraft内効果", NamedTextColor.GRAY));
        player.sendMessage(Component.text("・Minecraft体力とTRPG HPの同期", NamedTextColor.GRAY));

        player.sendMessage(Component.empty());
        player.sendMessage(openSheetButton());
        player.sendMessage(Component.text("/roll 1d100 でもダイスを振れます。", NamedTextColor.GRAY));
        player.sendMessage(Component.text("================================", NamedTextColor.DARK_GRAY));
    }

    private void sendReturningGuide(Player player) {
        String characterName = characterManager.getCharacterName(player);

        player.sendMessage(Component.text("================================", NamedTextColor.DARK_GRAY));
        player.sendMessage(
                Component.text("おかえりなさい、", NamedTextColor.GRAY)
                        .append(Component.text(characterName, NamedTextColor.GOLD)
                                .decorate(TextDecoration.BOLD))
        );

        player.sendMessage(
                Component.text("HP ", NamedTextColor.RED)
                        .append(Component.text(
                                characterManager.getCurrentHp(player)
                                        + " / "
                                        + characterManager.getHp(player),
                                NamedTextColor.WHITE
                        ))
        );

        player.sendMessage(
                Component.text("MP ", NamedTextColor.BLUE)
                        .append(Component.text(
                                characterManager.getCurrentMp(player)
                                        + " / "
                                        + characterManager.getMp(player),
                                NamedTextColor.WHITE
                        ))
        );

        player.sendMessage(
                Component.text("SAN ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(
                                characterManager.getCurrentSan(player)
                                        + " / "
                                        + characterManager.getSan(player),
                                NamedTextColor.WHITE
                        ))
        );

        player.sendMessage(Component.empty());
        player.sendMessage(openSheetButton());
        player.sendMessage(Component.text("================================", NamedTextColor.DARK_GRAY));
    }

    private Component openSheetButton() {
        return Component.text("[探索者シートを開く]", NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/status"))
                .hoverEvent(HoverEvent.showText(
                        Component.text("クリックして探索者シートを開く", NamedTextColor.GRAY)
                ));
    }
}
