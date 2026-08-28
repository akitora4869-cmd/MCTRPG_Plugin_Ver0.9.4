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
import java.util.LinkedHashMap;
import java.util.List;

public class BookManager {

    private final Plugin plugin;
    private final CharacterManager characterManager;
    private final SkillManager skillManager;
    private final NamespacedKey sheetKey;

    public BookManager(Plugin plugin,
                       CharacterManager characterManager,
                       SkillManager skillManager) {
        this.plugin = plugin;
        this.characterManager = characterManager;
        this.skillManager = skillManager;
        this.sheetKey = new NamespacedKey(plugin, "character_sheet");
    }

    public void openSheet(Player player) {
        player.openBook(createSheet(player));
    }

    public void openRandomConfirm(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("能力値一括生成");
        meta.setAuthor(plugin.getConfig().getString("book.author", "TRPG System"));

        Component page = Component.text("能力値一括生成", NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(
                        "現在の能力値を上書きして、CoC第6版標準式で新しく生成します。\n\n",
                        NamedTextColor.BLACK
                ))
                .append(Component.text(
                        "STR/CON/POW/DEX/APP: 3d6\nSIZ/INT: 2d6+6\nEDU: 3d6+3\n\n",
                        NamedTextColor.DARK_GRAY
                ))
                .append(button(
                        "[実行]",
                        NamedTextColor.DARK_GREEN,
                        "/status random",
                        "能力値を一括生成します"
                ))
                .append(Component.space())
                .append(button(
                        "[キャンセル]",
                        NamedTextColor.DARK_RED,
                        "/status",
                        "探索者シートへ戻ります"
                ));

        meta.addPages(page);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    public ItemStack createSheet(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle(plugin.getConfig().getString("book.title", "探索者シート"));
        meta.setAuthor(plugin.getConfig().getString("book.author", "TRPG System"));
        meta.getPersistentDataContainer().set(sheetKey, PersistentDataType.BYTE, (byte) 1);

        List<Component> pages = new ArrayList<>();
        pages.addAll(createStatsPages(player));
        pages.add(createDerivedPage(player));
        pages.add(createDerivedChecksPage(player));
        pages.add(createCompositePage());
        pages.add(createSkillPointPage(player));

        LinkedHashMap<String, List<SkillDefinition>> grouped = skillManager.groupByCategory();

        for (var entry : grouped.entrySet()) {
            List<SkillDefinition> skills = entry.getValue();

            for (int start = 0; start < skills.size(); start += 4) {
                int end = Math.min(start + 4, skills.size());
                pages.add(createSkillsPage(player, entry.getKey(), skills.subList(start, end)));
            }
        }

        meta.addPages(pages.toArray(Component[]::new));
        book.setItemMeta(meta);
        return book;
    }

    public boolean isCharacterSheet(ItemStack item) {
        if (item == null || item.getType() != Material.WRITTEN_BOOK) {
            return false;
        }

        if (!(item.getItemMeta() instanceof BookMeta meta)) {
            return false;
        }

        Byte value = meta.getPersistentDataContainer().get(sheetKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private List<Component> createStatsPages(Player player) {
        List<Component> pages = new ArrayList<>();

        // 1ページ目: 名前・能力値生成・STR～DEX
        Component first = title("基本能力値 1/2");

        first = first.append(
                Component.text("名前: " + characterManager.getCharacterName(player) + " ", NamedTextColor.BLACK)
        ).append(button(
                "[変更]",
                NamedTextColor.BLUE,
                "/trpgedit name character",
                "キャラクター名を変更"
        ));

        first = first.append(Component.newline())
                .append(button(
                        "[能力値一括生成]",
                        NamedTextColor.DARK_PURPLE,
                        "/status random-confirm",
                        "能力値一括生成の確認画面を開く"
                ))
                .append(Component.newline())
                .append(Component.newline());

        String[] firstStats = {"STR", "CON", "POW", "DEX"};
        for (String stat : firstStats) {
            first = appendStatLine(first, player, stat);
        }

        pages.add(first);

        // 2ページ目: APP～EDU + 素手攻撃設定
        Component second = title("基本能力値 2/2");

        String[] secondStats = {"APP", "SIZ", "INT", "EDU"};
        for (String stat : secondStats) {
            second = appendStatLine(second, player, stat);
        }

        second = second.append(Component.newline())
                .append(Component.text("―― 戦闘設定 ――", NamedTextColor.DARK_PURPLE))
                .append(Component.newline())
                .append(Component.text("素手攻撃: ", NamedTextColor.BLACK));

        String selectedUnarmed = characterManager.getUnarmedAttack(player);

        Component fistButton = button(
                "fist".equals(selectedUnarmed) ? "[✓こぶし]" : "[こぶし]",
                "fist".equals(selectedUnarmed) ? NamedTextColor.DARK_GREEN : NamedTextColor.BLUE,
                "/trpgattack fist",
                "素手攻撃を「こぶし」に設定（1d3+DB）"
        );

        Component kickButton = button(
                "kick".equals(selectedUnarmed) ? "[✓キック]" : "[キック]",
                "kick".equals(selectedUnarmed) ? NamedTextColor.DARK_GREEN : NamedTextColor.BLUE,
                "/trpgattack kick",
                "素手攻撃を「キック」に設定（1d6+DB）"
        );

        second = second.append(fistButton)
                .append(Component.space())
                .append(kickButton)
                .append(Component.newline())
                .append(Component.text(
                        "こぶし1d3+DB / キック1d6+DB",
                        NamedTextColor.DARK_GRAY
                ))
                .append(Component.newline())
                .append(Component.text(
                        "※両方マーシャルアーツ対象",
                        NamedTextColor.DARK_GRAY
                ));

        pages.add(second);

        return pages;
    }


    private Component appendStatLine(Component page, Player player, String stat) {
        int value = characterManager.getStat(player, stat);

        return page.append(
                        Component.text(stat + " " + value + " ", NamedTextColor.BLACK)
                )
                .append(button(
                        "[変更]",
                        NamedTextColor.BLUE,
                        "/trpgedit stat " + stat,
                        stat + "の値を変更"
                ))
                .append(Component.space())
                .append(button(
                        "[判定]",
                        NamedTextColor.DARK_GREEN,
                        "/trpgroll stat " + stat,
                        stat + "×5で1d100判定"
                ))
                .append(Component.newline());
    }



    private Component createDerivedPage(Player player) {
        Component page = title("HP・MP・SAN");

        page = page.append(
                Component.text("HP " + characterManager.getCurrentHp(player)
                        + " / " + characterManager.getHp(player) + " ", NamedTextColor.BLACK)
        );
        page = page.append(button(
                "[変更]",
                NamedTextColor.BLUE,
                "/trpgedit hp current",
                "現在HPの値を変更"
        ));
        page = page.append(Component.space());
        page = page.append(button(
                "[-]",
                NamedTextColor.DARK_RED,
                "/trpgedit hp damage",
                "HPダメージ量を入力"
        ));
        page = page.append(Component.space());
        page = page.append(button(
                "[+]",
                NamedTextColor.DARK_GREEN,
                "/trpgedit hp heal",
                "HP回復量を入力"
        ));
        page = page.append(Component.newline()).append(Component.newline());

        page = page.append(
                Component.text("MP " + characterManager.getCurrentMp(player)
                        + " / " + characterManager.getMp(player) + " ", NamedTextColor.BLACK)
        );
        page = page.append(button(
                "[変更]",
                NamedTextColor.BLUE,
                "/trpgedit mp current",
                "現在MPの値を変更"
        ));
        page = page.append(Component.space());
        page = page.append(button(
                "[-]",
                NamedTextColor.DARK_RED,
                "/trpgedit mp spend",
                "MP消費量を入力"
        ));
        page = page.append(Component.space());
        page = page.append(button(
                "[+]",
                NamedTextColor.DARK_GREEN,
                "/trpgedit mp recover",
                "MP回復量を入力"
        ));
        page = page.append(Component.newline()).append(Component.newline());

        page = page.append(
                Component.text("SAN " + characterManager.getCurrentSan(player)
                        + " / " + characterManager.getSan(player) + " ", NamedTextColor.BLACK)
        );
        page = page.append(button(
                "[変更]",
                NamedTextColor.BLUE,
                "/trpgedit san current",
                "現在SANの値を変更"
        ));
        page = page.append(Component.newline());

        page = page.append(button(
                "[SAN判定]",
                NamedTextColor.DARK_GREEN,
                "/trpgroll san current",
                "現在SANで1d100判定"
        ));
        page = page.append(Component.space());
        page = page.append(button(
                "[SAN減少]",
                NamedTextColor.DARK_RED,
                "/trpgedit sanloss apply",
                "SAN減少量を入力"
        ));

        page = page.append(Component.newline()).append(Component.newline())
                .append(Component.text(
                        "最大値: HP=(CON+SIZ)/2切上\nMP=POW\nSAN=POW×5",
                        NamedTextColor.DARK_GRAY
                ));

        return page;
    }

    private Component createDerivedChecksPage(Player player) {
        Component page = title("派生判定");

        page = page.append(derivedCheckLine(player, "アイデア", "idea"));
        page = page.append(derivedCheckLine(player, "幸運", "luck"));
        page = page.append(derivedCheckLine(player, "知識", "knowledge"));

        page = page.append(Component.newline())
                .append(Component.text(
                        "アイデア = INT×5\n幸運 = POW×5\n知識 = EDU×5",
                        NamedTextColor.DARK_GRAY
                ));

        return page;
    }


    private Component createCompositePage() {
        Component page = title("複合技能");

        page = page.append(Component.text("医学＋応急手当 ", NamedTextColor.BLACK))
                .append(button("[判定]", NamedTextColor.DARK_GREEN,
                        "/trpgcombo medicine_firstaid", "段階判定を行います"))
                .append(Component.newline()).append(Component.newline());

        page = page.append(Component.text("目星＋聞き耳 ", NamedTextColor.BLACK))
                .append(button("[判定]", NamedTextColor.DARK_GREEN,
                        "/trpgcombo spot_listen", "段階判定を行います"))
                .append(Component.newline()).append(Component.newline());

        page = page.append(Component.text("隠れる＋忍び歩き ", NamedTextColor.BLACK))
                .append(button("[判定]", NamedTextColor.DARK_GREEN,
                        "/trpgcombo hide_sneak", "段階判定を行います"))
                .append(Component.newline()).append(Component.newline());

        page = page.append(Component.text("登攀＋跳躍 ", NamedTextColor.BLACK))
                .append(button("[判定]", NamedTextColor.DARK_GREEN,
                        "/trpgcombo climb_jump", "段階判定を行います"));

        return page;
    }

    private Component createSkillPointPage(Player player) {
        int total = characterManager.getHobbyPointTotal(player);
        int used = characterManager.getHobbyPointUsed(player);
        int remaining = characterManager.getHobbyPointRemaining(player);

        Component page = title("技能ポイント");

        page = page.append(Component.text(
                "趣味ポイント\n",
                NamedTextColor.DARK_PURPLE
        ).decorate(TextDecoration.BOLD));

        page = page.append(Component.text(
                "合計: " + total + "\n"
                        + "使用: " + used + "\n"
                        + "残り: " + remaining + "\n",
                NamedTextColor.BLACK
        ));

        page = page.append(Component.text(
                "計算: INT×10\n\n",
                NamedTextColor.DARK_GRAY
        ));

        page = page.append(Component.text(
                "技能ページの [趣味+] から\n"
                        + "ポイントを割り振れます。\n\n",
                NamedTextColor.BLACK
        ));

        page = page.append(button(
                "[趣味ポイントをリセット]",
                NamedTextColor.DARK_RED,
                "/trpgskill hobby-reset",
                "割り振った趣味ポイントをすべて戻す"
        ));

        page = page.append(Component.newline())
                .append(Component.newline())
                .append(Component.text(
                        "職業ポイント: 未実装\n"
                                + "※職業実装時に別枠で追加予定",
                        NamedTextColor.DARK_GRAY
                ));

        return page;
    }

    private Component createSkillsPage(Player player,
                                       String category,
                                       List<SkillDefinition> skills) {
        Component page = title(category);

        for (SkillDefinition skill : skills) {
            int value = skillManager.getSkillValue(player, skill.getId());
            int hobby = characterManager.getHobbyAllocation(player, skill.getId());

            // 技能名が長い場合でもボタンが本の右端へはみ出さないよう、
            // 技能情報と操作ボタンを必ず別行にする。
            page = page.append(
                    Component.text(
                            skill.getName() + " " + value
                                    + (hobby > 0 ? " (趣+" + hobby + ")" : ""),
                            NamedTextColor.BLACK
                    )
            ).append(Component.newline());

            page = page.append(button(
                    "[趣味+]",
                    NamedTextColor.BLUE,
                    "/trpgskill hobby " + skill.getId(),
                    skill.getName() + "へ趣味ポイントを割り振る"
            ));

            page = page.append(Component.space());

            page = page.append(button(
                    "[判定]",
                    NamedTextColor.DARK_GREEN,
                    "/trpgroll skill " + skill.getId(),
                    skill.getName() + "で1d100判定"
            ));

            page = page.append(Component.newline());
        }

        return page;
    }

    private Component derivedCheckLine(Player player, String label, String id) {
        int value = characterManager.getDerived(player, id);

        return Component.text(label + " " + value + " ", NamedTextColor.BLACK)
                .append(button(
                        "[判定]",
                        NamedTextColor.DARK_GREEN,
                        "/trpgroll derived " + id,
                        label + "で1d100判定"
                ))
                .append(Component.newline());
    }

    private Component title(String text) {
        return Component.text(text, NamedTextColor.DARK_BLUE)
                .decorate(TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.newline());
    }

    private Component line(String label, int value) {
        return Component.text(label + " : " + value, NamedTextColor.BLACK)
                .append(Component.newline());
    }

    private Component button(String text,
                             NamedTextColor color,
                             String command,
                             String hover) {
        return Component.text(text, color)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(
                        Component.text(hover, NamedTextColor.GRAY)
                ));
    }
}
