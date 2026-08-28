package Rin.TRPGCharacter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class KeeperBookManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final KeeperManager keeperManager;
    private final SessionManager sessionManager;
    private final NamespacedKey key;

    public KeeperBookManager(Plugin plugin,
                             CharacterManager characterManager,
                             KeeperManager keeperManager,
                             SessionManager sessionManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.keeperManager = keeperManager;
        this.sessionManager = sessionManager;
        this.key = new NamespacedKey(plugin, "keeper_book");
    }

    public ItemStack createKeeperBook(Player keeper) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("KPブック");
        meta.setAuthor("TRPG System");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        List<Component> pages = new ArrayList<>();
        pages.add(createCommandPage());
        pages.addAll(createParticipantPages());
        meta.addPages(pages.toArray(Component[]::new));
        book.setItemMeta(meta);
        return book;
    }

    public void openKeeperBook(Player keeper) {
        keeper.openBook(createKeeperBook(keeper));
    }

    public boolean isKeeperBook(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) return false;
        if (!(item.getItemMeta() instanceof BookMeta meta)) return false;
        Byte value = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private Component createCommandPage() {
        Component page = title("KPメニュー");

        if (sessionManager.isActive()) {
            page = page.append(Component.text("セッション: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(sessionManager.getSessionName(), NamedTextColor.DARK_PURPLE)
                            .decorate(TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("参加: "
                            + sessionManager.getParticipantCount()
                            + "人", NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text("時間: "
                            + sessionManager.getTimePeriodName()
                            + " " + sessionManager.getScenarioTimeText(),
                            NamedTextColor.DARK_AQUA))
                    .append(Component.newline())
                    .append(Component.text("時計: "
                            + (sessionManager.isClockRunning() ? "進行中" : "停止中")
                            + " x" + sessionManager.getClockSpeed(),
                            NamedTextColor.GRAY))
                    .append(Component.newline()).append(Component.newline());
        } else {
            page = page.append(Component.text("セッション: 未開催", NamedTextColor.GRAY))
                    .append(Component.newline()).append(Component.newline());
        }

        page = page.append(button("[参加者を更新]", "/kpbook", "最新情報を再表示"))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("主なコマンド", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(command("/status", "探索者シート"))
                .append(command("/roll 1d100", "ダイス"))
                .append(command("/status revive <player>", "探索者を復活"))
                .append(command("/kp <player>", "KP権限を付与"))
                .append(command("/kpbook", "KPブック更新"))
                .append(command("/create session ", "セッション作成"))
                .append(command("/session list", "参加者一覧"))
                .append(command("/session time ", "セッション時刻"))
                .append(command("/session time start", "時計を開始"))
                .append(command("/session time pause", "時計を停止"))
                .append(command("/session end", "終了＋ログ保存"));
        return page;
    }

    private List<Component> createParticipantPages() {
        List<Component> pages = new ArrayList<>();
        List<Player> participants = new ArrayList<>();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (keeperManager.isKeeper(player)) {
                continue;
            }

            if (!sessionManager.isParticipant(player)) {
                continue;
            }

            participants.add(player);
        }

        if (participants.isEmpty()) {
            pages.add(title("参加探索者").append(
                    Component.text("現在オンラインの探索者はいません。", NamedTextColor.GRAY)
            ));
            return pages;
        }

        Component page = title("参加探索者");
        int count = 0;

        for (Player p : participants) {
            if (count >= 3) {
                pages.add(page);
                page = title("参加探索者");
                count = 0;
            }

            page = page
                    .append(Component.text(characterManager.getCharacterName(p), NamedTextColor.DARK_BLUE)
                            .decorate(TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("PL: " + p.getName(), NamedTextColor.DARK_GRAY))
                    .append(Component.newline())
                    .append(Component.text("HP " + characterManager.getCurrentHp(p) + "/" + characterManager.getHp(p), NamedTextColor.RED))
                    .append(Component.text("  MP " + characterManager.getCurrentMp(p) + "/" + characterManager.getMp(p), NamedTextColor.BLUE))
                    .append(Component.newline())
                    .append(Component.text("SAN " + characterManager.getCurrentSan(p) + "/" + characterManager.getSan(p), NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("  装甲 " + plugin.getArmorManager().getArmor(p), NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text(p.getGameMode().name(), NamedTextColor.GRAY))
                    .append(Component.newline()).append(Component.newline());

            count++;
        }

        pages.add(page);
        return pages;
    }

    private Component title(String text) {
        return Component.text(text, NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline()).append(Component.newline());
    }

    private Component button(String text, String command, String hover) {
        return Component.text(text, NamedTextColor.DARK_GREEN)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }

    private Component command(String command, String hover) {
        return Component.text(command, NamedTextColor.BLUE)
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)))
                .append(Component.newline());
    }
}
