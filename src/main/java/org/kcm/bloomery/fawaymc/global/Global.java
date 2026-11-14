package org.kcm.bloomery.fawaymc.global;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Global extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private Location spawnLocation;
    private BossbarManager bossBarManager;
    private PlayerItemsManager playerItemsManager;
    private ItemsConfigManager itemsConfigManager;
    private PlayerSettingsManager playerSettingsManager;
    private DoubleJumpManager doubleJumpManager;
    private WorldRestrictionManager worldRestrictionManager;
    private ScoreboardManager scoreboardManager;
    private TablistManager tablistManager;
    private MotdManager motdManager;

    private File customConfigFile;
    private FileConfiguration customConfig;

    private final Map<UUID, Integer> playerCoins = new HashMap<>();
    private final Map<UUID, Integer> playerKills = new HashMap<>();
    private final Map<UUID, Integer> playerDeaths = new HashMap<>();
    private final Set<UUID> bypassBuilders = new HashSet<>();
    private final Set<UUID> adminModePlayers = new HashSet<>();

    private final Map<UUID, Long> blockMsgCooldowns = new HashMap<>();

    private int buildActionBarTaskId = -1;
    private int itemCheckTaskId = -1;

    @EventHandler
    public void onWorldLoad(org.bukkit.event.world.WorldLoadEvent event) {
        if (spawnLocation == null) {
            loadSpawnLocation();
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        handlePlayerItems(event.getPlayer());
    }

    @Override
    public void onEnable() {
        createCustomConfig();
        loadSpawnLocation();

        playerSettingsManager = new PlayerSettingsManager(this);
        itemsConfigManager = new ItemsConfigManager(this);
        playerItemsManager = new PlayerItemsManager(this, itemsConfigManager);
        doubleJumpManager = new DoubleJumpManager(this, playerSettingsManager);

        scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.start();
        tablistManager = new TablistManager(this);
        tablistManager.start();
        bossBarManager = new BossbarManager(this);
        worldRestrictionManager = new WorldRestrictionManager(this);
        motdManager = new MotdManager(this);

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("setspawn") != null) getCommand("setspawn").setExecutor(this);
        if (getCommand("spawn") != null) getCommand("spawn").setExecutor(this);
        if (getCommand("globalreload") != null) getCommand("globalreload").setExecutor(this);
        if (getCommand("menu") != null) getCommand("menu").setExecutor(this);
        if (getCommand("profile") != null) getCommand("profile").setExecutor(this);
        if (getCommand("build") != null) { getCommand("build").setExecutor(this); getCommand("build").setTabCompleter(this); }
        if (getCommand("adminmode") != null) { getCommand("adminmode").setExecutor(this); getCommand("adminmode").setTabCompleter(this); }
        if (getCommand("worldrestriction") != null) { getCommand("worldrestriction").setExecutor(this); getCommand("worldrestriction").setTabCompleter(this); }

        startBuildActionBarTask();
        startItemCheckTask();

        getLogger().info(ChatColor.GREEN + "Global Plugin เปิดใช้งานสำเร็จแล้ว!");
    }

    @Override
    public void onDisable() {
        if (buildActionBarTaskId != -1) {
            Bukkit.getScheduler().cancelTask(buildActionBarTaskId);
            buildActionBarTaskId = -1;
        }
        if (itemCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(itemCheckTaskId);
            itemCheckTaskId = -1;
        }

        if (bossBarManager != null) {
            bossBarManager.stop();
        }

        if (doubleJumpManager != null) {
            doubleJumpManager.stop();
        }

        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }

        getLogger().info(ChatColor.RED + "Global Plugin ถูกปิดใช้งานเรียบร้อยแล้ว");
    }

    private void createCustomConfig() {
        customConfigFile = new File(getDataFolder(), "global.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("global.yml", false);
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    }

    @Override
    public FileConfiguration getConfig() {
        if (customConfig == null) reloadGlobalConfig();
        return customConfig;
    }

    public void reloadGlobalConfig() {
        if (customConfigFile == null) customConfigFile = new File(getDataFolder(), "global.yml");
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
        try (InputStreamReader defConfigStream = new InputStreamReader(getResource("global.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            customConfig.setDefaults(defConfig);
        } catch (Exception ignored) {}
    }

    public void saveGlobalConfig() {
        if (customConfig == null || customConfigFile == null) {
            getLogger().warning("ไม่สามารถบันทึก global.yml ได้: config ยังไม่ถูกโหลด");
            return;
        }
        try {
            File tempFile = new File(customConfigFile.getParentFile(), customConfigFile.getName() + ".tmp");
            customConfig.save(tempFile);
            java.nio.file.Files.move(
                    tempFile.toPath(),
                    customConfigFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            getLogger().severe("ไม่สามารถบันทึกไฟล์ global.yml ได้: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getMsg(String path) {
        String msg = getConfig().getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String getGuiMsg(String path) {
        String msg = itemsConfigManager.getConfig().getString("gui.messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private List<String> getMsgList(String path) {
        List<String> list = getConfig().getStringList("messages." + path);
        list.replaceAll(s -> ChatColor.translateAlternateColorCodes('&', s));
        return list;
    }

    private void loadSpawnLocation() {
        FileConfiguration config = getConfig();

        if (!config.contains("spawn.world")) {
            spawnLocation = null;
            return;
        }

        String worldName = config.getString("spawn.world");
        if (worldName == null) {
            spawnLocation = null;
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().warning("ไม่พบโลก '" + worldName + "' ที่บันทึกไว้ใน global.yml (world อาจยังไม่โหลด)");
            spawnLocation = null;
            return;
        }

        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw");
        float pitch = (float) config.getDouble("spawn.pitch");

        spawnLocation = new Location(world, x, y, z, yaw, pitch);
    }

    private void saveSpawnLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            getLogger().warning("ไม่สามารถบันทึก spawn ได้: Location หรือ World เป็น null");
            return;
        }

        this.spawnLocation = loc;

        FileConfiguration config = getConfig();
        config.set("spawn.world", loc.getWorld().getName());
        config.set("spawn.x", loc.getX());
        config.set("spawn.y", loc.getY());
        config.set("spawn.z", loc.getZ());
        config.set("spawn.yaw", loc.getYaw());
        config.set("spawn.pitch", loc.getPitch());

        saveGlobalConfig();
    }

    private void sendBlockWarningMessage(Player player, String msgKey) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMs = getConfig().getLong("admin.block_warning_cooldown_ms", 2000L);

        if (!blockMsgCooldowns.containsKey(uuid) || (now - blockMsgCooldowns.get(uuid)) >= cooldownMs) {
            blockMsgCooldowns.put(uuid, now);
            player.sendMessage(getMsg(msgKey));
        }
    }

    // ===== Admin Mode Actionbar =====

    private void startBuildActionBarTask() {
        if (buildActionBarTaskId != -1) {
            Bukkit.getScheduler().cancelTask(buildActionBarTaskId);
            buildActionBarTaskId = -1;
        }
        buildActionBarTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (adminModePlayers.isEmpty()) return;

            String rawOn = getConfig().getString("messages.admin.build_actionbar_on");
            if (rawOn == null || rawOn.isEmpty()) rawOn = "&a&lBuild Mode: ON";
            Component onComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(rawOn);

            String rawOff = getConfig().getString("messages.admin.build_actionbar_off");
            if (rawOff == null || rawOff.isEmpty()) rawOff = "&c&lBuild Mode: OFF";
            Component offComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(rawOff);

            for (UUID uuid : new HashSet<>(adminModePlayers)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendActionBar(bypassBuilders.contains(uuid) ? onComponent : offComponent);
                }
            }
        }, 0L, 20L).getTaskId();
    }

    private void clearBuildActionBar(Player player) {
        player.sendActionBar(Component.empty());
    }

    // ===== Item Auto-Refill / Auto-Remove ตาม world =====

    private void startItemCheckTask() {
        if (itemCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(itemCheckTaskId);
            itemCheckTaskId = -1;
        }
        itemCheckTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                handlePlayerItems(p);
            }
        }, 0L, 20L).getTaskId();
    }

    /**
     * ถ้าอยู่ใน world spawn → ตรวจ item หายแล้วเติมกลับ
     * ถ้าอยู่นอก world spawn → ลบ item พิเศษออก
     */
    private void handlePlayerItems(Player player) {
        boolean inSpawnWorld = spawnLocation != null && player.getWorld().equals(spawnLocation.getWorld());
        FileConfiguration itemsCfg = itemsConfigManager.getConfig();

        if (inSpawnWorld) {
            boolean needsRefill = false;

            if (itemsCfg.getBoolean("gui.open_item.enabled", true)) {
                int slot = itemsCfg.getInt("gui.open_item.slot", 4);
                if (!playerItemsManager.isSelectorItem(player.getInventory().getItem(slot))) {
                    needsRefill = true;
                }
            }
            if (itemsCfg.getBoolean("profile.item.enabled", true)) {
                int slot = itemsCfg.getInt("profile.item.slot", 0);
                if (!playerItemsManager.isProfileItem(player.getInventory().getItem(slot))) {
                    needsRefill = true;
                }
            }
            if (itemsCfg.getBoolean("visibility.item.enabled", true)) {
                int slot = itemsCfg.getInt("visibility.item.slot", 8);
                if (!playerItemsManager.isVisibilityItem(player.getInventory().getItem(slot))) {
                    needsRefill = true;
                }
            }

            if (needsRefill) {
                playerItemsManager.givePlayerItems(player);
            }
        } else {
            boolean removed = false;
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && playerItemsManager.isAnySpecialItem(item)) {
                    player.getInventory().setItem(i, null);
                    removed = true;
                }
            }
            if (removed) {
                player.updateInventory();
            }
        }
    }

    private void openGameMenu(Player player) {
        FileConfiguration itemsCfg = itemsConfigManager.getConfig();

        String title = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("gui.menu_title", "&8Game Menu"));
        int size = itemsCfg.getInt("gui.size", 27);
        Inventory menu = Bukkit.createInventory(null, size, title);
        ConfigurationSection itemsSection = itemsCfg.getConfigurationSection("gui.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                int slot = itemsCfg.getInt("gui.items." + key + ".slot", 0);
                String matName = itemsCfg.getString("gui.items." + key + ".material", "STONE");
                String name = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("gui.items." + key + ".name", ""));
                List<String> rawLore = itemsCfg.getStringList("gui.items." + key + ".lore");
                List<String> lore = new ArrayList<>();
                for (String l : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', l));
                Material material = Material.matchMaterial(matName);
                if (material == null) material = Material.STONE;
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                menu.setItem(slot, item);
            }
        }
        player.openInventory(menu);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private void openProfileMenu(Player player) {
        FileConfiguration itemsCfg = itemsConfigManager.getConfig();

        String title = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("profile.gui.title", "&8Player Profile"));
        int size = itemsCfg.getInt("profile.gui.size", 27);
        Inventory profileMenu = Bukkit.createInventory(null, size, title);
        int coins = playerCoins.getOrDefault(player.getUniqueId(), 100);
        int kills = playerKills.getOrDefault(player.getUniqueId(), 0);
        int deaths = playerDeaths.getOrDefault(player.getUniqueId(), 0);

        ConfigurationSection itemsSection = itemsCfg.getConfigurationSection("profile.gui.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (key.equalsIgnoreCase("double_jump_toggle")) continue;

                int slot = itemsCfg.getInt("profile.gui.items." + key + ".slot", 0);
                String matName = itemsCfg.getString("profile.gui.items." + key + ".material", "STONE");
                String name = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("profile.gui.items." + key + ".name", ""));
                List<String> rawLore = itemsCfg.getStringList("profile.gui.items." + key + ".lore");
                List<String> lore = new ArrayList<>();
                for (String line : rawLore) {
                    line = line.replace("{player}", player.getName())
                            .replace("{coins}", String.valueOf(coins))
                            .replace("{kills}", String.valueOf(kills))
                            .replace("{deaths}", String.valueOf(deaths));
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }

                ItemStack item;
                if (matName.equalsIgnoreCase("PLAYER_HEAD")) {
                    item = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta headMeta = (SkullMeta) item.getItemMeta();
                    if (headMeta != null) {
                        headMeta.setOwningPlayer(player);
                        headMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name.replace("{player}", player.getName())));
                        headMeta.setLore(lore);
                        item.setItemMeta(headMeta);
                    }
                } else {
                    Material material = Material.matchMaterial(matName);
                    if (material == null) material = Material.STONE;
                    item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                }
                profileMenu.setItem(slot, item);
            }
        }

        if (itemsCfg.getBoolean("profile.gui.items.double_jump_toggle.enabled", true)) {
            int slot = itemsCfg.getInt("profile.gui.items.double_jump_toggle.slot", 31);
            boolean djOn = playerSettingsManager.isDoubleJumpEnabled(player);

            String matName = djOn
                    ? itemsCfg.getString("profile.gui.items.double_jump_toggle.material_on", "FEATHER")
                    : itemsCfg.getString("profile.gui.items.double_jump_toggle.material_off", "GRAY_DYE");
            Material material = Material.matchMaterial(matName);
            if (material == null) material = djOn ? Material.FEATHER : Material.GRAY_DYE;

            String rawName = djOn
                    ? itemsCfg.getString("profile.gui.items.double_jump_toggle.name_on", "&a&lDouble Jump: ON")
                    : itemsCfg.getString("profile.gui.items.double_jump_toggle.name_off", "&c&lDouble Jump: OFF");
            String displayName = ChatColor.translateAlternateColorCodes('&', rawName);

            List<String> rawLore = djOn
                    ? itemsCfg.getStringList("profile.gui.items.double_jump_toggle.lore_on")
                    : itemsCfg.getStringList("profile.gui.items.double_jump_toggle.lore_off");
            List<String> lore = new ArrayList<>();
            for (String l : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', l));

            ItemStack djItem = new ItemStack(material);
            ItemMeta djMeta = djItem.getItemMeta();
            if (djMeta != null) {
                djMeta.setDisplayName(displayName);
                djMeta.setLore(lore);
                djItem.setItemMeta(djMeta);
            }
            profileMenu.setItem(slot, djItem);
        }

        player.openInventory(profileMenu);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private void reloadEverything() {
        reloadGlobalConfig();
        loadSpawnLocation();

        if (doubleJumpManager != null) doubleJumpManager.reloadJumpConfig();
        if (bossBarManager != null) bossBarManager.reloadBarConfig();
        if (worldRestrictionManager != null) worldRestrictionManager.reloadRestrictionConfig();
        if (motdManager != null) motdManager.reloadMotdConfig();
        if (scoreboardManager != null) { scoreboardManager.reloadScoreboardConfig(); scoreboardManager.start(); }
        if (tablistManager != null) { tablistManager.reloadTablistConfig(); tablistManager.start(); }
        if (playerSettingsManager != null) playerSettingsManager.reload();
        if (itemsConfigManager != null) itemsConfigManager.reload();

        startBuildActionBarTask();
        startItemCheckTask();

        for (Player p : Bukkit.getOnlinePlayers()) {
            handlePlayerItems(p);
            if (scoreboardManager != null) scoreboardManager.updateScoreboard(p);
            if (tablistManager != null) tablistManager.updateTablist(p);
            if (bossBarManager != null) bossBarManager.updateBossBar(p);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("globalreload")) {
            if (sender instanceof Player && !sender.hasPermission("global.admin")) {
                sender.sendMessage(getMsg("admin.no_permission"));
                return true;
            }
            reloadEverything();
            sender.sendMessage(getMsg("reload.success"));
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player)) return true;
            openGameMenu((Player) sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("profile")) {
            if (!(sender instanceof Player)) return true;
            openProfileMenu((Player) sender);
            return true;
        }

        if (command.getName().equalsIgnoreCase("build")) {
            if (!sender.hasPermission("global.admin")) {
                sender.sendMessage(getMsg("admin.no_permission"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console cannot use build command.");
                return true;
            }
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            if (args.length == 0) {
                if (bypassBuilders.contains(uuid)) {
                    bypassBuilders.remove(uuid);
                } else {
                    bypassBuilders.add(uuid);
                }
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("on")) {
                bypassBuilders.add(uuid);
            } else if (sub.equals("off")) {
                bypassBuilders.remove(uuid);
            } else {
                player.sendMessage(getMsg("admin.build_usage"));
            }
            return true;
        }

        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;   // ← ประกาศครั้งเดียว

        // ===== adminmode =====
        if (command.getName().equalsIgnoreCase("adminmode")) {
            if (!player.hasPermission("global.admin")) {
                player.sendMessage(getMsg("admin.no_permission"));
                return true;
            }
            UUID uuid = player.getUniqueId();

            if (args.length == 0) {
                if (adminModePlayers.contains(uuid)) {
                    adminModePlayers.remove(uuid);
                    clearBuildActionBar(player);
                    player.sendMessage(getMsg("admin.adminmode_disabled"));
                } else {
                    adminModePlayers.add(uuid);
                    player.sendMessage(getMsg("admin.adminmode_enabled"));
                }
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("on")) {
                adminModePlayers.add(uuid);
                player.sendMessage(getMsg("admin.adminmode_enabled"));
            } else if (sub.equals("off")) {
                adminModePlayers.remove(uuid);
                clearBuildActionBar(player);
                player.sendMessage(getMsg("admin.adminmode_disabled"));
            } else {
                player.sendMessage(getMsg("admin.adminmode_usage"));
            }
            return true;
        }

        // ===== setspawn =====
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("global.admin")) {
                player.sendMessage(getMsg("admin.no_permission"));
                return true;
            }
            saveSpawnLocation(player.getLocation());
            player.sendMessage(getMsg("spawn.set_success"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            return true;
        }

        // ===== spawn =====
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (spawnLocation == null) {
                player.sendMessage(getMsg("spawn.not_set"));
                return true;
            }
            player.teleport(spawnLocation);
            player.sendMessage(getMsg("spawn.teleport_success"));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("adminmode") && sender.hasPermission("global.admin") && args.length == 1) {
            String input = args[0].toLowerCase();
            if ("on".startsWith(input)) completions.add("on");
            if ("off".startsWith(input)) completions.add("off");
        }
        return completions;
    }

    private String getMsgForPlayer(Player player, String path) {
        String msg = getConfig().getString("messages." + path, "");
        if (msg.isEmpty()) return "";
        return PlaceholderManager.setPlaceholders(player, msg);
    }

    private List<String> getMsgListForPlayer(Player player, String path) {
        List<String> list = getConfig().getStringList("messages." + path);
        List<String> out = new ArrayList<>();
        for (String s : list) {
            out.add(PlaceholderManager.setPlaceholders(player, s));
        }
        return out;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        event.setJoinMessage(null);

        String rawJoin = getConfig().getString("messages.join.message", "");
        String joinMsg = PlaceholderManager.setPlaceholders(player, rawJoin).replace("{player}", playerName);
        if (!joinMsg.isEmpty()) {
            Bukkit.broadcastMessage(joinMsg);
        }

        String header = getMsgForPlayer(player, "join.header");
        if (!header.isEmpty()) player.sendMessage(header);
        for (String line : getMsgListForPlayer(player, "join.welcome_board")) {
            player.sendMessage(line.replace("{player}", playerName));
        }

        player.setGameMode(GameMode.SURVIVAL);
        if (spawnLocation != null) player.teleport(spawnLocation);

        playerItemsManager.onJoin(player);
        bypassBuilders.remove(player.getUniqueId());
        handlePlayerItems(player);

        if (player.hasPermission("global.admin")) { adminModePlayers.add(player.getUniqueId()); }

        if (bossBarManager != null) { bossBarManager.updateBossBar(player); }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();

        event.setQuitMessage(null);

        String rawQuit = getConfig().getString("messages.quit.message", "");
        String quitMsg = PlaceholderManager.setPlaceholders(event.getPlayer(), rawQuit).replace("{player}", playerName);
        if (!quitMsg.isEmpty()) {
            Bukkit.broadcastMessage(quitMsg);
        }

        playerItemsManager.onQuit(event.getPlayer());
        bypassBuilders.remove(event.getPlayer().getUniqueId());
        blockMsgCooldowns.remove(event.getPlayer().getUniqueId());

        adminModePlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getItemsToKeep().clear();
        event.getDrops().clear();

        Player player = event.getEntity();

        if (isInSpawnZone(player)) {
            event.getItemsToKeep().clear();
            event.getDrops().clear();
            if (spawnLocation != null) {
                player.setBedSpawnLocation(spawnLocation, true);
            }
        }

        playerDeaths.put(player.getUniqueId(), playerDeaths.getOrDefault(player.getUniqueId(), 0) + 1);
        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            playerKills.put(killer.getUniqueId(), playerKills.getOrDefault(killer.getUniqueId(), 0) + 1);
        }

        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            playerKills.put(killer.getUniqueId(), playerKills.getOrDefault(killer.getUniqueId(), 0) + 1);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);

        if (isInSpawnZone(player) && spawnLocation != null) {
            player.setGameMode(GameMode.SURVIVAL);
            event.setRespawnLocation(spawnLocation);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            handlePlayerItems(player);
            if (bossBarManager != null) {
                bossBarManager.updateBossBar(player);
            }
        }, 2L);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (playerItemsManager.isAnySpecialItem(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 1) จัดการไอเท็มพิเศษของ lobby ก่อน (selector, profile, visibility)
        if (item != null && playerItemsManager.isAnySpecialItem(item)) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                if (playerItemsManager.isSelectorItem(item)) {
                    openGameMenu(player);
                } else if (playerItemsManager.isProfileItem(item)) {
                    openProfileMenu(player);
                } else if (playerItemsManager.isVisibilityItem(item)) {
                    playerItemsManager.togglePlayerVisibility(player);
                }
            }
            return;
        }

        // 2) ตรวจสอบว่าอยู่ใน spawn zone หรือไม่
        if (!isInSpawnZone(player)) return;

        // 3) แอดมินที่เปิด /build on -> ปล่อยผ่าน
        if (player.hasPermission("global.admin") && bypassBuilders.contains(player.getUniqueId())) {
            return;
        }

        Action action = event.getAction();

        // 4) RIGHT_CLICK_BLOCK - interact กับ block (หีบ, ประตู, ปุ่ม, lever, ฯลฯ)
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);

            // เตือนเฉพาะ block ที่ interactable จริงๆ
            if (event.getClickedBlock().getType().isInteractable()) {
                if (player.hasPermission("global.admin")) {
                    sendBlockWarningMessage(player, "admin.block_interact_admin");
                } else {
                    sendBlockWarningMessage(player, "admin.block_interact_player");
                }
            }
            return;
        }

        // 5) RIGHT_CLICK_AIR - ใช้ไอเท็มในอากาศ
        if (action == Action.RIGHT_CLICK_AIR) {
            if (item != null && item.getType() != Material.AIR) {
                event.setCancelled(true);
            }
            return;
        }

        // LEFT_CLICK_BLOCK ปล่อยให้ BlockBreakEvent จัดการ (เตือนเฉพาะเมื่อ block แตกจริง)
    }

    /**
     * บล็อกการ interact กับ entity (villager, armor stand, item frame, painting, ฯลฯ)
     * PlayerInteractAtEntityEvent (armor stand) extends PlayerInteractEntityEvent จึงจับได้ทั้งหมด
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!isInSpawnZone(player)) return;

        // แอดมินที่เปิด /build on -> ปล่อยผ่าน
        if (player.hasPermission("global.admin") && bypassBuilders.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        if (player.hasPermission("global.admin")) {
            sendBlockWarningMessage(player, "admin.block_interact_admin");
        } else {
            sendBlockWarningMessage(player, "admin.block_interact_player");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        FileConfiguration itemsCfg = itemsConfigManager.getConfig();

        String menuTitle = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("gui.menu_title", ""));
        String profileTitle = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("profile.gui.title", "&8Player Profile"));

        if (current != null && playerItemsManager.isAnySpecialItem(current)) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getView().getTitle().equals(profileTitle)) {
            event.setCancelled(true);
            if (current == null || !current.hasItemMeta()) return;
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            if (itemsCfg.getBoolean("profile.gui.items.double_jump_toggle.enabled", true)) {
                int djSlot = itemsCfg.getInt("profile.gui.items.double_jump_toggle.slot", 31);
                if (slot == djSlot) {
                    playerSettingsManager.toggleDoubleJump(player);
                    boolean nowOn = playerSettingsManager.isDoubleJumpEnabled(player);

                    if (!nowOn) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    } else if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                        player.setAllowFlight(true);
                    }

                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            nowOn ? "&aเปิด Double Jump แล้ว!" : "&cปิด Double Jump แล้ว!"));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, nowOn ? 1.5f : 0.5f);
                    openProfileMenu(player);
                    return;
                }
            }

            ConfigurationSection profileItems = itemsCfg.getConfigurationSection("profile.gui.items");
            if (profileItems != null) {
                for (String key : profileItems.getKeys(false)) {
                    if (key.equalsIgnoreCase("double_jump_toggle")) continue;
                    int itemSlot = itemsCfg.getInt("profile.gui.items." + key + ".slot", -1);
                    if (itemSlot == slot) {
                        String link = itemsCfg.getString("profile.gui.items." + key + ".link", "");
                        if (!link.isEmpty()) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', link));
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            player.closeInventory();
                        }
                        break;
                    }
                }
            }
            return;
        }

        if (event.getView().getTitle().equals(menuTitle)) {
            event.setCancelled(true);
            if (current == null || !current.hasItemMeta()) return;
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();
            ConfigurationSection itemsSection = itemsCfg.getConfigurationSection("gui.items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    int itemSlot = itemsCfg.getInt("gui.items." + key + ".slot", -1);
                    if (itemSlot == slot) {
                        player.closeInventory();
                        String action = itemsCfg.getString("gui.items." + key + ".action", "");
                        if (!action.isEmpty()) {
                            player.sendMessage(getGuiMsg("action_executed").replace("{target}", key));
                        } else {
                            player.sendMessage(getGuiMsg("not_configured"));
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        break;
                    }
                }
            }
        }
    }

    private boolean isInSpawnZone(Player player) {
        if (spawnLocation == null) return false;
        return player.getWorld().equals(spawnLocation.getWorld());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isInSpawnZone(player)) return;

        if (player.hasPermission("global.admin") && bypassBuilders.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        if (player.hasPermission("global.admin")) {
            sendBlockWarningMessage(player, "admin.block_break_admin");
        } else {
            sendBlockWarningMessage(player, "admin.block_break_player");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isInSpawnZone(player)) return;

        if (player.hasPermission("global.admin") && bypassBuilders.contains(player.getUniqueId())) {
            return;
        }

        ItemStack item = event.getItemInHand();
        if (item != null && item.getType() == Material.PLAYER_HEAD) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (player.hasPermission("global.admin")) {
            sendBlockWarningMessage(player, "admin.block_place_admin");
        } else {
            sendBlockWarningMessage(player, "admin.block_place_player");
        }
    }

    // ===== Spawn Block Protection =====

    /** ใบไม้ร่วง/เน่า — ปิดในโซน spawn */
    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!getConfig().getBoolean("spawn_protection.leaves_decay", true)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    /** น้ำแข็ง/หิมะละลาย — ปิดในโซน spawn */
    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (!getConfig().getBoolean("spawn_protection.block_fade", true)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    /** ไฟไหม้บล็อก — ปิดในโซน spawn */
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!getConfig().getBoolean("spawn_protection.block_burn", true)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    /** ไฟลุกลาม — ปิดในโซน spawn */
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!getConfig().getBoolean("spawn_protection.block_spread", true)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    /** จุดไฟ — ปิดในโซน spawn */
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!getConfig().getBoolean("spawn_protection.block_ignite", true)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    /** น้ำ/ลาวาไหล — ปิดในโซน spawn (performance: ทำงานเฉพาะเมื่อเปิดใน config) */
    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!getConfig().getBoolean("spawn_protection.liquid_flow", false)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    /** พืช/ต้นไม้โต — ปิดในโซน spawn (ต้องใช้เฉพาะเมื่อต้องการ) */
    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (!getConfig().getBoolean("spawn_protection.block_grow", false)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    /** Entity เปลี่ยนบล็อก (enderman, falling sand, ravager) — ปิดในโซน spawn */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!getConfig().getBoolean("spawn_protection.entity_change_block", true)) return;
        if (spawnLocation == null) return;
        if (!event.getBlock().getWorld().equals(spawnLocation.getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (isInSpawnZone(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (!isInSpawnZone(player)) return;

            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                player.setFallDistance(0);
                if (spawnLocation != null) player.teleport(spawnLocation);
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
        }
    }
}