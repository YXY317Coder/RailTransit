package me.yxy317coder.slimefunaddon;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import java.util.UUID;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.unirest.json.JSONObject;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.yxy317coder.slimefunaddon.command.RTCommand;

public class ExampleAddon extends JavaPlugin implements SlimefunAddon {
    private ChatPrompter chatPrompter;
    private final CartRunning cartRunning = new CartRunning();
    private CartAI cartAI;
    private Controller controller;

    String[] icon_base64 = {
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzZjMmI5NjBlZmZmNjliZmM3MTYxYzRjYzA4MzY0MmM0ZGQ1ODM3ZmJjMWFiMzZkODhmN2FkZjIwOGRiZjJhIn19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWJmMjk2NmMyYTJhYjg1NzE1MTM1NTc2Nzg1YWVkZjk2MDJlMzc5YjNjYjBjOTQzODc3ZjEzZThhNmNkZTE2ZSJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTY0MDcwNzQwYTQ0ZjNkMGJhZWU1NjRhZjA0MWQ3MWM0Nzk3N2IwN2NlYzEzNWNhZTJhMzE3MDM4ZGQ4YWY1NyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGJjMjdiZTVjMWU2NTg3MmQ5ZGJhYjRkMWUzMTg1OGNkYjc3M2FiNjI0ZGYwYTYyZjQ4NTcwODMwNDg0YjFjNiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjliYzU0OTFmNDY5MjYxNGY3MTlmNjQ5OWNkYzYwNGU0M2U2YWNjZGYwYjllY2ZhY2ExOTUzZTVhOGEzM2EwYiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzZjMTJmZDc5YjJjNzczYzA1N2VkYmNiYzEzZDFhM2Y4NzYyNzA4MDNlMDk4YTU5ODk1MDJiMDMwMGNmMWI5ZCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjM1ODQyYzFlNDEzM2E3NWMxNjdkMWMyZDAwYzIxYTViMTQwOGZkYmE0MDg5MjM2MWJmMzBiNWM1ZDZiZjAwMiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2EzZWNjZTNmYjcxNTdjNjNkYTJkODU2MjI3MDE2ODQ2ZTliMzExNTBiOTNkMmViNzU4NmQ1YzE5N2RmNTVlNCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmZiYThkODk1YjUwM2FjOTgzZGRhMjUxYTJjZWE2M2QxZmExNDhiZWFhMThkZmM1YjY1OTQ0YWExMDczYzI1MyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjZmMmJlNjdhOTRmYmY0MGZlMDQ1MmViYjdkZjMwOTc1OTk1NmMzYTYwMGQxMjkyMjgxMWNmMGMwNWIyZjI4YiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWVhYzI1NTdlMjQ4MWVhMTc1NmJjM2EyYjg3MWFkMzI1MTE2YzgzNGJjNjY4MTVhMTg5NmU0OGYxMTJhNDlmOCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGFlODczOGJhNmJiMDA0Y2M2MzhlNGFmMzQyODA0MDNhMjUyNjVlZjM2OWQ2ODk2M2Y0N2EzYzdlYjZjNGRiMCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWY4MmM2YmJlZTZkOTM2M2ZhOTA2NDA1ZDBjZjc2OTJmOGM3Mzg2NzNiNDEzNTgyMGFhNjA5ZTgyZDY4NGQzMiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTZjOTExZGE2M2JlOTdlNjg5ODM1ZmFlNDM5OWI4ZDRiMjJjNGE2YzA5NjAxYTA0NzBjMjJmOTI5YWQ5NDVjZSJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGMzYjQyMzQxNjkwMzdiNmM4Y2NjMzFjYjk5YTBlNzkwMmNmMjZhNDRlYzQwODhlNjZmYjE5MjNjYzFmZjczZCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmU4YzE4ODFkMjUxOWI2NjUzMTRhNzE2ZTg5ODBhOTNlMzNjMmE2MWQ0OGVmZmY0MzdiM2JlNGFmMTQzMWRhIn19fQ==",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjEwOGVlNjc3Y2ZiZjdiOTU2ZjhlNDlmMjg0ZWQ1ZDQ4NjAxYTY1ZDIzMjlmNGRlOGM1N2FiZjcyYzZmZDEzOCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmEyZDA4MTZkZGJkOTVjZWNiNDQ3YjEzMDBmOThmNmMyODJmYTJjNTQxYjdjZDBiOTM3MmYxZjhhYjg2ODMzNSJ9fX0="
    };

    Config cfg = new Config(this);

