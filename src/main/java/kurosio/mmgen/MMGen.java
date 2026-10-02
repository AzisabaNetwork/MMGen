package kurosio.mmgen;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MMGen extends JavaPlugin implements CommandExecutor, TabCompleter {

    private File itemsFolder;

    private static final Map<ItemFlag, String> FLAGS = new HashMap<>();

    static {
        FLAGS.put(ItemFlag.HIDE_ENCHANTS, "ENCHANTS");
        FLAGS.put(ItemFlag.HIDE_ATTRIBUTES, "ATTRIBUTES");
        FLAGS.put(ItemFlag.HIDE_UNBREAKABLE, "UNBREAKABLE");
        FLAGS.put(ItemFlag.HIDE_DESTROYS, "DESTROYS");
        FLAGS.put(ItemFlag.HIDE_PLACED_ON, "PLACED_ON");
        FLAGS.put(ItemFlag.HIDE_DYE, "DYE");
    }

    @Override
    public void onEnable() {
        itemsFolder = new File(getDataFolder(), "items");

        if (!itemsFolder.exists() && !itemsFolder.mkdirs()) {
            getLogger().severe("itemsフォルダを作成できませんでした。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getCommand("mmgen") != null) {
            getCommand("mmgen").setExecutor(this);
            getCommand("mmgen").setTabCompleter(this);
        }

        getLogger().info("MMGenが有効になりました！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cプレイヤーのみ実行できます。"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mmgen.use")) {
            player.sendMessage(color("&c権限がありません。"));
            return true;
        }

        if (args.length < 3
                || !args[0].equalsIgnoreCase("items")
                || !args[1].equalsIgnoreCase("create")) {
            usage(player);
            return true;
        }

        if (!args[2].toLowerCase(Locale.ROOT).startsWith("newid:")) {
            player.sendMessage(color(
                    "&cMMIDは newid:<newMMID> の形式で指定してください。"
            ));
            return true;
        }

        String id = args[2].substring(6);

        if (id.isEmpty() || !id.matches("[a-zA-Z0-9_-]+")) {
            player.sendMessage(color(
                    "&cMMIDには英数字、_、- のみ使用できます。"
            ));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(color(
                    "&c手にアイテムを持ってください。"
            ));
            return true;
        }

        boolean forceUnbreakable = false;
        Integer model = null;

        for (int i = 3; i < args.length; i++) {
            String arg = args[i];

            if (arg.toLowerCase(Locale.ROOT).startsWith("options:")) {

                String value = arg.substring(8);

                if (value.isEmpty()) {
                    player.sendMessage(color(
                            "&coptionsの値を指定してください。"
                    ));
                    return true;
                }

                for (String option : value.split(",")) {
                    if (option.equalsIgnoreCase("Unbreakable")) {
                        forceUnbreakable = true;
                    } else {
                        player.sendMessage(color(
                                "&c未対応のオプション: &f" + option
                        ));
                        return true;
                    }
                }

            } else if (arg.toLowerCase(Locale.ROOT).startsWith("acm:")) {

                try {
                    model = Integer.parseInt(arg.substring(4));

                    if (model < 0)
                        throw new NumberFormatException();

                } catch (NumberFormatException e) {
                    player.sendMessage(color(
                            "&cACMには0以上の整数を指定してください。"
                    ));
                    return true;
                }

            } else {
                player.sendMessage(color(
                        "&c不明な引数: &f" + arg
                ));
                usage(player);
                return true;
            }
        }

        ItemMeta meta = item.getItemMeta();

        if (model == null && meta != null && meta.hasCustomModelData())
            model = meta.getCustomModelData();

        File file = new File(itemsFolder, id + ".yml");

        if (file.exists()) {
            player.sendMessage(color(
                    "&c同じMMIDのファイルが既に存在します: &f" + id
            ));
            return true;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        String p = id;

        yaml.set(p + ".Id", item.getType().name());

        if (meta != null) {

            if (meta.hasDisplayName()) {
                yaml.set(
                        p + ".Display",
                        toMythicColor(meta.getDisplayName())
                );
            }

            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();

                for (String s : meta.getLore())
                    lore.add(toMythicColor(s));

                yaml.set(p + ".Lore", lore);
            }

            if (!meta.getEnchants().isEmpty()) {
                List<String> enchants = new ArrayList<>();

                for (Map.Entry<Enchantment, Integer> e :
                        meta.getEnchants().entrySet()) {

                    enchants.add(
                            e.getKey().getName() + ":" + e.getValue()
                    );
                }

                yaml.set(p + ".Enchantments", enchants);
            }

            if (!meta.getItemFlags().isEmpty()) {
                List<String> hide = new ArrayList<>();

                for (ItemFlag flag : meta.getItemFlags()) {
                    String name = FLAGS.get(flag);

                    if (name != null)
                        hide.add(name);
                }

                if (!hide.isEmpty())
                    yaml.set(p + ".Hide", hide);
            }

            if (forceUnbreakable || meta.isUnbreakable())
                yaml.set(p + ".Options.Unbreakable", true);
        }

        if (model != null)
            yaml.set(p + ".Model", model);

        try {
            yaml.save(file);

            player.sendMessage(color(
                    "&aMMアイテムを生成しました！"
            ));
            player.sendMessage(color(
                    "&7ID: &a" + id
            ));
            player.sendMessage(color(
                    "&7保存先: &fplugins/MMGen/items/" + id + ".yml"
            ));

        } catch (IOException e) {
            getLogger().severe(
                    "YAMLの保存に失敗しました: " + file.getName()
            );

            e.printStackTrace();

            player.sendMessage(color(
                    "&cファイルの生成に失敗しました。"
            ));
        }

        return true;
    }

    private void usage(Player player) {
        player.sendMessage(color(
                "&e使用方法: &f/mmgen items create <newMMID>" +
                        " [options:<option>] [acm:<acm>]"
        ));
    }

    /**
     * プラグイン内のメッセージ用
     * &c → §c
     */
    private String color(String text) {
        return text.replace('&', '§');
    }

    /**
     * MythicMobsのYAML用
     * §c → &c
     */
    private String toMythicColor(String text) {
        return text == null ? null : text.replace('§', '&');
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        if (!sender.hasPermission("mmgen.use"))
            return Collections.emptyList();

        String current =
                args[args.length - 1].toLowerCase(Locale.ROOT);

        List<String> list = new ArrayList<>();

        if (args.length == 1) {

            list.add("items");

        } else if (args.length == 2
                && args[0].equalsIgnoreCase("items")) {

            list.add("create");

        } else if (args.length == 3
                && args[0].equalsIgnoreCase("items")
                && args[1].equalsIgnoreCase("create")) {

            list.add("newid:");

        } else if (args.length >= 4
                && args[0].equalsIgnoreCase("items")
                && args[1].equalsIgnoreCase("create")) {

            boolean options = false;
            boolean acm = false;

            for (int i = 3; i < args.length - 1; i++) {
                String s = args[i].toLowerCase(Locale.ROOT);

                if (s.startsWith("options:"))
                    options = true;

                if (s.startsWith("acm:"))
                    acm = true;
            }

            if (current.startsWith("options:") && !options) {

                list.add("options:Unbreakable");

            } else if (current.startsWith("acm:") && !acm) {

                list.add("acm:");

            } else if (current.isEmpty()) {

                if (!options)
                    list.add("options:Unbreakable");

                if (!acm)
                    list.add("acm:");
            }
        }

        List<String> result = new ArrayList<>();

        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(current))
                result.add(s);
        }

        return result;
    }
}
