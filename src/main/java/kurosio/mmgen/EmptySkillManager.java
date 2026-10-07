package kurosio.mmgen;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EmptySkillManager {

    private final MMGen plugin;

    private final Map<UUID, PendingInsert> pending =
            new HashMap<>();

    public EmptySkillManager(MMGen plugin) {
        this.plugin = plugin;
    }

    public void request(Player player) {

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (item == null
                || item.getType().isAir()) {

            player.sendMessage(
                    plugin.color("&c手にアイテムを持ってください。")
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

        if (file == null || !file.exists()) {

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

        ItemMeta meta = item.getItemMeta();

        player.sendMessage(
                plugin.color(
                        "&6&m------------------------------"
                )
        );

        player.sendMessage(
                plugin.color(
                        "&a&l[MMGen] &e&l空スキル挿入"
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

        if (meta != null
                && meta.hasDisplayName()) {

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

                player.sendMessage(
                        "  " + line
                );
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

        /*
         * すでにスキルがある場合はここで終了。
         * YAML自体は変更しない。
         */
        if (hasActualSkills(yaml, mythicId)) {

            player.sendMessage(
                    plugin.color(
                            "&eこのアイテムには既にスキルが設定されています。"
                    )
            );

            return;
        }

        UUID token =
                UUID.randomUUID();

        pending.put(
                player.getUniqueId(),
                new PendingInsert(
                        token,
                        mythicId,
                        filePath,
                        item.clone()
                )
        );

        /*
         * クリック可能な確認メッセージ
         */
        player.spigot().sendMessage(
                new ComponentBuilder(
                        "このアイテムに空スキルを挿入してもよろしいですか？ "
                )
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("[クリックして挿入]")
                        .color(net.md_5.bungee.api.ChatColor.GREEN)
                        .bold(true)
                        .event(
                                new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/mmgen items insert-empty-skill confirm "
                                                + token
                                )
                        )
                        .event(
                                new HoverEvent(
                                        Action.SHOW_TEXT,
                                        new ComponentBuilder(
                                                "クリックしてSkillsにdelay 0を追加"
                                        ).create()
                                )
                        )
                        .create()
        );

        /*
         * 10秒後に確認を無効化
         */
        new BukkitRunnable() {

            @Override
            public void run() {

                PendingInsert current =
                        pending.get(
                                player.getUniqueId()
                        );

                if (current != null
                        && current.token.equals(token)) {

                    pending.remove(
                            player.getUniqueId()
                    );

                    if (player.isOnline()) {

                        player.sendMessage(
                                plugin.color(
                                        "&7空スキルの挿入確認がタイムアウトしました。"
                                )
                        );
                    }
                }
            }

        }.runTaskLater(
                plugin,
                20L * 10
        );
    }


    public void confirm(
            Player player,
            String tokenString) {

        PendingInsert data =
                pending.get(
                        player.getUniqueId()
                );

        if (data == null) {

            player.sendMessage(
                    plugin.color(
                            "&c確認が期限切れです。もう一度実行してください。"
                    )
            );

            return;
        }

        if (!data.token.toString()
                .equalsIgnoreCase(tokenString)) {

            player.sendMessage(
                    plugin.color(
                            "&c無効な確認です。"
                    )
            );

            return;
        }

        /*
         * 先に削除。
         * 同じ確認を2回クリックできないようにする。
         */
        pending.remove(
                player.getUniqueId()
        );

        File file =
                plugin.getMythicItemFile(
                        data.filePath
                );

        if (file == null || !file.exists()) {

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

        /*
         * もう一度Skillsを確認。
         *
         * 確認メッセージを出したあとに
         * 別の場所からスキルが追加されていた場合、
         * 絶対に上書きしない。
         */
        if (hasActualSkills(
                yaml,
                data.mythicId
        )) {

            player.sendMessage(
                    plugin.color(
                            "&eこのアイテムには既にスキルが設定されています。"
                    )
            );

            player.sendMessage(
                    plugin.color(
                            "&7ファイルは変更していません。"
                    )
            );

            return;
        }

        /*
         * Skills:
         * - delay 0
         */
        List<String> skills =
                new java.util.ArrayList<>();

        skills.add("delay 0");

        yaml.set(
                data.mythicId + ".Skills",
                skills
        );

        try {

            yaml.save(file);

        } catch (IOException e) {

            plugin.getLogger().severe(
                    "空スキルの挿入に失敗しました: "
                            + file.getName()
            );

            e.printStackTrace();

            player.sendMessage(
                    plugin.color(
                            "&c空スキルの挿入に失敗しました。"
                    )
            );

            return;
        }

        player.sendMessage(
                plugin.color(
                        "&a空スキルを挿入しました！"
                )
        );

        player.sendMessage(
                plugin.color(
                        "&7MMID: &f"
                                + data.mythicId
                )
        );

        player.sendMessage(
                plugin.color(
                        "&7追加内容: &fSkills: - delay 0"
                )
        );
    }

    private boolean hasActualSkills(
            YamlConfiguration yaml,
            String mythicId) {

        String path =
                mythicId + ".Skills";

        if (!yaml.contains(path)) {
            return false;
        }

        List<String> skills =
                yaml.getStringList(path);

        return skills != null
                && !skills.isEmpty();
    }

    private static class PendingInsert {

        private final UUID token;
        private final String mythicId;
        private final String filePath;
        private final ItemStack item;

        private PendingInsert(
                UUID token,
                String mythicId,
                String filePath,
                ItemStack item) {

            this.token = token;
            this.mythicId = mythicId;
            this.filePath = filePath;
            this.item = item;
        }
    }
}