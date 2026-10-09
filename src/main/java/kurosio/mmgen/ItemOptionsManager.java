package kurosio.mmgen;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemOptionsManager {

    private final MMGen plugin;

    private final Map<UUID, PendingInsert> pending =
            new HashMap<>();

    public ItemOptionsManager(MMGen plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, String option) {

        String normalizedOption = normalizeOption(option);

        if (normalizedOption == null) {
            player.sendMessage(
                    plugin.color(
                            "&c対応していないオプションです。"
                    )
            );
            player.sendMessage(
                    plugin.color(
                            "&7使用可能: Unbreakable, AppendType"
                    )
            );
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            player.sendMessage(
                    plugin.color(
                            "&c手にアイテムを持ってください。"
                    )
            );
            return;
        }

        String mythicId =
                plugin.getMythicItemId(item);

        if (mythicId == null) {
            player.sendMessage(
                    plugin.color(
                            "&cこのアイテムのMMIDを取得できませんでした。"
                    )
            );
            return;
        }

        String filePath =
                plugin.findMythicItemFile(mythicId);

        if (filePath == null) {
            player.sendMessage(
                    plugin.color(
                            "&cMMIDの定義ファイルを見つけられませんでした。"
                    )
            );
            return;
        }

        File file =
                plugin.getMythicItemFile(filePath);

        if (file == null || !file.isFile()) {
            player.sendMessage(
                    plugin.color(
                            "&c対象YAMLファイルを見つけられませんでした。"
                    )
            );
            return;
        }

        YamlConfiguration yaml =
                new YamlConfiguration();

        try {
            yaml.load(file);
        } catch (Exception e) {
            player.sendMessage(
                    plugin.color(
                            "&c対象YAMLを読み込めませんでした。"
                    )
            );
            return;
        }

        String optionPath =
                mythicId + ".Options." + normalizedOption;

        /*
         * true / false の値にかかわらず、
         * 対象キーが既に存在すれば変更しない。
         */
        if (yaml.contains(optionPath)) {
            player.sendMessage(
                    plugin.color(
                            "&cこのアイテムには既にOptions."
                                    + normalizedOption
                                    + "が記述されています。"
                    )
            );

            player.sendMessage(
                    plugin.color(
                            "&7既存の設定は変更していません。"
                    )
            );
            return;
        }

        ItemMeta meta = item.getItemMeta();

        player.sendMessage(
                plugin.color(
                        "&6&m------------------------------"
                )
        );

        player.sendMessage(
                plugin.color(
                        "&a&l[MMGen] &e&lOptions挿入"
                )
        );

        player.sendMessage(
                plugin.color(
                        "&7File: &f" + filePath
                )
        );

        player.sendMessage(
                plugin.color(
                        "&7MMID: &a" + mythicId
                )
        );

        player.sendMessage(
                plugin.color(
                        "&7Option: &f"
                                + normalizedOption
                                + ": true"
                )
        );

        if (meta != null && meta.hasDisplayName()) {
            player.sendMessage(
                    plugin.color(
                            "&7Display: &f"
                                    + meta.getDisplayName()
                    )
            );
        } else {
            player.sendMessage(
                    plugin.color(
                            "&7Display: &fなし"
                    )
            );
        }

        if (meta != null
                && meta.hasLore()
                && meta.getLore() != null
                && !meta.getLore().isEmpty()) {

            player.sendMessage(
                    plugin.color("&7Lore:")
            );

            for (String line : meta.getLore()) {
                player.sendMessage("  " + line);
            }

        } else {
            player.sendMessage(
                    plugin.color(
                            "&7Lore: &fなし"
                    )
            );
        }

        player.sendMessage(
                plugin.color(
                        "&6&m------------------------------"
                )
        );

        UUID token = UUID.randomUUID();

        pending.put(
                player.getUniqueId(),
                new PendingInsert(
                        token,
                        mythicId,
                        filePath,
                        normalizedOption
                )
        );

        player.spigot().sendMessage(
                new ComponentBuilder(
                        "Options."
                                + normalizedOption
                                + ": true を追加しますか？ "
                )
                        .color(
                                net.md_5.bungee.api.ChatColor.YELLOW
                        )
                        .append("[クリックして挿入]")
                        .color(
                                net.md_5.bungee.api.ChatColor.GREEN
                        )
                        .bold(true)
                        .event(
                                new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/mmgen items insert confirm "
                                                + token
                                )
                        )
                        .event(
                                new HoverEvent(
                                        Action.SHOW_TEXT,
                                        new ComponentBuilder(
                                                "クリックしてOptions."
                                                        + normalizedOption
                                                        + ": true を追加"
                                        ).create()
                                )
                        )
                        .create()
        );

        // 10秒後に確認を無効化
        new BukkitRunnable() {
            @Override
            public void run() {

                PendingInsert current =
                        pending.get(player.getUniqueId());

                if (current != null
                        && current.token.equals(token)) {

                    pending.remove(player.getUniqueId());

                    if (player.isOnline()) {
                        player.sendMessage(
                                plugin.color(
                                        "&7Optionsの挿入確認がタイムアウトしました。"
                                )
                        );
                    }
                }
            }
        }.runTaskLater(plugin, 20L * 10);
    }

    public void confirm(Player player, String tokenString) {

        PendingInsert data =
                pending.get(player.getUniqueId());

        if (data == null) {
            player.sendMessage(
                    plugin.color(
                            "&c確認が期限切れです。もう一度実行してください。"
                    )
            );
            return;
        }

        if (!data.token.toString().equalsIgnoreCase(tokenString)) {
            player.sendMessage(
                    plugin.color(
                            "&c無効な確認です。"
                    )
            );
            return;
        }

        // 同じ確認を二度実行できないようにする
        pending.remove(player.getUniqueId());

        File file =
                plugin.getMythicItemFile(data.filePath);

        if (file == null || !file.isFile()) {
            player.sendMessage(
                    plugin.color(
                            "&c対象YAMLファイルが見つかりません。"
                    )
            );
            return;
        }

        YamlConfiguration yaml =
                new YamlConfiguration();

        try {
            yaml.load(file);
        } catch (Exception e) {
            player.sendMessage(
                    plugin.color(
                            "&c対象YAMLを読み込めませんでした。"
                    )
            );
            return;
        }

        String optionPath =
                data.mythicId + ".Options." + data.option;

        if (yaml.contains(optionPath)) {
            player.sendMessage(
                    plugin.color(
                            "&cこのアイテムには既にOptions."
                                    + data.option
                                    + "が記述されています。"
                    )
            );

            player.sendMessage(
                    plugin.color(
                            "&7ファイルは変更していません。"
                    )
            );
            return;
        }

        yaml.set(optionPath, true);

        try {
            yaml.save(file);
        } catch (IOException e) {

            plugin.getLogger().severe(
                    "Optionsの挿入に失敗しました: "
                            + file.getName()
            );

            e.printStackTrace();

            player.sendMessage(
                    plugin.color(
                            "&cOptionsの挿入に失敗しました。"
                    )
            );
            return;
        }

        player.sendMessage(
                plugin.color(
                        "&aOptionsを挿入しました！"
                )
        );

        player.sendMessage(
                plugin.color(
                        "&7MMID: &f" + data.mythicId
                )
        );

        player.sendMessage(
                plugin.color(
                        "&7追加内容: &fOptions."
                                + data.option
                                + ": true"
                )
        );
    }

    private String normalizeOption(String option) {

        if (option == null) {
            return null;
        }

        if (option.equalsIgnoreCase("Unbreakable")) {
            return "Unbreakable";
        }

        if (option.equalsIgnoreCase("AppendType")) {
            return "AppendType";
        }

        return null;
    }

    private static class PendingInsert {

        private final UUID token;
        private final String mythicId;
        private final String filePath;
        private final String option;

        private PendingInsert(
                UUID token,
                String mythicId,
                String filePath,
                String option) {

            this.token = token;
            this.mythicId = mythicId;
            this.filePath = filePath;
            this.option = option;
        }
    }

}