    @Override
    public void onEnable() {
        // 复制 jar 内默认资源到插件文件夹作为模板（已存在则不覆盖）
        saveDefaultConfig();
        saveResource("group.json", false);
        saveResource("station.json", false);

        /*
         * 1. 创建分类
         * 分类的显示物品将使用以下物品
         */
        ItemStack itemGroupItem = new CustomItemStack(Material.MINECART, "&6轨道交通");
        NamespacedKey itemGroupId = new NamespacedKey(this, "railway_transit");
        ItemGroup itemGroup = new ItemGroup(itemGroupId, itemGroupItem);

        // 列车
        SlimefunItemStack slimefunItem_train = new SlimefunItemStack("RT_TRAIN", Material.MINECART, "&4列车", "只是列车");
        ItemStack[] recipe_train = { null, null, null, null, new ItemStack(Material.FURNACE_MINECART), null, null, null,
                null };
        Train item_train = new Train(itemGroup, slimefunItem_train, RecipeType.ENHANCED_CRAFTING_TABLE, recipe_train);
        item_train.register(this);

        // 指挥台
        SlimefunItemStack slimefunItem_controller = new SlimefunItemStack("RT_CONTROLLER", Material.LECTERN, "&4指挥台",
                "规划中");
        ItemStack[] recipe_controller = { SlimefunItems.COPPER_WIRE, new ItemStack(Material.REDSTONE_BLOCK),
                SlimefunItems.COPPER_WIRE, SlimefunItems.COPPER_WIRE, SlimefunItems.BASIC_CIRCUIT_BOARD,
                SlimefunItems.COPPER_WIRE, SlimefunItems.COPPER_WIRE, SlimefunItems.COPPER_WIRE,
                SlimefunItems.COPPER_WIRE };
        controller = new Controller(itemGroup, slimefunItem_controller,
                RecipeType.ENHANCED_CRAFTING_TABLE, recipe_controller);
        controller.register(this);

        // 列车自动驾驶引擎
        cartAI = new CartAI(this);

        // 指令
        RTCommand RailTransitCommand = new RTCommand(this);
        getCommand("railtransit").setExecutor(RailTransitCommand);
        getCommand("railtransit").setTabCompleter(RailTransitCommand);
        getCommand("railtransfer").setExecutor(RailTransitCommand);
        getCommand("railtransfer").setTabCompleter(RailTransitCommand);

        // 私聊询问（Ask）
        chatPrompter = new ChatPrompter(this);

        // 服务器重启后恢复世界上的列车（绑定过线路的矿车），延迟等待世界区块加载完毕
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (controller != null) {
                controller.restoreTrains();
            }
        }, 100L); // 5 秒后执行
    }

    @Override
    public void onDisable() {
        // 禁用插件的逻辑...
    }

    public class RTRailway extends SlimefunItem {
        public RTRailway(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
            super(itemGroup, item, recipeType, recipe);
        }
    }

    public class RTStopway extends SlimefunItem {
        public RTStopway(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
            super(itemGroup, item, recipeType, recipe);
        }
    }

    public class RTBackway extends SlimefunItem {
        public RTBackway(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
            super(itemGroup, item, recipeType, recipe);
        }
    }

    public class Train extends SlimefunItem {
        public Train(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
            super(itemGroup, item, recipeType, recipe);
        }
    }

    public class Controller extends SlimefunItem implements Listener {
        // 等待添加站台（空手右键站停铁轨）的玩家
        private final Set<String> waitingAddStation = Collections.synchronizedSet(new HashSet<>());
        // 等待重新编译（空手右键侧线铁轨）的玩家 -> [组织索引, 线路索引]
        private final Map<String, int[]> waitingCompile = Collections.synchronizedMap(new HashMap<>());
        // 玩家最近一次编译的线路 -> [组织索引, 线路索引]（供 /railtransfer continue 无参续编时使用）
        private final Map<String, int[]> lastCompile = Collections.synchronizedMap(new HashMap<>());
        // 已选中线路（/railtransfer select）的玩家 -> [组织索引, 线路索引]
        private final Map<String, int[]> selectedLine = Collections.synchronizedMap(new HashMap<>());

        public Controller(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
            super(itemGroup, item, recipeType, recipe);
        }

        public ItemStack getNamedItem(ItemStack thing, String name, @Nullable List<String> lore) {
            ItemStack item = thing; // copy or new ItemStack(Material.AIR)
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }

        public String colored(String val) {
            return "§" + val;
        }

        @Override
        public void preRegister() {
            BlockUseHandler blockUseHandler = this::onBlockRightClick;
            addItemHandler(blockUseHandler);
            // 注册GUI的点击监听器
            getAddon().getJavaPlugin().getServer().getPluginManager().registerEvents(this,
                    getAddon().getJavaPlugin());
        }

        private void onBlockRightClick(PlayerRightClickEvent event) {
            if (!event.getPlayer().isSneaking()) { // 允许：放置告示牌
                event.cancel();
                // GUI：27 格
                Inventory inventory = Bukkit.createInventory(null, 9, "§a控制台");
                ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
                for (int i = 0; i < 9; i++) {
                    inventory.setItem(i, pane.clone());
                }
                inventory.setItem(2, getNamedItem(new ItemStack(Material.SPYGLASS), "§6查看组织", null));
                inventory.setItem(3, getNamedItem(new ItemStack(Material.RED_BANNER), "§6成立组织", null));
                inventory.setItem(4, getNamedItem(new ItemStack(Material.TINTED_GLASS), "§6管理组织", null));
                inventory.setItem(5, getNamedItem(new ItemStack(Material.MAP), "§6查看铁路", null));
                inventory.setItem(6, getNamedItem(new ItemStack(Material.WRITABLE_BOOK), "§6规划铁路", null));
                event.getPlayer().openInventory(inventory);
            }
        }

        private void CheckGroup(InventoryClickEvent event, Integer page, Boolean close) {
            Player player = (Player) event.getWhoClicked();
            Gson gson = new Gson();
            JsonElement group_root = null;
            try (Reader reader = loadGroupReader()) {
                group_root = gson.fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("无法加载配置文件", e);
            }
            JsonObject group_json = group_root.getAsJsonObject();
            int total = group_json.getAsJsonArray("group").size();
            int totalPages = Math.max(1, (total + 17) / 18); // 总页数向上取整，至少 1 页
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27, "§a查询组织 - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));

            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                JsonObject group = group_json.getAsJsonArray("group").get(index).getAsJsonObject();
                inventory.setItem(i,
                        getNamedItem(new CustomItemStack(SlimefunUtils.getCustomHead(group.get("icon").getAsString())),
                                colored("6") + group.get("name").getAsString(),
                                List.of(colored("5") + group.get("owner").getAsString(),
                                        colored("3") + group.get("info").getAsString())));
            }
            player.openInventory(inventory);
        }

        private void GroupInfo(InventoryClickEvent event, Integer group, Integer page, Boolean close) {
            Player player = (Player) event.getWhoClicked();
            Gson gson = new Gson();
            JsonElement group_root = null;
            try (Reader reader = loadGroupReader()) {
                group_root = gson.fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("无法加载配置文件", e);
            }
            JsonObject group_json = group_root.getAsJsonObject();
            int total = group_json.getAsJsonArray("group").get(group).getAsJsonObject().get("member").getAsJsonArray()
                    .size();
            int totalPages = Math.max(1, (total + 17) / 18); // 总页数向上取整，至少 1 页
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27, "§a组织信息 - " + String.valueOf(group));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));

            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                String group_member = group_json.getAsJsonArray("group").get(group).getAsJsonObject().get("member")
                        .getAsJsonArray().get(i).getAsString();
                if (group_member.equals(
                        group_json.getAsJsonArray("group").get(group).getAsJsonObject().get("owner").getAsString())) {
                    inventory.setItem(i, getNamedItem(new CustomItemStack(SlimefunUtils.getCustomHead(
                            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjI5MTUxYjZiZTMxNjllYjQ0Y2IyODVlYThhOWU0NTkwOTc1NWE4NzZmODlmZTAzNmZlN2E2MjZmYjZmNTMxNSJ9fX0=")),
                            colored("6") + group_member, null));
                } else {
                    inventory.setItem(i, getNamedItem(new CustomItemStack(SlimefunUtils.getCustomHead(
                            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjA5ZDZhMzQ4YTk5MjNlYmJiMzQyM2IxOTY5NzNkNDk1YjFhYTVjMDU2ZTRiMzUxYjEyMGQzNmQ4MzMxYTU0MiJ9fX0=")),
                            colored("6") + group_member, null));
                }
            }
            player.openInventory(inventory);
        }

        private void ManageMember(InventoryClickEvent event, Integer page, Boolean close) {
            Player player = (Player) event.getWhoClicked();
            Gson gson = new Gson();
            JsonElement group_root = null;
            try (Reader reader = loadGroupReader()) {
                group_root = gson.fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("无法加载配置文件", e);
            }
            JsonObject group_json = group_root.getAsJsonObject();
            JsonArray groups = group_json.getAsJsonArray("group");
            Integer id = -1;
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i).getAsJsonObject().get("owner").getAsString().equals(player.getDisplayName())) {
                    id = i;
                    break;
                }
            }
            if (id == -1) {
                player.closeInventory();
                player.sendMessage("§c你不是任何组织的创建者，无法管理成员");
                return;
            }
            int total = groups.get(id).getAsJsonObject().get("member").getAsJsonArray().size();
            int totalPages = Math.max(1, (total + 17) / 18); // 总页数向上取整，至少 1 页
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27, "§1管理成员 - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));

            JsonArray memberArray = groups.get(id).getAsJsonObject().getAsJsonArray("member");
            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                String group_member = memberArray.get(index).getAsString();
                if (group_member.equals(groups.get(id).getAsJsonObject().get("owner").getAsString())) {
                    inventory.setItem(i, getNamedItem(new CustomItemStack(SlimefunUtils.getCustomHead(
                            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjI5MTUxYjZiZTMxNjllYjQ0Y2IyODVlYThhOWU0NTkwOTc1NWE4NzZmODlmZTAzNmZlN2E2MjZmYjZmNTMxNSJ9fX0=")),
                            colored("6") + group_member, null));
                } else {
                    inventory.setItem(i, getNamedItem(new CustomItemStack(SlimefunUtils.getCustomHead(
                            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjA5ZDZhMzQ4YTk5MjNlYmJiMzQyM2IxOTY5NzNkNDk1YjFhYTVjMDU2ZTRiMzUxYjEyMGQzNmQ4MzMxYTU0MiJ9fX0=")),
                            colored("6") + group_member, null));
                }
            }
            player.openInventory(inventory);
        }

        private Reader loadGroupReader() throws IOException {
            File file = new File(getAddon().getJavaPlugin().getDataFolder(), "group.json");
            if (file.exists()) {
                return new FileReader(file);
            }
            java.io.InputStream in = getAddon().getJavaPlugin().getResource("group.json");
            if (in != null) {
                return new java.io.InputStreamReader(in, StandardCharsets.UTF_8);
            }
            throw new IOException("group.json 不存在且 jar 内无 group.json");
        }

        private void MakeGroup(Player player) {
            player.closeInventory();
            Inventory inventory = Bukkit.createInventory(null, 18, colored("6") + "选择组织图标");
            for (int i = 0; i < icon_base64.length; i++) {
                inventory.setItem(i, getNamedItem(new CustomItemStack(SlimefunUtils.getCustomHead(icon_base64[i])),
                        colored("7") + String.valueOf(i + 1), null));
            }
            player.openInventory(inventory);
        }

        private void GroupSettings(Player player) {
            player.closeInventory();
            Inventory inventory = Bukkit.createInventory(null, 9, colored("1") + "组织设置");
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(2, getNamedItem(new ItemStack(Material.WRITABLE_BOOK), colored("9") + "更改组织介绍", null));
            inventory.setItem(3, getNamedItem(new ItemStack(Material.ELYTRA), colored("9") + "邀请成员", null));
            inventory.setItem(4, getNamedItem(new ItemStack(Material.BOOKSHELF), colored("9") + "成员管理", null));
            inventory.setItem(5, getNamedItem(new ItemStack(Material.SNOWBALL), colored("9") + "传话", null));
            inventory.setItem(6, getNamedItem(new ItemStack(Material.LAVA_BUCKET), colored("9") + "解散组织", null));
            player.openInventory(inventory);
        }

        private void DoPlayer(Integer group, Integer who, Player player) {
            player.closeInventory();
            Gson gson = new Gson();
            JsonElement group_root = null;
            try (Reader reader = loadGroupReader()) {
                group_root = gson.fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("无法加载配置文件", e);
            }
            JsonObject group_json = group_root.getAsJsonObject();
            String targetName = group_json.getAsJsonArray("group").get(group).getAsJsonObject().get("member")
                    .getAsJsonArray().get(who).getAsString();
            Inventory inventory = Bukkit.createInventory(null, 9, colored("1") + "玩家 - " + String.valueOf(who));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(3, getNamedItem(new ItemStack(Material.ELYTRA), colored("9") + "组织转让", null));
            inventory.setItem(4, getNamedItem(new ItemStack(Material.PLAYER_HEAD), colored("9") + targetName, null));
            inventory.setItem(5, getNamedItem(new ItemStack(Material.LAVA_BUCKET), colored("9") + "踢出成员", null));
            player.openInventory(inventory);
        }

        // ============ 规划铁路 ============

        private void PlanRailway(Player player) {
            player.closeInventory();
            Inventory inventory = Bukkit.createInventory(null, 9, "§6规划铁路");
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(3, getNamedItem(new ItemStack(Material.EMERALD_BLOCK), "§6添加站台",
                    List.of(colored("7") + "退出后空手右键站停铁轨")));
            inventory.setItem(4, getNamedItem(new ItemStack(Material.PLAYER_HEAD), "§6已加入组织", null));
            inventory.setItem(5, getNamedItem(new ItemStack(Material.WRITABLE_BOOK), "§6编辑线路", null));
            player.openInventory(inventory);
        }

        private Reader loadStationReader() throws IOException {
            File file = new File(getAddon().getJavaPlugin().getDataFolder(), "station.json");
            if (file.exists()) {
                return new FileReader(file);
            }
            java.io.InputStream in = getAddon().getJavaPlugin().getResource("station.json");
            if (in != null) {
                return new java.io.InputStreamReader(in, StandardCharsets.UTF_8);
            }
            throw new IOException("station.json 不存在且 jar 内无 station.json");
        }

        private JsonObject loadStationJson() {
            Gson gson = new Gson();
            JsonElement station_root = null;
            try (Reader reader = loadStationReader()) {
                station_root = gson.fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("无法加载配置文件", e);
            }
            return station_root.getAsJsonObject();
        }

        private void saveStation(JsonObject station_json) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = gson.toJson(station_json);
            File dataFolder = getAddon().getJavaPlugin().getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            try (FileWriter writer = new FileWriter(new File(dataFolder, "station.json"))) {
                writer.write(jsonString);
            } catch (IOException e) {
                getLogger().severe(e.getLocalizedMessage());
            }
        }

        // 玩家已加入的组织（group[] 全局索引）
        private List<Integer> findJoinedGroups(Player player) {
            Gson gson = new Gson();
            JsonElement group_root = null;
            try (Reader reader = loadGroupReader()) {
                group_root = gson.fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("无法加载配置文件", e);
            }
            JsonObject group_json = group_root.getAsJsonObject();
            JsonArray groups = group_json.getAsJsonArray("group");
            List<Integer> joined = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                JsonObject g = groups.get(i).getAsJsonObject();
                JsonArray members = g.getAsJsonArray("member");
                for (int j = 0; j < members.size(); j++) {
                    if (members.get(j).getAsString().equals(player.getDisplayName())) {
                        joined.add(i);
                        break;
                    }
                }
            }
            return joined;
        }

        // 某组织拥有的线路（line[] 全局索引）
        private List<Integer> findOrgLines(JsonObject station_json, int orgIndex) {
            JsonArray lines = station_json.getAsJsonArray("line");
            List<Integer> orgLines = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).getAsJsonObject().get("organizer").getAsInt() == orgIndex) {
                    orgLines.add(i);
                }
            }
            return orgLines;
        }

        // 编辑线路 - 选择玩家已加入的组织（27 格分页）
        private void LineSelect(InventoryClickEvent event, Integer page, Boolean close) {
            Player player = (Player) event.getWhoClicked();
            List<Integer> joined = findJoinedGroups(player);
            int total = joined.size();
            int totalPages = Math.max(1, (total + 17) / 18);
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27, "§6线路选择 - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));
            Gson gson = new Gson();
            JsonElement group_root = null;
            try (Reader reader = loadGroupReader()) {
                group_root = gson.fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("无法加载配置文件", e);
            }
            JsonArray groups = group_root.getAsJsonObject().getAsJsonArray("group");
            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                int orgIndex = joined.get(index);
                JsonObject group = groups.get(orgIndex).getAsJsonObject();
                inventory.setItem(i,
                        getNamedItem(new CustomItemStack(SlimefunUtils.getCustomHead(group.get("icon").getAsString())),
                                colored("6") + group.get("name").getAsString(),
                                List.of(colored("7") + "组织编号 #" + orgIndex,
                                        colored("3") + group.get("info").getAsString())));
            }
            player.openInventory(inventory);
        }

        // 线路面板（9 格）
        private void LinePanel(Player player, int orgIndex) {
            player.closeInventory();
            Inventory inventory = Bukkit.createInventory(null, 9, "§6线路面板 - " + String.valueOf(orgIndex));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, pane.clone());
            }
            int lineCount = findOrgLines(loadStationJson(), orgIndex).size();
            inventory.setItem(3, getNamedItem(new ItemStack(Material.BOOK), "§6编辑已有线路", null));
            inventory.setItem(4, getNamedItem(new ItemStack(Material.PLAYER_HEAD), "§6已有线路数",
                    List.of(colored("3") + lineCount + " 条")));
            inventory.setItem(5, getNamedItem(new ItemStack(Material.WRITABLE_BOOK), "§6添加线路", null));
            player.openInventory(inventory);
        }

        // 编辑已有线路（27 格分页，显示某组织所有线路）
        private void EditLineSelect(InventoryClickEvent event, Integer orgIndex, Integer page, Boolean close) {
            Player player = (Player) event.getWhoClicked();
            JsonObject station_json = loadStationJson();
            List<Integer> orgLines = findOrgLines(station_json, orgIndex);
            int total = orgLines.size();
            int totalPages = Math.max(1, (total + 17) / 18);
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27,
                    "§6编辑已有线路 - " + orgIndex + " - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));
            JsonArray lines = station_json.getAsJsonArray("line");
            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                int lineIndex = orgLines.get(index);
                JsonObject line = lines.get(lineIndex).getAsJsonObject();
                inventory.setItem(i, getNamedItem(new ItemStack(Material.RAIL),
                        colored("6") + line.get("name").getAsString(),
                        List.of(colored("7") + "线路编号 " + line.get("id").getAsString(),
                                colored("3") + "站台数 " + line.get("station").getAsJsonArray().size())));
            }
            player.openInventory(inventory);
        }

        // 线路编辑（9 格）
        private void LineEdit(Player player, int orgIndex, int lineIndex) {
            player.closeInventory();
            Inventory inventory = Bukkit.createInventory(null, 9,
                    "§6线路编辑 - " + orgIndex + " - " + lineIndex);
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(3, getNamedItem(new ItemStack(Material.RAIL), "§6编辑经过站台", null));
            inventory.setItem(4, getNamedItem(new ItemStack(Material.COMMAND_BLOCK), "§6编译",
                    null));
            inventory.setItem(5, getNamedItem(new ItemStack(Material.BARRIER), "§6删除线路", null));
            player.openInventory(inventory);
        }

        // 编译界面（9 格）
        private void CompilePanel(Player player, int orgIndex, int lineIndex) {
            player.closeInventory();
            JsonObject station_json = loadStationJson();
            JsonArray lines = station_json.getAsJsonArray("line");
            if (lineIndex < 0 || lineIndex >= lines.size()) {
                player.sendMessage("§c线路不存在");
                return;
            }
            JsonObject line = lines.get(lineIndex).getAsJsonObject();
            boolean back = line.has("back") && line.get("back").getAsBoolean();
            boolean compile = isCompiled(line);
            int outSec = line.has("out") ? line.get("out").getAsInt() : 5;
            int waitSec = line.has("wait") ? line.get("wait").getAsInt() : 2;
            Inventory inventory = Bukkit.createInventory(null, 9,
                    "§6编译界面 - " + orgIndex + " - " + lineIndex);
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(3, getNamedItem(new ItemStack(Material.REPEATER), "§6是否回厂",
                    List.of(colored("7") + "当前：" + (back ? "§a开启（单行线）" : "§c关闭（环线）"),
                            colored("7") + "点击切换")));
            inventory.setItem(4, getNamedItem(new ItemStack(Material.COMMAND_BLOCK), "§6重新编译",
                    List.of(colored("7") + "退出后空手右键侧线铁轨")));
            inventory.setItem(5, getNamedItem(new ItemStack(compile ? Material.GREEN_STAINED_GLASS
                    : Material.RED_STAINED_GLASS), "§6编译结果",
                    List.of(compile ? colored("a") + "已编译" : colored("c") + "未编译",
                            colored("7") + "回车厂后 " + outSec + " 秒再次发车，站停 " + waitSec + " 秒")));
            player.openInventory(inventory);
        }

        // 站台编辑（9 格）
        private void StationEdit(Player player, int orgIndex, int lineIndex) {
            player.closeInventory();
            Inventory inventory = Bukkit.createInventory(null, 9,
                    "§6站台编辑 - " + orgIndex + " - " + lineIndex);
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(3, getNamedItem(new ItemStack(Material.EMERALD_BLOCK), "§6添加站台", null));
            inventory.setItem(4, getNamedItem(new ItemStack(Material.MAP), "§6预览站台", null));
            inventory.setItem(5, getNamedItem(new ItemStack(Material.REDSTONE_BLOCK), "§6删除站台", null));
            player.openInventory(inventory);
        }

        // 添加站台列表（27 格分页，显示世界所有站台）
        private void StationListAdd(InventoryClickEvent event, Integer orgIndex, Integer lineIndex, Integer page,
                Boolean close) {
            Player player = (Player) event.getWhoClicked();
            JsonObject station_json = loadStationJson();
            JsonArray platforms = station_json.getAsJsonArray("platform");
            JsonArray stations = station_json.getAsJsonArray("line").get(lineIndex).getAsJsonObject()
                    .getAsJsonArray("station");
            int total = platforms.size();
            int totalPages = Math.max(1, (total + 17) / 18);
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[3]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27,
                    "§6添加站台 - " + orgIndex + " - " + lineIndex + " - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));
            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                JsonObject platform = platforms.get(index).getAsJsonObject();
                boolean has = false;
                for (int j = 0; j < stations.size(); j++) {
                    if (stations.get(j).getAsInt() == index) {
                        has = true;
                        break;
                    }
                }
                inventory.setItem(i, getNamedItem(new ItemStack(Material.STONE_BUTTON),
                        colored("6") + platform.get("name").getAsString(),
                        List.of(colored("7") + "站台编号 #" + index,
                                colored("3") + (has ? "已在线路中" : "点击添加"))));
            }
            player.openInventory(inventory);
        }

        // 预览站台列表（27 格分页，只读显示线路所有站台）
        private void StationListPreview(InventoryClickEvent event, Integer orgIndex, Integer lineIndex, Integer page,
                Boolean close) {
            Player player = (Player) event.getWhoClicked();
            JsonObject station_json = loadStationJson();
            JsonArray platforms = station_json.getAsJsonArray("platform");
            JsonArray stations = station_json.getAsJsonArray("line").get(lineIndex).getAsJsonObject()
                    .getAsJsonArray("station");
            int total = stations.size();
            int totalPages = Math.max(1, (total + 17) / 18);
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[3]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27,
                    "§6预览站台 - " + orgIndex + " - " + lineIndex + " - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));
            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                int platformIndex = stations.get(index).getAsInt();
                JsonObject platform = platforms.get(platformIndex).getAsJsonObject();
                inventory.setItem(i, getNamedItem(new ItemStack(Material.MAP),
                        colored("6") + platform.get("name").getAsString(),
                        List.of(colored("7") + "站台编号 #" + platformIndex)));
            }
            player.openInventory(inventory);
        }

        // 删除站台列表（27 格分页，点击移除）
        private void StationListRemove(InventoryClickEvent event, Integer orgIndex, Integer lineIndex, Integer page,
                Boolean close) {
            Player player = (Player) event.getWhoClicked();
            JsonObject station_json = loadStationJson();
            JsonArray platforms = station_json.getAsJsonArray("platform");
            JsonArray stations = station_json.getAsJsonArray("line").get(lineIndex).getAsJsonObject()
                    .getAsJsonArray("station");
            int total = stations.size();
            int totalPages = Math.max(1, (total + 17) / 18);
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[3]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27,
                    "§6删除站台 - " + orgIndex + " - " + lineIndex + " - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));
            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                int platformIndex = stations.get(index).getAsInt();
                JsonObject platform = platforms.get(platformIndex).getAsJsonObject();
                inventory.setItem(i, getNamedItem(new ItemStack(Material.REDSTONE_BLOCK),
                        colored("6") + platform.get("name").getAsString(),
                        List.of(colored("7") + "站台编号 #" + platformIndex, colored("3") + "点击删除")));
            }
            player.openInventory(inventory);
        }

        // 删除线路列表（27 格分页，点击删除）
        private void LineListDelete(InventoryClickEvent event, Integer orgIndex, Integer page, Boolean close) {
            Player player = (Player) event.getWhoClicked();
            JsonObject station_json = loadStationJson();
            List<Integer> orgLines = findOrgLines(station_json, orgIndex);
            int total = orgLines.size();
            int totalPages = Math.max(1, (total + 17) / 18);
            if (close) {
                player.closeInventory();
            } else {
                page += Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                player.closeInventory();
            }
            if (page < 1) {
                page = 1;
            } else if (page > totalPages) {
                page = totalPages;
            }
            Inventory inventory = Bukkit.createInventory(null, 27,
                    "§6删除线路 - " + orgIndex + " - " + String.valueOf(page));
            ItemStack pane = getNamedItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), "§7 ", null);
            for (int i = 0; i < 27; i++) {
                inventory.setItem(i, pane.clone());
            }
            inventory.setItem(18, getNamedItem(new ItemStack(Material.ARROW), "§6上一页", null));
            inventory.setItem(26, getNamedItem(new ItemStack(Material.ARROW), "§6下一页", null));
            JsonArray lines = station_json.getAsJsonArray("line");
            for (int i = 0; i < 18; i++) {
                int index = (page - 1) * 18 + i;
                if (index >= total) {
                    break;
                }
                int lineIndex = orgLines.get(index);
                JsonObject line = lines.get(lineIndex).getAsJsonObject();
                inventory.setItem(i, getNamedItem(new ItemStack(Material.BARRIER),
                        colored("6") + line.get("name").getAsString(),
                        List.of(colored("7") + "线路编号 " + line.get("id").getAsString(),
                                colored("3") + "点击删除")));
            }
            player.openInventory(inventory);
        }

        // 添加站台：等待玩家空手右键站停铁轨（激活铁轨），右键后异步 Ask 站台名并写 platform
        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            // 放置列车：玩家已选中线路，在铁轨上放置 RT_TRAIN 时按编译路径启动 AI
            // （环线模式下可不在侧线铁轨上启动，从 compile 中坐标匹配的关键点开始）
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                    && cartRunning.isRail(event.getClickedBlock())) {
                int[] sel = selectedLine.get(player.getName());
                if (sel != null) {
                    ItemStack hand = event.getItem();
                    if (hand != null && hand.getType() == Material.MINECART) {
                        SlimefunItem sf = SlimefunItem.getByItem(hand);
                        if (sf != null && "RT_TRAIN".equals(sf.getId())) {
                            startTrainOnSideRail(player, event, event.getClickedBlock(), sel[0], sel[1]);
                            return;
                        }
                    }
                }
            }
            // 重新编译：等待空手右键侧线铁轨作为线路起点
            if (waitingCompile.containsKey(player.getName())) {
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                    if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                        player.sendMessage("§c请空手右键一个侧线铁轨，输入 §ccancel§7 取消");
                    }
                    return;
                }
                Block block = event.getClickedBlock();
                if (block == null || block.getType() != Material.DETECTOR_RAIL) {
                    player.sendMessage("§c请空手右键一个侧线铁轨（探测铁轨），输入 §ccancel§7 取消");
                    return;
                }
                ItemStack hand = event.getItem();
                if (hand != null && hand.getType() != Material.AIR) {
                    player.sendMessage("§c请空手右键侧线铁轨");
                    return;
                }
                try {
                    if (!cartRunning.isSideRail(block)) {
                        player.sendMessage("§c这不是本插件添加的侧线铁轨");
                        return;
                    }
                } catch (Exception e) {
                    getLogger().severe(e.getLocalizedMessage());
                }
                event.setCancelled(true);
                int[] ctx = waitingCompile.remove(player.getName());
                if (ctx == null) {
                    return;
                }
                final int orgIndex = ctx[0];
                final int lineIndex = ctx[1];
                final Block sideBlock = block;
                // 编译 DFS 必须同步在主线程执行：异步线程访问世界（getRelative/getType）
                // 会触发区块加载风暴，曾导致服务器 watchdog 卡死
                Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                    try {
                    JsonObject station_json = loadStationJson();
                    JsonArray lines = station_json.getAsJsonArray("line");
                    if (lineIndex < 0 || lineIndex >= lines.size()) {
                        Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                            player.sendMessage("§c线路不存在，编译失败");
                        });
                        return;
                    }
                    JsonObject line = lines.get(lineIndex).getAsJsonObject();
                    JsonArray platforms = station_json.getAsJsonArray("platform");
                    // 记录本次编译的起点侧线坐标（供 /railtransfer continue 续编时定位，无需玩家返回侧线）
                    line.addProperty("compileStart",
                            sideBlock.getX() + "," + sideBlock.getY() + "," + sideBlock.getZ());
                    saveStation(station_json);
                    // 记录玩家最近编译的线路（供 continue 无参续编）
                    lastCompile.put(player.getName(), new int[] { orgIndex, lineIndex });
                    List<Block> path = cartRunning.compilePath(sideBlock, line, platforms,
                            msg -> Bukkit.getLogger().info("[RailTransit-DFS] " + msg),
                            msg -> player.sendMessage(msg));
                    Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                        JsonObject station_json2 = loadStationJson();
                        JsonArray lines2 = station_json2.getAsJsonArray("line");
                        if (lineIndex >= 0 && lineIndex < lines2.size()) {
                            JsonObject line2 = lines2.get(lineIndex).getAsJsonObject();
                            if (!path.isEmpty()) {
                                // 编译成功：写入路径关键点（侧线起点、拐角、每站位置、终点侧线）
                                JsonArray compileArr = new JsonArray();
                                for (Block b : path) {
                                    compileArr.add(b.getX() + "," + b.getY() + "," + b.getZ());
                                }
                                line2.add("compile", compileArr);
                                saveStation(station_json2);
                                player.sendMessage("§a编译成功！该线路已可正常寻路运行");
                            } else {
                                // 编译失败：空数组视为未编译/失败
                                line2.add("compile", new JsonArray());
                                saveStation(station_json2);
                                player.sendMessage("§7编译未完成，详见上方提示；可移动到提示位置附近后输入 §f/railtransfer continue §7继续");
                            }
                        }
                    });
                    } catch (Throwable t) {
                        player.sendMessage("§c编译异常中断：" + t.getClass().getSimpleName()
                                + "，请检查铁轨连接是否异常，可稍后重试或分段编译");
                    }
                });
                return;
            }
            if (!waitingAddStation.contains(player.getName())) {
                return;
            }
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                    player.sendMessage("§c请空手右键一个站停铁轨，输入 §ccancel§7 取消");
                }
                return;
            }
            Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.ACTIVATOR_RAIL) {
                player.sendMessage("§c请空手右键一个站停铁轨（激活铁轨），输入 §ccancel§7 取消");
                return;
            }
            ItemStack hand = event.getItem();
            if (hand != null && hand.getType() != Material.AIR) {
                player.sendMessage("§c请空手右键站停铁轨");
                return;
            }
            event.setCancelled(true);
            waitingAddStation.remove(player.getName());
            final int bx = block.getX();
            final int by = block.getY();
            final int bz = block.getZ();
            Bukkit.getScheduler().runTaskAsynchronously(getAddon().getJavaPlugin(), () -> {
                String stationName = chatPrompter.Ask(player, "§6请输入站台名称：");
                if (stationName == null) {
                    player.sendMessage("§c已取消添加站台");
                    return;
                }
                JsonObject station_json = loadStationJson();
                JsonObject platform = new JsonObject();
                platform.addProperty("name", stationName);
                JsonObject pos = new JsonObject();
                pos.addProperty("x", bx);
                pos.addProperty("y", by);
                pos.addProperty("z", bz);
                platform.add("position", pos);
                station_json.getAsJsonArray("platform").add(platform);
                int newIndex = station_json.getAsJsonArray("platform").size() - 1;
                saveStation(station_json);
                Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                    player.sendMessage("§a站台 §f" + stationName + " §a添加成功（站台编号 #" + newIndex + "）");
                });
            });
        }

        // 等待右键状态中，输入 cancel 取消
        @EventHandler
        public void onChatCancel(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            if (event.getMessage().trim().equalsIgnoreCase("cancel")) {
                if (waitingAddStation.remove(player.getName())) {
                    event.setCancelled(true);
                    player.sendMessage("§c已取消添加站台");
                } else if (waitingCompile.remove(player.getName()) != null) {
                    event.setCancelled(true);
                    player.sendMessage("§c已取消重新编译");
                }
            }
        }

        // compile 为数组且非空 = 已编译成功
        private boolean isCompiled(JsonObject line) {
            return line.has("compile") && line.get("compile").isJsonArray()
                    && line.getAsJsonArray("compile").size() > 0;
        }

        // 区块加载时恢复其中未运行的列车（覆盖启动后玩家接近才加载区块的场景）
        @EventHandler
        public void onChunkLoad(ChunkLoadEvent event) {
            // 先判断区块内是否有矿车，避免无谓的配置文件 IO
            Entity[] ents = event.getChunk().getEntities();
            boolean hasCart = false;
            for (Entity e : ents) {
                if (e instanceof Minecart) {
                    hasCart = true;
                    break;
                }
            }
            if (!hasCart) {
                return;
            }
            JsonObject station_json = loadStationJson();
            JsonArray lines = station_json.getAsJsonArray("line");
            for (Entity e : ents) {
                if (e instanceof Minecart) {
                    Minecart cart = (Minecart) e;
                    if (!cartAI.isRunning(cart)) {
                        restoreOne(cart, station_json, lines);
                    }
                }
            }
        }

        // /railtransfer select [组织id] [线路id]：记录玩家选中线路，之后在起点侧线放置列车自动运行
        private void selectLine(Player player, int orgId, String lineId) {
            JsonElement group_root = null;
            try (Reader reader = loadGroupReader()) {
                group_root = new Gson().fromJson(reader, JsonElement.class);
            } catch (IOException e) {
                player.sendMessage("§c无法读取组织配置");
                return;
            }
            JsonArray groups = group_root.getAsJsonObject().getAsJsonArray("group");
            if (orgId < 0 || orgId >= groups.size()) {
                player.sendMessage("§c组织不存在：id=" + orgId);
                return;
            }
            JsonObject station_json = loadStationJson();
            JsonArray lines = station_json.getAsJsonArray("line");
            for (int i = 0; i < lines.size(); i++) {
                JsonObject line = lines.get(i).getAsJsonObject();
                if (line.get("organizer").getAsInt() == orgId && line.get("id").getAsString().equals(lineId)) {
                    selectedLine.put(player.getName(), new int[] { orgId, i });
                    String orgName = groups.get(orgId).getAsJsonObject().get("name").getAsString();
                    player.sendMessage("§a已选中线路 §f" + line.get("name").getAsString()
                            + " §a（组织 §f" + orgName + " §a）");
                    player.sendMessage("§7在该线路起点侧线铁轨上放置列车，即可按编译路径自动运行");
                    return;
                }
            }
            player.sendMessage("§c未找到线路：组织 " + orgId + "，线路 " + lineId);
        }

        // /railtransfer compile [组织id] [线路id]：进入编译等待状态，等待玩家空手右键侧线铁轨作为线路起点
        private void compileLine(Player player, int orgId, String lineId) {
            JsonObject station_json = loadStationJson();
            JsonArray lines = station_json.getAsJsonArray("line");
            int lineIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                JsonObject line = lines.get(i).getAsJsonObject();
                if (line.get("organizer").getAsInt() == orgId && line.get("id").getAsString().equals(lineId)) {
                    lineIndex = i;
                    break;
                }
            }
            if (lineIndex < 0) {
                player.sendMessage("§c未找到线路：组织 " + orgId + "，线路 " + lineId);
                return;
            }
            waitingCompile.put(player.getName(), new int[] { orgId, lineIndex });
            player.sendMessage("§6已进入编译状态，请空手右键一个侧线铁轨（探测铁轨）作为该线路起点，输入 §ccancel§7 可取消");
        }

        // /railtransfer continue [组织id] [线路id]：基于上次编译的起点侧线继续编译，
        // 玩家无需返回侧线铁轨，在未加载区块位置附近加载区块后原地执行即可
        private void continueCompile(Player player, int orgId, String lineId) {
            int lineIndex;
            if (orgId < 0) {
                // 无参：使用该玩家最近一次右键侧线编译的线路
                int[] last = lastCompile.get(player.getName());
                if (last == null) {
                    player.sendMessage("§c未找到你最近编译的线路，请使用 /railtransfer continue [组织id] [线路id]");
                    return;
                }
                lineIndex = last[1];
            } else {
                // 带参：按组织 + 线路id 定位
                JsonObject station_json = loadStationJson();
                JsonArray lines = station_json.getAsJsonArray("line");
                lineIndex = -1;
                for (int i = 0; i < lines.size(); i++) {
                    JsonObject line = lines.get(i).getAsJsonObject();
                    if (line.get("organizer").getAsInt() == orgId && line.get("id").getAsString().equals(lineId)) {
                        lineIndex = i;
                        break;
                    }
                }
                if (lineIndex < 0) {
                    player.sendMessage("§c未找到线路：组织 " + orgId + "，线路 " + lineId);
                    return;
                }
            }
            final int fLineIndex = lineIndex;
            // 编译 DFS 必须在主线程执行（避免异步访问世界触发区块加载风暴）
            Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                try {
                JsonObject station_json = loadStationJson();
                JsonArray lines = station_json.getAsJsonArray("line");
                if (fLineIndex < 0 || fLineIndex >= lines.size()) {
                    player.sendMessage("§c线路不存在，无法继续编译");
                    return;
                }
                JsonObject line = lines.get(fLineIndex).getAsJsonObject();
                // 读取上次右键侧线编译时记录的起点侧线坐标
                if (!line.has("compileStart")) {
                    player.sendMessage("§c该线路尚未开始过编译，请先 /railtransfer compile [组织id] [线路id] 并空手右键侧线铁轨");
                    return;
                }
                String[] parts = line.get("compileStart").getAsString().split(",");
                int sx, sy, sz;
                try {
                    sx = Integer.parseInt(parts[0]);
                    sy = Integer.parseInt(parts[1]);
                    sz = Integer.parseInt(parts[2]);
                } catch (Exception e) {
                    player.sendMessage("§c编译起点数据异常，请重新空手右键侧线铁轨开始编译");
                    return;
                }
                org.bukkit.World world = player.getWorld();
                Block sideBlock = world.getBlockAt(sx, sy, sz);
                // 起点侧线区块可能已卸载（玩家已远离侧线）：同步加载该单个区块再取方块，不触发加载风暴
                int cx = sx >> 4;
                int cz = sz >> 4;
                if (!world.isChunkLoaded(cx, cz)) {
                    world.loadChunk(cx, cz, false);
                }
                if (sideBlock.getType() != Material.DETECTOR_RAIL) {
                    player.sendMessage("§c编译起点侧线已被移除，请重新空手右键侧线铁轨开始编译");
                    return;
                }
                JsonArray platforms = station_json.getAsJsonArray("platform");
                List<Block> path = cartRunning.compilePath(sideBlock, line, platforms,
                        msg -> Bukkit.getLogger().info("[RailTransit-DFS] " + msg),
                        msg -> player.sendMessage(msg));
                JsonObject station_json2 = loadStationJson();
                JsonArray lines2 = station_json2.getAsJsonArray("line");
                if (fLineIndex >= 0 && fLineIndex < lines2.size()) {
                    JsonObject line2 = lines2.get(fLineIndex).getAsJsonObject();
                    if (!path.isEmpty()) {
                        JsonArray compileArr = new JsonArray();
                        for (Block b : path) {
                            compileArr.add(b.getX() + "," + b.getY() + "," + b.getZ());
                        }
                        line2.add("compile", compileArr);
                        saveStation(station_json2);
                        player.sendMessage("§a编译成功！该线路已可正常寻路运行");
                    } else {
                        line2.add("compile", new JsonArray());
                        saveStation(station_json2);
                        player.sendMessage("§7编译未完成，详见上方提示；可继续移动到提示位置附近后再次输入 §f/railtransfer continue §7继续");
                    }
                }
                } catch (Throwable t) {
                    player.sendMessage("§c编译异常中断：" + t.getClass().getSimpleName()
                            + "，请检查铁轨连接是否异常，可稍后重试或分段编译");
                }
            });
        }

        // /railtransfer out|wait [组织id] [线路id]：调整线路运行时间（out=回车厂后再次发车秒数，wait=每站停留秒数）
        private void setLineTime(Player player, int orgId, String lineId, String kind) {
            String label = "out".equals(kind) ? "回车厂后再次发车时间" : "每站停留时间";
            Bukkit.getScheduler().runTaskAsynchronously(getAddon().getJavaPlugin(), () -> {
                String input = chatPrompter.Ask(player, "§6请输入线路的" + label + "（秒数）：");
                if (input == null) {
                    player.sendMessage("§c已取消设置");
                    return;
                }
                int sec;
                try {
                    sec = Integer.parseInt(input.trim());
                } catch (NumberFormatException e) {
                    player.sendMessage("§c请输入有效的秒数（整数）");
                    return;
                }
                if (sec < 0) {
                    player.sendMessage("§c秒数不能为负数");
                    return;
                }
                JsonObject station_json = loadStationJson();
                JsonArray lines = station_json.getAsJsonArray("line");
                boolean found = false;
                for (int i = 0; i < lines.size(); i++) {
                    JsonObject line = lines.get(i).getAsJsonObject();
                    if (line.get("organizer").getAsInt() == orgId && line.get("id").getAsString().equals(lineId)) {
                        line.addProperty(kind, sec);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    player.sendMessage("§c未找到线路：组织 " + orgId + "，线路 " + lineId);
                    return;
                }
                saveStation(station_json);
                player.sendMessage("§a已将线路的" + label + "设置为 §f" + sec + " §a秒");
            });
        }

        // 在侧线铁轨放置列车：位置与选中线路 compile 路径起点一致且已编译 -> 取消默认放置，按路径运行
        private void startTrainOnSideRail(Player player, PlayerInteractEvent event, Block sideBlock, int orgIndex,
                int lineIndex) {
            getLogger().info("[RT] startTrainOnSideRail: player=" + player.getName() + " side=("
                    + sideBlock.getX() + "," + sideBlock.getY() + "," + sideBlock.getZ()
                    + ") org=" + orgIndex + " lineIdx=" + lineIndex);
            JsonObject station_json = loadStationJson();
            JsonArray lines = station_json.getAsJsonArray("line");
            if (lineIndex < 0 || lineIndex >= lines.size()) {
                getLogger().warning("[RT] lineIndex out of range: " + lineIndex + ", lineCount=" + lines.size());
                return;
            }
            JsonObject line = lines.get(lineIndex).getAsJsonObject();
            if (line.get("organizer").getAsInt() != orgIndex) {
                getLogger().warning("[RT] organizer mismatch");
                player.sendMessage("§c线路不属于该组织，无法运行");
                return;
            }
            if (!isCompiled(line)) {
                getLogger().warning("[RT] line not compiled");
                player.sendMessage("§c该线路未编译或编译失败，列车无法运行");
                return;
            }
            JsonArray compileArr = line.getAsJsonArray("compile");
            // 解析路径关键点，并定位玩家放置列车所在方块在 compile 中的索引（起始索引）
            List<int[]> path = new ArrayList<>();
            int startIndex = -1;
            for (int i = 0; i < compileArr.size(); i++) {
                String[] p = compileArr.get(i).getAsString().split(",");
                int px = Integer.parseInt(p[0].trim());
                int py = Integer.parseInt(p[1].trim());
                int pz = Integer.parseInt(p[2].trim());
                path.add(new int[] { px, py, pz });
                if (startIndex == -1 && sideBlock.getX() == px && sideBlock.getY() == py
                        && sideBlock.getZ() == pz) {
                    startIndex = i;
                }
            }
            if (startIndex == -1) {
                getLogger().warning("[RT] block not in compile path: (" + sideBlock.getX() + ","
                        + sideBlock.getY() + "," + sideBlock.getZ() + ")");
                player.sendMessage("§c该铁轨不是线路路径上的关键点，列车无法在此运行");
                return;
            }
            getLogger().info("[RT] 起始索引=" + startIndex + " 坐标=(" + sideBlock.getX() + ","
                    + sideBlock.getY() + "," + sideBlock.getZ() + ")");
            // 读取线路运行参数（out=回车厂后再次发车秒数，wait=每站停留秒数）
            boolean back = line.has("back") && line.get("back").getAsBoolean();
            int outSec = line.has("out") ? line.get("out").getAsInt() : 5;
            int waitSec = line.has("wait") ? line.get("wait").getAsInt() : 2;
            String lineIdStr = line.has("id") ? line.get("id").getAsString() : "";
            // 取消默认放置，手动生成列车实体并交由 CartAI 驱动（线路信息绑定到列车实体，循环运行）
            event.setCancelled(true);
            ItemStack hand = event.getItem();
            if (hand.getAmount() > 1) {
                hand.setAmount(hand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            Location loc = sideBlock.getLocation().add(0.5D, 0.05D, 0.5D);
            Minecart cart = sideBlock.getWorld().spawn(loc, Minecart.class);
            if (cartAI.start(cart, path, back, outSec * 20, waitSec * 20, orgIndex, lineIdStr, startIndex)) {
                player.sendMessage("§a列车已接入线路，开始沿编译路径循环运行"
                        + "（站停 §f" + waitSec + " §a秒，回车厂后 §f" + outSec + " §a秒再次发车）");
            } else {
                player.sendMessage("§c列车启动失败");
            }
        }

        // ============ 服务器重启后恢复列车运行 ============

        /**
         * 服务器重启后恢复世界上的列车：扫描所有已加载矿车中绑定线路（PDC rt_org/rt_line）
         * 的列车，从 station.json 读取对应线路 compile 路径，按最近关键点恢复 AI 运行。
         * 在 onEnable 末尾延迟执行（等世界区块加载完毕）。
         */
        private void restoreTrains() {
            JsonObject station_json = loadStationJson();
            JsonArray lines = station_json.getAsJsonArray("line");
            int restored = 0;
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity e : world.getEntities()) {
                    if (!(e instanceof Minecart)) {
                        continue;
                    }
                    Minecart cart = (Minecart) e;
                    if (cartAI.isRunning(cart)) {
                        continue;
                    }
                    restoreOne(cart, station_json, lines);
                    if (cartAI.isRunning(cart)) {
                        restored++;
                    }
                }
            }
            getLogger().info("[RT] 服务器重启恢复列车完成，共恢复 " + restored + " 辆");
        }

        /**
         * 恢复单辆列车：按 PDC 绑定的线路信息从 station.json 读取配置，恢复 AI 运行。
         * 非列车（无 PDC 标记）直接跳过，不恢复。
         */
        private void restoreOne(Minecart cart, JsonObject station_json, JsonArray lines) {
            if (cart == null || cart.isDead()) {
                return;
            }
            String[] trainLine = cartAI.getTrainLine(cart);
            if (trainLine == null) {
                return; // 不是列车（普通矿车），不恢复
            }
            int orgId;
            String lineId;
            try {
                orgId = Integer.parseInt(trainLine[0]);
                lineId = trainLine[1];
            } catch (NumberFormatException e) {
                return;
            }
            // 查找匹配的线路配置
            JsonObject line = null;
            for (int i = 0; i < lines.size(); i++) {
                JsonObject l = lines.get(i).getAsJsonObject();
                if (l.get("organizer").getAsInt() == orgId && l.get("id").getAsString().equals(lineId)) {
                    line = l;
                    break;
                }
            }
            if (line == null || !isCompiled(line)) {
                getLogger().warning("[RT] restore skip: 线路不存在或未编译 org=" + orgId + " line=" + lineId);
                return;
            }
            // 解析 compile 路径
            JsonArray compileArr = line.getAsJsonArray("compile");
            List<int[]> path = new ArrayList<>();
            for (int i = 0; i < compileArr.size(); i++) {
                String[] p = compileArr.get(i).getAsString().split(",");
                path.add(new int[]{
                        Integer.parseInt(p[0].trim()),
                        Integer.parseInt(p[1].trim()),
                        Integer.parseInt(p[2].trim())
                });
            }
            if (path.size() < 2) {
                return;
            }
            // 起点索引：列车当前位置匹配最近的关键点（重启后列车可能停在两关键点之间）
            int startIndex = nearestPathIndex(path, cart.getLocation());
            // 读取线路运行参数
            boolean back = line.has("back") && line.get("back").getAsBoolean();
            int outSec = line.has("out") ? line.get("out").getAsInt() : 5;
            int waitSec = line.has("wait") ? line.get("wait").getAsInt() : 2;
            if (cartAI.start(cart, path, back, outSec * 20, waitSec * 20, orgId, lineId, startIndex)) {
                getLogger().info("[RT] 恢复列车 " + cart.getUniqueId() + " org=" + orgId + " line=" + lineId
                        + " startIndex=" + startIndex + " loc=("
                        + cart.getLocation().getBlockX() + "," + cart.getLocation().getBlockY() + ","
                        + cart.getLocation().getBlockZ() + ")");
            }
        }

        /**
         * 计算 compile 路径中离位置最近的关键点索引。
         * 方块中心坐标 (x+0.5, y+0.05, z+0.5) 与矿车位置比较。
         */
        private int nearestPathIndex(List<int[]> path, Location loc) {
            int best = 0;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < path.size(); i++) {
                int[] p = path.get(i);
                double dx = (p[0] + 0.5D) - loc.getX();
                double dy = (p[1] + 0.05D) - loc.getY();
                double dz = (p[2] + 0.5D) - loc.getZ();
                double d = dx * dx + dy * dy + dz * dz;
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
            return best;
        }

        private Boolean doubled(JsonObject lict, Integer org, String tag) {
            JsonArray lines = lict.get("line").getAsJsonArray();
            for (Integer i = 0; i < lines.size(); i++) {
                JsonObject jso = lines.get(i).getAsJsonObject();
                if (jso.get("organizer").getAsInt() == org && jso.get("id").getAsString().equals(tag)) {
                    return true;
                }
            }
            return false;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getView().getTitle().contains("控制台")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true); // 全！部！锁！定！
                switch (slot) {
                    case 2:
                        CheckGroup(event, 1, true);
                        break;
                    case 3:
                        if (cfg.getBoolean("options.private.enable") || player.isOp()) {
                            // 前置判断：已拥有组织则禁止再创建
                            Gson gson = new Gson();
                            JsonElement group_root = null;
                            try (Reader reader = loadGroupReader()) {
                                group_root = gson.fromJson(reader, JsonElement.class);
                            } catch (IOException e) {
                                throw new RuntimeException("无法加载配置文件", e);
                            }
                            JsonObject group_json = group_root.getAsJsonObject();
                            JsonArray groups = group_json.getAsJsonArray("group");
                            boolean owned = false;
                            for (int i = 0; i < groups.size(); i++) {
                                if (groups.get(i).getAsJsonObject().get("owner").getAsString()
                                        .equals(player.getDisplayName())) {
                                    owned = true;
                                    break;
                                }
                            }
                            if (owned) {
                                player.sendMessage("§c你已拥有组织，不能再创建新的组织");
                                break;
                            }
                            MakeGroup(player);
                        }
                        break;
                    case 4:
                        GroupSettings(player);
                        break;
                    case 5:
                        player.sendMessage("https://yxy317coder.github.io/others/Wiki/RailTransit/Map?ip="
                                + Bukkit.getServer().getIp() + ":" + Bukkit.getPort());
                        break; // 网站暂未完成，考虑使用模组或该网址来显示地图，纯插件无法显示地图
                    case 6:
                        PlanRailway(player);
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("查询组织")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true); // 全！部！锁！定！
                switch (slot) {
                    case 18:
                        CheckGroup(event, -1, false);
                        break;
                    case 26:
                        CheckGroup(event, 1, false);
                        break;
                    default:
                        if (event.getView().getTopInventory()
                                .getItem(slot) != new ItemStack(Material.GRAY_STAINED_GLASS_PANE)) {
                            GroupInfo(event,
                                    ((Integer.valueOf(event.getView().getTitle().split(" - ")[1]) - 1) * 18 + slot), 1,
                                    true);
                        }
                        break;
                }
            } else if (event.getView().getTitle().contains("组织信息")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true); // 全！部！锁！定！
                switch (slot) {
                    case 18:
                        GroupInfo(event, Integer.valueOf(event.getView().getTitle().split(" - ")[1]), -1, false);
                        break;
                    case 26:
                        GroupInfo(event, Integer.valueOf(event.getView().getTitle().split(" - ")[1]), 1, false);
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("选择组织图标")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true); // 全！部！锁！定！
                String target_icon = icon_base64[slot];
                player.closeInventory();
                // 询问组织名称与一句话描述（Ask 会阻塞，必须在异步线程调用）
                Bukkit.getScheduler().runTaskAsynchronously(getAddon().getJavaPlugin(), () -> {
                    String groupName = chatPrompter.Ask(player, "§6请给你的组织起个名字：");
                    if (groupName == null) {
                        player.sendMessage("§c已取消创建组织");
                        return;
                    }
                    String groupInfo = chatPrompter.Ask(player, "§6请用一句话描述你的组织：");
                    if (groupInfo == null) {
                        player.sendMessage("§c已取消创建组织");
                        return;
                    }
                    Gson gson = new GsonBuilder().setPrettyPrinting().create(); // 格式化输出
                    JsonElement group_root = null;
                    try (Reader reader = loadGroupReader()) {
                        group_root = gson.fromJson(reader, JsonElement.class);
                    } catch (IOException e) {
                        throw new RuntimeException("无法加载配置文件", e);
                    }
                    JsonObject group_json = group_root.getAsJsonObject();
                    JsonObject new_group = new JsonObject();
                    JsonArray memberArray = new JsonArray();
                    memberArray.add(player.getDisplayName());
                    new_group.addProperty("name", groupName);
                    new_group.addProperty("info", groupInfo);
                    new_group.addProperty("owner", player.getDisplayName());
                    new_group.addProperty("icon", target_icon);
                    new_group.add("member", memberArray);
                    group_json.getAsJsonArray("group").add(new_group);
                    String jsonString = gson.toJson(group_json);
                    File dataFolder = getAddon().getJavaPlugin().getDataFolder();
                    if (!dataFolder.exists()) {
                        dataFolder.mkdirs();
                    }
                    try (FileWriter writer = new FileWriter(new File(dataFolder, "group.json"))) {
                        writer.write(jsonString);
                        Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                            player.sendMessage("§a组织 §f" + groupName + " §a创建成功");
                        });
                    } catch (IOException e) {
                        getLogger().severe(e.getLocalizedMessage());
                        Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                            player.sendMessage("§a组织 §f" + groupName + " §a创建失败");
                        });
                    }
                });
            } else if (event.getView().getTitle().contains("组织设置")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true); // 全！部！锁！定！
                switch (slot) {
                    case 2:
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskAsynchronously(getAddon().getJavaPlugin(), () -> {
                            String groupInfo = chatPrompter.Ask(player, "§6重新用一句话描述你的组织：");
                            if (groupInfo == null) {
                                player.sendMessage("§c已取消覆写组织");
                                return;
                            }
                            Gson gson = new GsonBuilder().setPrettyPrinting().create(); // 格式化输出
                            JsonElement group_root = null;
                            try (Reader reader = loadGroupReader()) {
                                group_root = gson.fromJson(reader, JsonElement.class);
                            } catch (IOException e) {
                                throw new RuntimeException("无法加载配置文件", e);
                            }
                            JsonObject group_json = group_root.getAsJsonObject();
                            JsonArray groups = group_json.getAsJsonArray("group");
                            Integer id = -1;
                            for (int i = 0; i < groups.size(); i++) {
                                if (groups.get(i).getAsJsonObject().get("owner").getAsString()
                                        .equals(player.getDisplayName())) {
                                    id = i;
                                    break;
                                }
                            }
                            if (id == -1) {
                                player.sendMessage("§c你不是任何组织的创建者，无法修改介绍");
                                return;
                            }
                            groups.get(id).getAsJsonObject().addProperty("info", groupInfo);
                            String jsonString = gson.toJson(group_json);
                            File dataFolder = getAddon().getJavaPlugin().getDataFolder();
                            if (!dataFolder.exists()) {
                                dataFolder.mkdirs();
                            }
                            try (FileWriter writer = new FileWriter(new File(dataFolder, "group.json"))) {
                                writer.write(jsonString);
                                Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                                    player.sendMessage("§a组织介绍覆写成功");
                                });
                            } catch (IOException e) {
                                getLogger().severe(e.getLocalizedMessage());
                                Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                                    player.sendMessage("§a组织介绍覆写失败");
                                });
                            }
                        });
                        break;
                    case 3:
                        player.closeInventory();
                        // 邀请成员：询问要邀请的玩家名称，在线则询问其是否同意，同意则加入 member 列表
                        Bukkit.getScheduler().runTaskAsynchronously(getAddon().getJavaPlugin(), () -> {
                            String targetName = chatPrompter.Ask(player, "§6请输入要邀请的玩家名称：");
                            if (targetName == null) {
                                player.sendMessage("§c已取消邀请");
                                return;
                            }
                            Player target = Bukkit.getServer().getPlayer(targetName);
                            if (target == null) {
                                player.sendMessage("§c玩家 §f" + targetName + " §c不在线，无法邀请");
                                return;
                            }
                            if (target.getUniqueId().equals(player.getUniqueId())) {
                                player.sendMessage("§c不能邀请自己");
                                return;
                            }
                            Gson gson = new GsonBuilder().setPrettyPrinting().create(); // 格式化输出
                            JsonElement group_root = null;
                            try (Reader reader = loadGroupReader()) {
                                group_root = gson.fromJson(reader, JsonElement.class);
                            } catch (IOException e) {
                                throw new RuntimeException("无法加载配置文件", e);
                            }
                            JsonObject group_json = group_root.getAsJsonObject();
                            JsonArray groups = group_json.getAsJsonArray("group");
                            Integer id = -1;
                            for (int i = 0; i < groups.size(); i++) {
                                if (groups.get(i).getAsJsonObject().get("owner").getAsString()
                                        .equals(player.getDisplayName())) {
                                    id = i;
                                    break;
                                }
                            }
                            if (id == -1) {
                                player.sendMessage("§c你不是任何组织的创建者，无法邀请成员");
                                return;
                            }
                            String groupName = groups.get(id).getAsJsonObject().get("name").getAsString();
                            // 检查是否已在组织中
                            JsonArray memberArray = groups.get(id).getAsJsonObject().getAsJsonArray("member");
                            for (int i = 0; i < memberArray.size(); i++) {
                                if (memberArray.get(i).getAsString().equals(target.getDisplayName())) {
                                    player.sendMessage("§c玩家 §f" + target.getDisplayName() + " §c已在组织中");
                                    return;
                                }
                            }
                            // 询问被邀请玩家（输入 cancel 或超时即拒绝）
                            String reply = chatPrompter.Ask(target,
                                    "§6玩家 §f" + player.getDisplayName() + " §6邀请你加入组织 §f" + groupName
                                            + "§6，输入任意内容同意加入，输入 cancel 拒绝");
                            if (reply == null) {
                                player.sendMessage("§c玩家 §f" + target.getDisplayName() + " §c拒绝了邀请或超时未回复");
                                return;
                            }
                            // 同意：加入 member 列表并保存
                            memberArray.add(target.getDisplayName());
                            String jsonString = gson.toJson(group_json);
                            File dataFolder = getAddon().getJavaPlugin().getDataFolder();
                            if (!dataFolder.exists()) {
                                dataFolder.mkdirs();
                            }
                            try (FileWriter writer = new FileWriter(new File(dataFolder, "group.json"))) {
                                writer.write(jsonString);
                                Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                                    player.sendMessage("§a玩家 §f" + target.getDisplayName() + " §a已加入组织 §f" + groupName);
                                    target.sendMessage("§a你已加入组织 §f" + groupName);
                                });
                            } catch (IOException e) {
                                getLogger().severe(e.getLocalizedMessage());
                                Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                                    player.sendMessage("§c邀请保存失败");
                                });
                            }
                        });
                        break;
                    case 4:
                        ManageMember(event, 1, true);
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("管理成员")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true); // 全！部！锁！定！
                switch (slot) {
                    case 18:
                        ManageMember(event, -1, false);
                        break;
                    case 26:
                        ManageMember(event, 1, false);
                        break;
                    default:
                        Player who = (Player) event.getWhoClicked();
                        Gson gson = new Gson();
                        JsonElement group_root = null;
                        try (Reader reader = loadGroupReader()) {
                            group_root = gson.fromJson(reader, JsonElement.class);
                        } catch (IOException e) {
                            throw new RuntimeException("无法加载配置文件", e);
                        }
                        JsonObject group_json = group_root.getAsJsonObject();
                        JsonArray groups = group_json.getAsJsonArray("group");
                        Integer id = -1;
                        for (int i = 0; i < groups.size(); i++) {
                            if (groups.get(i).getAsJsonObject().get("owner").getAsString()
                                    .equals(who.getDisplayName())) {
                                id = i;
                                break;
                            }
                        }
                        if (id == -1) {
                            who.closeInventory();
                            who.sendMessage("§c你不是任何组织的创建者，无法管理成员");
                            return;
                        }
                        if (slot < 18 && (!event.getView().getTopInventory().getItem(slot).getType()
                                .equals(Material.GRAY_STAINED_GLASS_PANE))) {
                            DoPlayer(id,
                                    ((Integer.valueOf(event.getView().getTitle().split(" - ")[1]) - 1) * 18 + slot),
                                    who);
                        }
                        break;
                }
            } else if (event.getView().getTitle().contains("玩家")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true); // 全！部！锁！定！
                if (slot != 3 && slot != 5) {
                    return;
                }
                Integer who = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                Gson gson = new GsonBuilder().setPrettyPrinting().create(); // 格式化输出
                JsonElement group_root = null;
                try (Reader reader = loadGroupReader()) {
                    group_root = gson.fromJson(reader, JsonElement.class);
                } catch (IOException e) {
                    throw new RuntimeException("无法加载配置文件", e);
                }
                JsonObject group_json = group_root.getAsJsonObject();
                JsonArray groups = group_json.getAsJsonArray("group");
                Integer id = -1;
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).getAsJsonObject().get("owner").getAsString().equals(player.getDisplayName())) {
                        id = i;
                        break;
                    }
                }
                if (id == -1) {
                    player.closeInventory();
                    player.sendMessage("§c你不是任何组织的创建者，无法管理成员");
                    return;
                }
                JsonObject group = groups.get(id).getAsJsonObject();
                JsonArray memberArray = group.getAsJsonArray("member");
                if (who < 0 || who >= memberArray.size()) {
                    return;
                }
                String targetName = memberArray.get(who).getAsString();
                if (slot == 3) {
                    // 组织转让：owner 改为目标成员
                    if (targetName.equals(group.get("owner").getAsString())) {
                        player.sendMessage("§c对方已是组织创建者，无需转让");
                        return;
                    }
                    group.addProperty("owner", targetName);
                } else if (slot == 5) {
                    // 踢出成员
                    if (targetName.equals(group.get("owner").getAsString())) {
                        player.sendMessage("§c不能踢出组织创建者");
                        return;
                    }
                    memberArray.remove(who);
                }
                String jsonString = gson.toJson(group_json);
                File dataFolder = getAddon().getJavaPlugin().getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                try (FileWriter writer = new FileWriter(new File(dataFolder, "group.json"))) {
                    writer.write(jsonString);
                    if (slot == 3) {
                        player.sendMessage("§a组织已转让给 §f" + targetName);
                    } else if (slot == 5) {
                        player.sendMessage("§a已踢出成员 §f" + targetName);
                    }
                    player.closeInventory();
                } catch (IOException e) {
                    getLogger().severe(e.getLocalizedMessage());
                    player.sendMessage("§c操作保存失败");
                }
            } else if (event.getView().getTitle().contains("规划铁路")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                switch (slot) {
                    case 3:
                        player.closeInventory();
                        waitingAddStation.add(player.getName());
                        player.sendMessage("§6请空手右键一个站停铁轨（激活铁轨），输入 §ccancel§7 可取消");
                        break;
                    case 4:
                        player.closeInventory();
                        List<Integer> joined = findJoinedGroups(player);
                        if (joined.isEmpty()) {
                            player.sendMessage("§c你还未加入任何组织");
                        } else {
                            Gson gson = new Gson();
                            JsonElement group_root = null;
                            try (Reader reader = loadGroupReader()) {
                                group_root = gson.fromJson(reader, JsonElement.class);
                            } catch (IOException e) {
                                throw new RuntimeException("无法加载配置文件", e);
                            }
                            JsonArray groups = group_root.getAsJsonObject().getAsJsonArray("group");
                            StringBuilder sb = new StringBuilder();
                            for (Integer org : joined) {
                                sb.append("§f").append(groups.get(org).getAsJsonObject().get("name").getAsString())
                                        .append("§7(#").append(org).append(") ");
                            }
                            player.sendMessage("§6你已加入 " + joined.size() + " 个组织：" + sb.toString());
                        }
                        break;
                    case 5:
                        LineSelect(event, 1, true);
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("线路选择")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                switch (slot) {
                    case 18:
                        LineSelect(event, -1, false);
                        break;
                    case 26:
                        LineSelect(event, 1, false);
                        break;
                    default:
                        if (slot < 18 && event.getView().getTopInventory().getItem(slot) != null
                                && !event.getView().getTopInventory().getItem(slot).getType()
                                        .equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            int page = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                            int orgIndex = findJoinedGroups(player).get((page - 1) * 18 + slot);
                            LinePanel(player, orgIndex);
                        }
                        break;
                }
            } else if (event.getView().getTitle().contains("线路面板")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                switch (slot) {
                    case 3:
                        EditLineSelect(event, orgIndex, 1, true);
                        break;
                    case 5:
                        player.closeInventory();
                        // 添加线路：先问编号（不能重复，可为字母），再问名称
                        Bukkit.getScheduler().runTaskAsynchronously(getAddon().getJavaPlugin(), () -> {
                            String lineId = chatPrompter.Ask(player, "§6请输入线路编号（不能重复，可以是字母）：");
                            if (lineId == null) {
                                player.sendMessage("§c已取消添加线路");
                                return;
                            }
                            JsonObject station_json = loadStationJson();
                            JsonArray lines = station_json.getAsJsonArray("line");
                            if (doubled(station_json, orgIndex, lineId)) {
                                player.sendMessage("§c线路编号 §f" + lineId + " §c已存在，请勿重复");
                                return;
                            }
                            String lineName = chatPrompter.Ask(player, "§6请输入线路名称：");
                            if (lineName == null) {
                                player.sendMessage("§c已取消添加线路");
                                return;
                            }
                            JsonObject newLine = new JsonObject();
                            newLine.addProperty("organizer", orgIndex);
                            newLine.addProperty("id", lineId);
                            newLine.addProperty("name", lineName);
                            newLine.add("station", new JsonArray());
                            newLine.addProperty("back", true);
                            newLine.addProperty("out", 5); // 回车厂后再次发车时间（秒）
                            newLine.addProperty("wait", 2); // 每站停留时间（秒）
                            newLine.add("compile", new JsonArray()); // 默认空数组=未编译
                            lines.add(newLine);
                            saveStation(station_json);
                            Bukkit.getScheduler().runTask(getAddon().getJavaPlugin(), () -> {
                                player.sendMessage("§a线路 §f" + lineName + " §a添加成功");
                            });
                        });
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("编辑已有线路")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                switch (slot) {
                    case 18:
                        EditLineSelect(event, orgIndex, -1, false);
                        break;
                    case 26:
                        EditLineSelect(event, orgIndex, 1, false);
                        break;
                    default:
                        if (slot < 18 && event.getView().getTopInventory().getItem(slot) != null
                                && !event.getView().getTopInventory().getItem(slot).getType()
                                        .equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            int page = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                            JsonObject station_json = loadStationJson();
                            int lineIndex = findOrgLines(station_json, orgIndex).get((page - 1) * 18 + slot);
                            LineEdit(player, orgIndex, lineIndex);
                        }
                        break;
                }
            } else if (event.getView().getTitle().contains("线路编辑")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                int lineIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                switch (slot) {
                    case 3:
                        StationEdit(player, orgIndex, lineIndex);
                        break;
                    case 4:
                        CompilePanel(player, orgIndex, lineIndex);
                        break;
                    case 5:
                        LineListDelete(event, orgIndex, 1, true);
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("站台编辑")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                int lineIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                switch (slot) {
                    case 3:
                        StationListAdd(event, orgIndex, lineIndex, 1, true);
                        break;
                    case 4:
                        StationListPreview(event, orgIndex, lineIndex, 1, true);
                        break;
                    case 5:
                        StationListRemove(event, orgIndex, lineIndex, 1, true);
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("编译界面")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                int lineIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                switch (slot) {
                    case 3: // 是否回厂：切换
                        JsonObject station_json = loadStationJson();
                        JsonArray lines = station_json.getAsJsonArray("line");
                        if (lineIndex < 0 || lineIndex >= lines.size()) {
                            player.sendMessage("§c线路不存在");
                            break;
                        }
                        JsonObject line = lines.get(lineIndex).getAsJsonObject();
                        boolean newBack = !(line.has("back") && line.get("back").getAsBoolean());
                        line.addProperty("back", newBack);
                        line.add("compile", new JsonArray()); // 回厂方式改变，原编译结果失效
                        saveStation(station_json);
                        player.sendMessage("§a线路已切换为" + (newBack ? "§a回厂（单行线）" : "§c环线（不回厂）"));
                        CompilePanel(player, orgIndex, lineIndex); // 刷新界面
                        break;
                    case 4: // 重新编译
                        player.closeInventory();
                        waitingCompile.put(player.getName(), new int[] { orgIndex, lineIndex });
                        player.sendMessage("§6请空手右键一个侧线铁轨作为该线路起点，输入 §ccancel§7 可取消");
                        break;
                    case 5: // 编译结果
                        JsonObject station_json5 = loadStationJson();
                        JsonArray lines5 = station_json5.getAsJsonArray("line");
                        if (lineIndex < 0 || lineIndex >= lines5.size()) {
                            player.sendMessage("§c线路不存在");
                            break;
                        }
                        boolean compiled = isCompiled(lines5.get(lineIndex).getAsJsonObject());
                        player.sendMessage(compiled ? "§a编译结果：已编译，可以运行" : "§c编译结果：未编译，请先重新编译");
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("添加站台")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                int lineIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                switch (slot) {
                    case 18:
                        StationListAdd(event, orgIndex, lineIndex, -1, false);
                        break;
                    case 26:
                        StationListAdd(event, orgIndex, lineIndex, 1, false);
                        break;
                    default:
                        if (slot < 18 && event.getView().getTopInventory().getItem(slot) != null
                                && !event.getView().getTopInventory().getItem(slot).getType()
                                        .equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            JsonObject station_json = loadStationJson();
                            JsonArray platforms = station_json.getAsJsonArray("platform");
                            JsonArray stations = station_json.getAsJsonArray("line").get(lineIndex).getAsJsonObject()
                                    .getAsJsonArray("station");
                            int page = Integer.valueOf(event.getView().getTitle().split(" - ")[3]);
                            int target = (page - 1) * 18 + slot;
                            if (target >= platforms.size()) {
                                return;
                            }
                            // boolean has = false;
                            // for (int j = 0; j < stations.size(); j++) {
                            // if (stations.get(j).getAsInt() == target) {
                            // has = true;
                            // break;
                            // }
                            // }
                            // if (has) {
                            // player.sendMessage("§c站台已在当前线路中");
                            // return;
                            // }
                            stations.add(target);
                            // 路线站点变化，编译结果失效
                            station_json.getAsJsonArray("line").get(lineIndex).getAsJsonObject()
                                    .add("compile", new JsonArray());
                            saveStation(station_json);
                            player.sendMessage("§a站台 §f"
                                    + platforms.get(target).getAsJsonObject().get("name").getAsString()
                                    + " §a已添加到线路");
                        }
                        break;
                }
            } else if (event.getView().getTitle().contains("预览站台")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                int lineIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                switch (slot) {
                    case 18:
                        StationListPreview(event, orgIndex, lineIndex, -1, false);
                        break;
                    case 26:
                        StationListPreview(event, orgIndex, lineIndex, 1, false);
                        break;
                    default:
                        break;
                }
            } else if (event.getView().getTitle().contains("删除站台")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                int lineIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                switch (slot) {
                    case 18:
                        StationListRemove(event, orgIndex, lineIndex, -1, false);
                        break;
                    case 26:
                        StationListRemove(event, orgIndex, lineIndex, 1, false);
                        break;
                    default:
                        if (slot < 18 && event.getView().getTopInventory().getItem(slot) != null
                                && !event.getView().getTopInventory().getItem(slot).getType()
                                        .equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            JsonObject station_json = loadStationJson();
                            JsonArray stations = station_json.getAsJsonArray("line").get(lineIndex).getAsJsonObject()
                                    .getAsJsonArray("station");
                            int page = Integer.valueOf(event.getView().getTitle().split(" - ")[3]);
                            int target = (page - 1) * 18 + slot;
                            if (target >= stations.size()) {
                                return;
                            }
                            int platformIndex = stations.get(target).getAsInt();
                            stations.remove(target);
                            // 路线站点变化，编译结果失效
                            station_json.getAsJsonArray("line").get(lineIndex).getAsJsonObject()
                                    .add("compile", new JsonArray());
                            saveStation(station_json);
                            player.sendMessage("§a已从线路中移除站台 #" + platformIndex);
                        }
                        break;
                }
            } else if (event.getView().getTitle().contains("删除线路")) {
                if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int slot = event.getRawSlot();
                event.setCancelled(true);
                int orgIndex = Integer.valueOf(event.getView().getTitle().split(" - ")[1]);
                switch (slot) {
                    case 18:
                        LineListDelete(event, orgIndex, -1, false);
                        break;
                    case 26:
                        LineListDelete(event, orgIndex, 1, false);
                        break;
                    default:
                        if (slot < 18 && event.getView().getTopInventory().getItem(slot) != null
                                && !event.getView().getTopInventory().getItem(slot).getType()
                                        .equals(Material.GRAY_STAINED_GLASS_PANE)) {
                            JsonObject station_json = loadStationJson();
                            JsonArray lines = station_json.getAsJsonArray("line");
                            int page = Integer.valueOf(event.getView().getTitle().split(" - ")[2]);
                            int lineIndex = findOrgLines(station_json, orgIndex).get((page - 1) * 18 + slot);
                            String lineName = lines.get(lineIndex).getAsJsonObject().get("name").getAsString();
                            lines.remove(lineIndex);
                            saveStation(station_json);
                            player.sendMessage("§a线路 §f" + lineName + " §a已删除");
                        }
                        break;
                }
            }
        }
    }

    /**
     * /railtransfer select [组织id] [线路id]：玩家选中线路，之后在该线路起点侧线放置列车即可自动运行。
     */
    public void selectLine(Player player, int orgId, String lineId) {
        controller.selectLine(player, orgId, lineId);
    }

    /**
     * /railtransfer out [组织id] [线路id]：调整回车厂后再次发车时间（秒）。
     */
    public void setLineOut(Player player, int orgId, String lineId) {
        controller.setLineTime(player, orgId, lineId, "out");
    }

    /**
     * /railtransfer wait [组织id] [线路id]：调整每站停留时间（秒）。
     */
    public void setLineWait(Player player, int orgId, String lineId) {
        controller.setLineTime(player, orgId, lineId, "wait");
    }

    /**
     * /railtransfer compile [组织id] [线路id]：进入编译等待状态，等待玩家空手右键侧线铁轨作为线路起点。
     */
    public void compileLine(Player player, int orgId, String lineId) {
        controller.compileLine(player, orgId, lineId);
    }

    /**
     * /railtransfer continue [组织id] [线路id]：基于上次编译的起点侧线继续编译，玩家无需返回侧线铁轨。
     * orgId 传 -1 表示无参，使用该玩家最近一次右键侧线编译的线路。
     */
    public void continueCompile(Player player, int orgId, String lineId) {
        controller.continueCompile(player, orgId, lineId);
    }

    @Override
    public String getBugTrackerURL() {
        // 你可以在这里返回你的问题追踪器的网址，而不是 null
        return null;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        /*
         * 你需要返回对你插件的引用。
         * 如果这是你插件的主类，只需要返回 "this" 即可。
         */
        return this;
    }

}
