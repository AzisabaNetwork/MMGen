package kurosio.mmgen;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class MMGen extends JavaPlugin implements CommandExecutor, TabCompleter {

    private File itemsFolder;

    private static final Map<ItemFlag, String> FLAGS = new HashMap<>();

    static {
        FLAGS.put(ItemFlag.HIDE_ENCHANTS, "ENCHANTS");
        FLAGS.put(ItemFlag.HIDE_ATTRIBUTES, "ATTRIBUTES");
        FLAGS.put(ItemFlag.HIDE_UNBREAKABLE, "UNBREAKABLE");
        FLAGS.put(ItemFlag.HIDE_DESTROYS, "DESTROYS");
        FLAGS.put(ItemFlag.HIDE_PLACED_ON, "PLACED_ON");
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
        } else {
            getLogger().severe("plugin.ymlにmmgenコマンドが定義されていません。");
            getServer().getPluginManager().disablePlugin(this);
            return;
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

        if (args.length == 2
                && args[0].equalsIgnoreCase("items")
                && args[1].equalsIgnoreCase("info")) {

            showItemInfo(player);

            return true;
        }


        if (args.length == 2
                && args[0].equalsIgnoreCase("items")
                && args[1].equalsIgnoreCase("mmid")) {

            showMythicId(player);

            return true;
        }


        if (args.length < 2
                || !args[0].equalsIgnoreCase("items")
                || !args[1].equalsIgnoreCase("create")) {

            usage(player);

            return true;
        }

        // /mmgen items create newyml:<name>
        if (args.length == 3
                && args[0].equalsIgnoreCase("items")
                && args[1].equalsIgnoreCase("create")
                && args[2].toLowerCase(Locale.ROOT).startsWith("newyml:")) {

            String name = args[2].substring(7);

            if (name.isEmpty()) {
                player.sendMessage(color(
                        "&cnewymlの名前を指定してください。"
                ));
                return true;
            }

            if (!name.matches("[a-zA-Z0-9_-]+")) {
                player.sendMessage(color(
                        "&cファイル名には英数字、_、- のみ使用できます。"
                ));
                return true;
            }

            File file = new File(
                    itemsFolder,
                    name + ".yml"
            );

            if (file.exists()) {
                player.sendMessage(color(
                        "&c同じファイルが既に存在します: &f"
                                + name + ".yml"
                ));
                return true;
            }

            YamlConfiguration yaml = new YamlConfiguration();

            try {
                yaml.save(file);

                player.sendMessage(color(
                        "&aYAMLファイルを作成しました！"
                ));

                player.sendMessage(color(
                        "&7保存先: &fplugins/MMGen/items/"
                                + name + ".yml"
                ));

            } catch (IOException e) {

                getLogger().severe(
                        "YAMLファイルの作成に失敗しました: "
                                + file.getName()
                );

                e.printStackTrace();

                player.sendMessage(color(
                        "&cYAMLファイルの作成に失敗しました。"
                ));
            }

            return true;
        }

        String id = null;
        String filename = null;


        for (int i = 2; i < args.length; i++) {

            String arg = args[i];
            String lowerArg = arg.toLowerCase(Locale.ROOT);

            if (lowerArg.startsWith("newid:")) {

                if (id != null) {
                    player.sendMessage(color(
                            "&cnewidは1回だけ指定してください。"
                    ));
                    return true;
                }

                id = arg.substring(6);

                if (id.isEmpty()
                        || !id.matches("[a-zA-Z0-9_-]+")) {

                    player.sendMessage(color(
                            "&cMMIDには英数字、_、- のみ使用できます。"
                    ));
                    return true;
                }

            } else if (lowerArg.startsWith("filename:")) {

                if (filename != null) {
                    player.sendMessage(color(
                            "&cfilenameは1回だけ指定してください。"
                    ));
                    return true;
                }

                filename = arg.substring(9);

                if (filename.isEmpty()) {
                    player.sendMessage(color(
                            "&cfilenameの値を指定してください。"
                    ));
                    return true;
                }

                if (!filename.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    player.sendMessage(color(
                            "&cfilenameには.ymlファイルを指定してください。"
                    ));
                    return true;
                }

                // パスを指定して別フォルダへアクセスできないようにする
                if (filename.contains("/")
                        || filename.contains("\\")
                        || filename.contains("..")) {

                    player.sendMessage(color(
                            "&cfilenameにはファイル名のみ指定してください。"
                    ));
                    return true;
                }

            } else if (lowerArg.startsWith("options:")) {

                String value = arg.substring(8);

                if (value.isEmpty()) {
                    player.sendMessage(color(
                            "&coptionsの値を指定してください。"
                    ));
                    return true;
                }

                for (String option : value.split(",")) {

                    if (option.equalsIgnoreCase("Unbreakable")) {
                        // 後でOptions.Unbreakable=trueにする
                    } else {
                        player.sendMessage(color(
                                "&c未対応のオプション: &f" + option
                        ));
                        return true;
                    }
                }

            } else if (lowerArg.startsWith("acm:")) {

                try {
                    int value = Integer.parseInt(arg.substring(4));

                    if (value < 0) {
                        throw new NumberFormatException();
                    }

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

        // newidは必須
        if (id == null) {
            player.sendMessage(color(
                    "&cnewid:<newID> を指定してください。"
            ));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(color(
                    "&c手にアイテムを持ってください。"
            ));
            return true;
        }

        boolean forceUnbreakable = false;
        Integer model = null;


        for (int i = 2; i < args.length; i++) {

            String arg = args[i];
            String lowerArg = arg.toLowerCase(Locale.ROOT);

            if (lowerArg.startsWith("options:")) {

                String value = arg.substring(8);

                for (String option : value.split(",")) {

                    if (option.equalsIgnoreCase("Unbreakable")) {
                        forceUnbreakable = true;
                    }
                }

            } else if (lowerArg.startsWith("acm:")) {

                try {
                    model = Integer.parseInt(arg.substring(4));

                    if (model < 0) {
                        throw new NumberFormatException();
                    }

                } catch (NumberFormatException e) {

                    player.sendMessage(color(
                            "&cACMには0以上の整数を指定してください。"
                    ));
                    return true;
                }
            }
        }

        ItemMeta meta = item.getItemMeta();

        // acmが指定されていない場合はアイテムから取得
        if (model == null
                && meta != null
                && meta.hasCustomModelData()) {

            model = meta.getCustomModelData();
        }


        File file;

        if (filename != null) {

            file = new File(itemsFolder, filename);

            if (!file.exists()) {
                player.sendMessage(color(
                        "&c指定されたファイルが存在しません: &f"
                                + filename
                ));
                return true;
            }

        } else {

            file = new File(itemsFolder, id + ".yml");

            if (file.exists()) {
                player.sendMessage(color(
                        "&c同じMMIDのファイルが既に存在します: &f"
                                + id
                ));
                return true;
            }
        }


        YamlConfiguration yaml;

        if (filename != null) {
            yaml = YamlConfiguration.loadConfiguration(file);
        } else {
            yaml = new YamlConfiguration();
        }

        String path = id;


        if (yaml.contains(path)) {
            player.sendMessage(color(
                    "&c指定ファイルに同じMMIDが既に存在します: &f"
                            + id
            ));
            return true;
        }


        yaml.set(path + ".Id", item.getType().name());

        if (meta != null) {

            if (meta.hasDisplayName()) {
                yaml.set(
                        path + ".Display",
                        toMythicColor(meta.getDisplayName())
                );
            }

            if (meta.hasLore()) {

                List<String> lore = meta.getLore();

                if (lore != null && !lore.isEmpty()) {

                    List<String> convertedLore = new ArrayList<>();

                    for (String line : lore) {
                        convertedLore.add(
                                toMythicColor(line)
                        );
                    }

                    yaml.set(
                            path + ".Lore",
                            convertedLore
                    );
                }
            }

            if (!meta.getEnchants().isEmpty()) {

                List<String> enchants = new ArrayList<>();

                for (Map.Entry<Enchantment, Integer> entry
                        : meta.getEnchants().entrySet()) {

                    enchants.add(
                            entry.getKey().getName()
                                    + ":"
                                    + entry.getValue()
                    );
                }

                yaml.set(
                        path + ".Enchantments",
                        enchants
                );
            }

            if (!meta.getItemFlags().isEmpty()) {

                List<String> hide = new ArrayList<>();

                for (ItemFlag flag : meta.getItemFlags()) {

                    String name = FLAGS.get(flag);

                    if (name != null) {
                        hide.add(name);
                    }
                }

                if (!hide.isEmpty()) {
                    yaml.set(
                            path + ".Hide",
                            hide
                    );
                }
            }

            if (forceUnbreakable || meta.isUnbreakable()) {
                yaml.set(
                        path + ".Options.Unbreakable",
                        true
                );
            }
        }

        if (model != null) {
            yaml.set(
                    path + ".Model",
                    model
            );
        }


        try {

            yaml.save(file);

            player.sendMessage(color(
                    "&aMMアイテムを生成しました！"
            ));

            player.sendMessage(color(
                    "&7ID: &a" + id
            ));

            if (filename != null) {

                player.sendMessage(color(
                        "&7追加先: &fplugins/MMGen/items/"
                                + filename
                ));

            } else {

                player.sendMessage(color(
                        "&7保存先: &fplugins/MMGen/items/"
                                + id + ".yml"
                ));
            }

        } catch (IOException e) {

            getLogger().severe(
                    "YAMLの保存に失敗しました: "
                            + file.getName()
            );

            e.printStackTrace();

            player.sendMessage(color(
                    "&cファイルの生成に失敗しました。"
            ));
        }

        return true;
    }


     //mmgen items info
    private void showItemInfo(Player player) {

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (item == null
                || item.getType() == Material.AIR) {

            player.sendMessage(color(
                    "&c手にアイテムを持ってください。"
            ));

            return;
        }

        ItemMeta meta = item.getItemMeta();

        player.sendMessage(color(
                "&6&m------------------------------"
        ));

        player.sendMessage(color(
                "&a&l[MMGen] &e&lアイテム情報"
        ));

        // MCID
        player.sendMessage(color(
                "&7MCID: &f" + item.getType().name()
        ));

        // MMID
        String mythicId = getMythicItemId(item);

        if (mythicId != null) {

            player.sendMessage(color(
                    "&7MMID: &a" + mythicId
            ));

            String mythicFile = findMythicItemFile(mythicId);

            if (mythicFile != null) {

                player.sendMessage(color(
                        "&7File: &f" + mythicFile
                ));

            } else {

                player.sendMessage(color(
                        "&7File: &c見つかりませんでした"
                ));
            }

        } else {

            player.sendMessage(color(
                    "&7MMID: &c取得できませんでした"
            ));
        }

        // Display
        if (meta != null
                && meta.hasDisplayName()) {

            player.sendMessage(color(
                    "&7Display: &f"
                            + meta.getDisplayName()
            ));

        } else {

            player.sendMessage(color(
                    "&7Display: &fなし"
            ));
        }

        // CustomModelData
        if (meta != null
                && meta.hasCustomModelData()) {

            player.sendMessage(color(
                    "&7CustomModelData: &f"
                            + meta.getCustomModelData()
            ));

        } else {

            player.sendMessage(color(
                    "&7CustomModelData: &fなし"
            ));
        }

        // Lore
        if (meta != null
                && meta.hasLore()
                && meta.getLore() != null
                && !meta.getLore().isEmpty()) {

            player.sendMessage(color(
                    "&7Lore:"
            ));

            for (String line : meta.getLore()) {

                player.sendMessage(
                        "  " + line
                );
            }

        } else {

            player.sendMessage(color(
                    "&7Lore: &fなし"
            ));
        }

        player.sendMessage(color(
                "&6&m------------------------------"
        ));
    }

    private void showMythicId(Player player) {

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (item == null
                || item.getType() == Material.AIR) {

            player.sendMessage(color(
                    "&c手にアイテムを持ってください。"
            ));

            return;
        }

        String mythicId = getMythicItemId(item);

        if (mythicId == null) {

            player.sendMessage(color(
                    "&cこのアイテムのMMIDを取得できませんでした。"
            ));

            return;
        }

        player.sendMessage(color(
                "&aMMID: &f" + mythicId
        ));
    }


    private String getMythicItemId(ItemStack item) {

        if (item == null
                || item.getType() == Material.AIR) {

            return null;
        }

        try {

            // MythicMobs本体
            Class<?> mythicMobsClass =
                    Class.forName(
                            "io.lumine.xikage.mythicmobs.MythicMobs"
                    );

            // MythicMobs.inst()
            Method instMethod =
                    mythicMobsClass.getMethod("inst");

            Object mythicMobs =
                    instMethod.invoke(null);

            // getItemManager()
            Method getItemManagerMethod =
                    mythicMobsClass.getMethod(
                            "getItemManager"
                    );

            Object itemManager =
                    getItemManagerMethod.invoke(mythicMobs);

            // getItems()
            Method getItemsMethod =
                    itemManager.getClass().getMethod(
                            "getItems"
                    );

            Object result =
                    getItemsMethod.invoke(itemManager);

            if (!(result instanceof Iterable)) {
                return null;
            }

            // MythicItem
            Class<?> mythicItemClass =
                    Class.forName(
                            "io.lumine.xikage.mythicmobs.items.MythicItem"
                    );

            Method getInternalNameMethod =
                    mythicItemClass.getMethod(
                            "getInternalName"
                    );

            Method generateItemStackMethod =
                    mythicItemClass.getMethod(
                            "generateItemStack",
                            int.class
                    );

            // BukkitAdapter
            Class<?> bukkitAdapterClass =
                    Class.forName(
                            "io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter"
                    );

            Method adaptMethod =
                    bukkitAdapterClass.getMethod(
                            "adapt",
                            Class.forName(
                                    "io.lumine.xikage.mythicmobs.adapters.AbstractItemStack"
                            )
                    );

            for (Object mythicItem : (Iterable<?>) result) {

                // MMID
                String internalName =
                        (String) getInternalNameMethod.invoke(
                                mythicItem
                        );

                // MythicItemからItemStackを生成
                Object abstractItemStack =
                        generateItemStackMethod.invoke(
                                mythicItem,
                                1
                        );

                if (abstractItemStack == null) {
                    continue;
                }

                // Bukkit ItemStackへ変換
                ItemStack generatedItem =
                        (ItemStack) adaptMethod.invoke(
                                null,
                                abstractItemStack
                        );

                if (generatedItem == null) {
                    continue;
                }

                // アイテム本体を比較
                if (generatedItem.isSimilar(item)) {
                    return internalName;
                }
            }

        } catch (Exception e) {

            getLogger().warning(
                    "MythicMobsのMMID取得中にエラーが発生しました: "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
            );
        }

        return null;
    }

    private String findMythicItemFile(String mythicId) {

        org.bukkit.plugin.Plugin mythicMobs =
                getServer().getPluginManager().getPlugin("MythicMobs");

        if (mythicMobs == null) {

            getLogger().warning(
                    "MythicMobsが見つかりません。"
            );

            return null;
        }

        File mythicItemsFolder =
                new File(
                        mythicMobs.getDataFolder(),
                        "Items"
                );

        getLogger().info(
                "MythicMobs Itemsフォルダ: "
                        + mythicItemsFolder.getAbsolutePath()
        );

        if (!mythicItemsFolder.exists()
                || !mythicItemsFolder.isDirectory()) {

            getLogger().warning(
                    "Itemsフォルダが存在しません: "
                            + mythicItemsFolder.getAbsolutePath()
            );

            return null;
        }

        List<File> files = new ArrayList<>();

        collectYamlFiles(
                mythicItemsFolder,
                files
        );

        getLogger().info(
                "検索対象YAML数: "
                        + files.size()
        );

        for (File file : files) {

            YamlConfiguration yaml =
                    YamlConfiguration.loadConfiguration(file);

            for (String key : yaml.getKeys(false)) {

                if (key.equalsIgnoreCase(mythicId)) {

                    return getRelativePath(
                            mythicItemsFolder,
                            file
                    );
                }
            }
        }

        getLogger().warning(
                "MMIDの定義ファイルが見つかりません: "
                        + mythicId
        );

        return null;
    }

    private void collectYamlFiles(
            File folder,
            List<File> files) {

        File[] children = folder.listFiles();

        if (children == null) {
            return;
        }

        for (File child : children) {

            if (child.isDirectory()) {

                collectYamlFiles(
                        child,
                        files
                );

            } else if (child.isFile()
                    && child.getName().toLowerCase(Locale.ROOT)
                    .endsWith(".yml")) {

                files.add(child);
            }
        }
    }

    private String getRelativePath(
            File baseFolder,
            File file) {

        String basePath =
                baseFolder.getAbsolutePath();

        String filePath =
                file.getAbsolutePath();

        if (filePath.startsWith(basePath)) {

            String relative =
                    filePath.substring(
                            basePath.length()
                    );

            if (relative.startsWith(File.separator)) {
                relative = relative.substring(1);
            }

            return "Items/" + relative.replace(
                    File.separatorChar,
                    '/'
            );
        }

        return file.getName();
    }

    private void usage(Player player) {
        player.sendMessage(color(
                "&e使用方法:"
        ));

        player.sendMessage(color(
                "&f/mmgen items create newyml:<name>"
        ));

        player.sendMessage(color(
                "&f/mmgen items create newid:<newMMID>"
                        + " [filename:<filename.yml>]"
                        + " [options:<option>]"
                        + " [acm:<acm>]"
        ));

        player.sendMessage(color(
                "&f/mmgen items [info/mmid]"
        ));
    }


    private String color(String text) {
        return text.replace('&', '§');
    }


    private String toMythicColor(String text) {
        return text == null
                ? null
                : text.replace('§', '&');
    }

    private List<String> getItemYamlFiles() {

        List<String> files = new ArrayList<>();

        if (itemsFolder == null || !itemsFolder.exists()) {
            return files;
        }

        File[] listFiles = itemsFolder.listFiles();

        if (listFiles == null) {
            return files;
        }

        for (File file : listFiles) {

            if (file.isFile()
                    && file.getName()
                    .toLowerCase(Locale.ROOT)
                    .endsWith(".yml")) {

                files.add(
                        "filename:" + file.getName()
                );
            }
        }

        Collections.sort(files);

        return files;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        if (!sender.hasPermission("mmgen.use")) {
            return Collections.emptyList();
        }

        String current =
                args[args.length - 1]
                        .toLowerCase(Locale.ROOT);

        List<String> list = new ArrayList<>();

        if (args.length == 1) {

            list.add("items");

        } else if (args.length == 2
                && args[0].equalsIgnoreCase("items")) {

            list.add("create");
            list.add("info");
            list.add("mmid");

        } else if (args.length >= 3
                && args[0].equalsIgnoreCase("items")
                && args[1].equalsIgnoreCase("create")) {

            boolean filename = false;
            boolean newid = false;
            boolean newyml = false;
            boolean options = false;
            boolean acm = false;

            for (int i = 2; i < args.length - 1; i++) {
                String s =
                        args[i].toLowerCase(Locale.ROOT);
                if (s.startsWith("filename:")) {
                    filename = true;
                }
                if (s.startsWith("newyml:")) {
                    newyml = true;
                }
                if (s.startsWith("newid:")) {
                    newid = true;
                }
                if (s.startsWith("options:")) {
                    options = true;
                }
                if (s.startsWith("acm:")) {
                    acm = true;
                }
            }

            if (current.startsWith("filename:")
                    && !filename) {
                list.addAll(
                        getItemYamlFiles()
                );

            } else if (current.startsWith("newyml:")
                    && !newyml) {
                list.add("newyml:");

            } else if (current.startsWith("newid:")
                    && !newid) {
                list.add("newid:");

            } else if (current.startsWith("options:")
                    && !options) {
                list.add(
                        "options:Unbreakable"
                );
            } else if (current.startsWith("acm:")
                    && !acm) {
                list.add("acm:");
            } else if (current.isEmpty()) {

                if (!newyml) {
                    list.add("newyml:");
                }
                if (!filename) {
                    list.add("filename:");
                }

                if (!newid) {
                    list.add("newid:");
                }

                if (!options) {
                    list.add(
                            "options:Unbreakable"
                    );
                }

                if (!acm) {
                    list.add("acm:");
                }
            }
        }

        List<String> result = new ArrayList<>();

        for (String suggestion : list) {

            if (suggestion
                    .toLowerCase(Locale.ROOT)
                    .startsWith(current)) {

                result.add(suggestion);
            }
        }

        return result;
    }
}