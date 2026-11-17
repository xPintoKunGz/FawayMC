package org.kcm.bloomery.fawaymc.bedwars.global;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class Global extends JavaPlugin implements Listener, CommandExecutor, TabCompleter, PluginMessageListener {
   private Location spawnLocation;
   private BossbarManager bossBarManager;
   private PlayerItemsManager playerItemsManager;
   private ItemsConfigManager itemsConfigManager;
   private PlayerSettingsManager playerSettingsManager;
   private DoubleJumpManager doubleJumpManager;
   private LaunchpadManager launchpadManager;
   private JoinEffectManager joinEffectManager;
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
   private final Map<UUID, BossBar> adminBossBars = new HashMap<>();
   private final Map<UUID, Long> blockMsgCooldowns = new HashMap<>();
   private final Map<UUID, Long> visibilityCooldowns = new HashMap<>();
   private final Map<String, Integer> bungeeServerCounts = new HashMap<>();
   private int bungeeTotalPlayers = 0;
   private int bungeeTaskId = -1;
   private int adminBossBarTaskId = -1;
   private int itemCheckTaskId = -1;

   @EventHandler
   public void onWorldLoad(WorldLoadEvent event) {
      if (this.spawnLocation == null) {
         this.loadSpawnLocation();
      }
   }

   @EventHandler
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      this.handlePlayerItems(event.getPlayer());
   }

   public void onEnable() {
      this.createCustomConfig();
      this.loadSpawnLocation();
      this.registerChannels();
      this.playerSettingsManager = new PlayerSettingsManager(this);
      this.itemsConfigManager = new ItemsConfigManager(this);
      this.playerItemsManager = new PlayerItemsManager(this, this.itemsConfigManager);
      this.doubleJumpManager = new DoubleJumpManager(this, this.playerSettingsManager);
      this.launchpadManager = new LaunchpadManager(this);
      this.scoreboardManager = new ScoreboardManager(this);
      this.scoreboardManager.start();
      this.tablistManager = new TablistManager(this);
      this.tablistManager.start();
      this.bossBarManager = new BossbarManager(this);
      this.worldRestrictionManager = new WorldRestrictionManager(this);
      this.motdManager = new MotdManager(this);
      this.joinEffectManager = new JoinEffectManager(this);
      this.getServer().getPluginManager().registerEvents(this, this);
      if (this.getCommand("setspawn") != null) {
         this.getCommand("setspawn").setExecutor(this);
      }

      if (this.getCommand("spawn") != null) {
         this.getCommand("spawn").setExecutor(this);
      }

      if (this.getCommand("globalreload") != null) {
         this.getCommand("globalreload").setExecutor(this);
      }

      if (this.getCommand("menu") != null) {
         this.getCommand("menu").setExecutor(this);
      }

      if (this.getCommand("profile") != null) {
         this.getCommand("profile").setExecutor(this);
      }

      if (this.getCommand("build") != null) {
         this.getCommand("build").setExecutor(this);
         this.getCommand("build").setTabCompleter(this);
      }

      if (this.getCommand("adminmode") != null) {
         this.getCommand("adminmode").setExecutor(this);
         this.getCommand("adminmode").setTabCompleter(this);
      }

      if (this.getCommand("worldrestriction") != null) {
         this.getCommand("worldrestriction").setExecutor(this);
         this.getCommand("worldrestriction").setTabCompleter(this);
      }

      this.startAdminBossBarTask();
      this.startItemCheckTask();
      this.getLogger().info(String.valueOf(ChatColor.GREEN) + "Global Plugin เปิดใช้งานสำเร็จแล้ว!");
   }

   public void onDisable() {
      if (this.adminBossBarTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.adminBossBarTaskId);
      }

      if (this.itemCheckTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.itemCheckTaskId);
      }

      if (this.bungeeTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.bungeeTaskId);
      }

      try {
         this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
         this.getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord");
      } catch (Exception var6) {
      }

      // ✅ FIX #1: ใส่ <> ให้ HashSet เพื่อให้ element เป็น UUID ไม่ใช่ Object
      for (UUID uuid : new HashSet<>(this.adminBossBars.keySet())) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null && p.isOnline()) {
            try {
               p.hideBossBar((BossBar)this.adminBossBars.get(uuid));
            } catch (Exception var5) {
            }
         }
      }

      if (this.itemCheckTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.itemCheckTaskId);
         this.itemCheckTaskId = -1;
      }

      if (this.bossBarManager != null) {
         this.bossBarManager.stop();
      }

      if (this.doubleJumpManager != null) {
         this.doubleJumpManager.stop();
      }

      if (this.launchpadManager != null) {
         this.launchpadManager.stop();
      }

      if (this.scoreboardManager != null) {
         this.scoreboardManager.stop();
      }

      this.adminBossBars.clear();
      this.adminModePlayers.clear();
      this.blockMsgCooldowns.clear();
      this.visibilityCooldowns.clear();
      this.bungeeServerCounts.clear();
      this.getLogger().info(String.valueOf(ChatColor.RED) + "Global Plugin ถูกปิดใช้งานเรียบร้อยแล้ว");
   }

   private void startBungeeTask() {
      if (this.bungeeTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.bungeeTaskId);
      }

      this.bungeeTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> this.requestBungeeData(), 100L, 100L).getTaskId();
   }

   private void requestBungeeData() {
      if (!Bukkit.getOnlinePlayers().isEmpty()) {
         Player p = (Player)Bukkit.getOnlinePlayers().iterator().next();
         ByteArrayDataOutput out = ByteStreams.newDataOutput();
         out.writeUTF("PlayerCount");
         out.writeUTF("ALL");
         p.sendPluginMessage(this, "BungeeCord", out.toByteArray());

         for(String server : this.getConfig().getStringList("bungee_servers")) {
            ByteArrayDataOutput out2 = ByteStreams.newDataOutput();
            out2.writeUTF("PlayerCount");
            out2.writeUTF(server);
            p.sendPluginMessage(this, "BungeeCord", out2.toByteArray());
         }

      }
   }

   public void onPluginMessageReceived(String channel, Player player, byte[] message) {
      if (channel.equals("BungeeCord")) {
         try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if (subchannel.equals("PlayerCount")) {
               String server = in.readUTF();
               int count = in.readInt();
               if (server.equalsIgnoreCase("ALL")) {
                  this.bungeeTotalPlayers = count;
               } else {
                  this.bungeeServerCounts.put(server.toLowerCase(), count);
               }
            }
         } catch (Exception var8) {
         }

      }
   }

   private String applyBungeePlaceholders(String text) {
      if (text != null && !text.isEmpty()) {
         text = text.replace("%bungee_total%", String.valueOf(this.bungeeTotalPlayers));
         text = text.replace("%bungee_all%", String.valueOf(this.bungeeTotalPlayers));

         for(Map.Entry<String, Integer> entry : this.bungeeServerCounts.entrySet()) {
            text = text.replace("%bungee_" + (String)entry.getKey() + "%", String.valueOf(entry.getValue()));
         }

         return text;
      } else {
         return text;
      }
   }

   private String applyPlaceholders(Player player, String text) {
      if (text != null && !text.isEmpty()) {
         text = this.applyBungeePlaceholders(text);
         if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
         } else {
            try {
               return PlaceholderAPI.setPlaceholders(player, text);
            } catch (Throwable t) {
               this.getLogger().fine("[Papi] failed: " + t.getMessage());
               return text;
            }
         }
      } else {
         return text;
      }
   }

   private void createCustomConfig() {
      this.customConfigFile = new File(this.getDataFolder(), "global.yml");
      if (!this.customConfigFile.exists()) {
         this.customConfigFile.getParentFile().mkdirs();
         this.saveResource("global.yml", false);
      }

      this.customConfig = YamlConfiguration.loadConfiguration(this.customConfigFile);
   }

   public FileConfiguration getConfig() {
      if (this.customConfig == null) {
         this.reloadGlobalConfig();
      }

      return this.customConfig;
   }

   public void reloadGlobalConfig() {
      if (this.customConfigFile == null) {
         this.customConfigFile = new File(this.getDataFolder(), "global.yml");
      }

      this.customConfig = YamlConfiguration.loadConfiguration(this.customConfigFile);

      try {
         InputStreamReader defConfigStream = new InputStreamReader(this.getResource("global.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            this.customConfig.setDefaults(defConfig);
         } catch (Throwable var5) {
            try {
               defConfigStream.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         defConfigStream.close();
      } catch (Exception var6) {
      }

   }

   public void saveGlobalConfig() {
      if (this.customConfig != null && this.customConfigFile != null) {
         try {
            File tempFile = new File(this.customConfigFile.getParentFile(), this.customConfigFile.getName() + ".tmp");
            this.customConfig.save(tempFile);
            Files.move(tempFile.toPath(), this.customConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException e) {
            this.getLogger().severe("ไม่สามารถบันทึกไฟล์ global.yml ได้: " + e.getMessage());
         }

      }
   }

   private String getMsg(String path) {
      String msg = this.getConfig().getString("messages." + path, "");
      return ChatColor.translateAlternateColorCodes('&', msg);
   }

   private String getGuiMsg(String path) {
      String msg = this.itemsConfigManager.getConfig().getString("gui.messages." + path, "");
      return ChatColor.translateAlternateColorCodes('&', msg);
   }

   private void loadSpawnLocation() {
      FileConfiguration config = this.getConfig();
      if (!config.contains("spawn.world")) {
         this.spawnLocation = null;
      } else {
         String worldName = config.getString("spawn.world");
         if (worldName == null) {
            this.spawnLocation = null;
         } else {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
               this.spawnLocation = null;
            } else {
               this.spawnLocation = new Location(world, config.getDouble("spawn.x"), config.getDouble("spawn.y"), config.getDouble("spawn.z"), (float)config.getDouble("spawn.yaw"), (float)config.getDouble("spawn.pitch"));
            }
         }
      }
   }

   private void saveSpawnLocation(Location loc) {
      if (loc != null && loc.getWorld() != null) {
         this.spawnLocation = loc;
         FileConfiguration config = this.getConfig();
         config.set("spawn.world", loc.getWorld().getName());
         config.set("spawn.x", loc.getX());
         config.set("spawn.y", loc.getY());
         config.set("spawn.z", loc.getZ());
         config.set("spawn.yaw", loc.getYaw());
         config.set("spawn.pitch", loc.getPitch());
         this.saveGlobalConfig();
      }
   }

   /**
    * ใช้โดย ScoreboardManager และคลาสอื่น ๆ ที่ต้องรู้ว่าโลก spawn คือโลกไหน
    */
   public Location getSpawnLocation() {
      return this.spawnLocation;
   }

   private void sendBlockWarningMessage(Player player, String msgKey) {
      UUID uuid = player.getUniqueId();
      long now = System.currentTimeMillis();
      long cooldownMs = this.getConfig().getLong("admin.block_warning_cooldown_ms", 2000L);
      if (!this.blockMsgCooldowns.containsKey(uuid) || now - (Long)this.blockMsgCooldowns.get(uuid) >= cooldownMs) {
         this.blockMsgCooldowns.put(uuid, now);
         player.sendMessage(this.getMsg(msgKey));
      }

   }

   private void startAdminBossBarTask() {
      if (this.adminBossBarTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.adminBossBarTaskId);
         this.adminBossBarTaskId = -1;
      }

      this.adminBossBarTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
         Iterator<Map.Entry<UUID, BossBar>> it = this.adminBossBars.entrySet().iterator();

         while(it.hasNext()) {
            Map.Entry<UUID, BossBar> e = (Map.Entry)it.next();
            UUID uuid = (UUID)e.getKey();
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
               if (!this.adminModePlayers.contains(uuid)) {
                  try {
                     p.hideBossBar((BossBar)e.getValue());
                  } catch (Exception var8) {
                  }

                  it.remove();
               } else {
                  this.updateAdminBossBar(p, (BossBar)e.getValue());
               }
            } else {
               it.remove();
            }
         }

         // ✅ FIX #2: ใส่ <> ให้ HashSet
         for(UUID uuid : new HashSet<>(this.adminModePlayers)) {
            if (!this.adminBossBars.containsKey(uuid)) {
               Player p = Bukkit.getPlayer(uuid);
               if (p != null && p.isOnline()) {
                  BossBar bar = BossBar.bossBar(Component.empty(), 1.0F, Color.GREEN, Overlay.PROGRESS);

                  try {
                     p.showBossBar(bar);
                  } catch (Exception var7) {
                  }

                  this.adminBossBars.put(uuid, bar);
                  this.updateAdminBossBar(p, bar);
               }
            }
         }

      }, 0L, 10L).getTaskId();
   }

   private void updateAdminBossBar(Player player, BossBar bar) {
      boolean buildOn = this.bypassBuilders.contains(player.getUniqueId());
      boolean vanished = this.isVanished(player);
      String raw;
      BossBar.Color color;
      if (buildOn && vanished) {
         raw = this.getConfig().getString("messages.admin.bossbar_on_vanished", "&c[Admin Mode] &fBuild: &aON &7| &fVanish: &aON");
         color = Color.PURPLE;
      } else if (buildOn) {
         raw = this.getConfig().getString("messages.admin.bossbar_on_not_vanished", "&c[Admin Mode] &fBuild: &aON &7| &fVanish: &cOFF");
         color = Color.GREEN;
      } else if (vanished) {
         raw = this.getConfig().getString("messages.admin.bossbar_off_vanished", "&c[Admin Mode] &fBuild: &cOFF &7| &fVanish: &aON");
         color = Color.BLUE;
      } else {
         raw = this.getConfig().getString("messages.admin.bossbar_off", "&c[Admin Mode] &fBuild: &cOFF &7| &fVanish: &cOFF");
         color = Color.RED;
      }

      try {
         bar.name(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
      } catch (Exception var8) {
      }

      bar.color(color);
   }

   private void hideAdminBossBar(Player player) {
      if (player != null) {
         BossBar bar = (BossBar)this.adminBossBars.remove(player.getUniqueId());
         if (bar != null) {
            try {
               player.hideBossBar(bar);
            } catch (Exception var4) {
            }
         }

      }
   }

   private boolean isVanished(Player player) {
      if (player != null && player.isOnline()) {
         try {
            for(MetadataValue mv : player.getMetadata("vanished")) {
               if (mv.asBoolean()) {
                  return true;
               }
            }
         } catch (Throwable var7) {
         }

         try {
            Plugin sv = Bukkit.getPluginManager().getPlugin("SuperVanish");
            if (sv == null) {
               sv = Bukkit.getPluginManager().getPlugin("PremiumVanish");
            }

            if (sv != null && sv.isEnabled()) {
               Object api = sv.getClass().getMethod("getVanishApi").invoke(sv);
               if (api != null) {
                  Object r = api.getClass().getMethod("isVanished", Player.class).invoke(api, player);
                  if (Boolean.TRUE.equals(r)) {
                     return true;
                  }
               }
            }
         } catch (Throwable var6) {
         }

         try {
            Plugin ess = Bukkit.getPluginManager().getPlugin("Essentials");
            if (ess == null) {
               ess = Bukkit.getPluginManager().getPlugin("EssentialsX");
            }

            if (ess != null && ess.isEnabled()) {
               Object user = ess.getClass().getMethod("getUser", Player.class).invoke(ess, player);
               if (user != null) {
                  Object r = user.getClass().getMethod("isVanished").invoke(user);
                  if (Boolean.TRUE.equals(r)) {
                     return true;
                  }
               }
            }
         } catch (Throwable var5) {
         }

         return false;
      } else {
         return false;
      }
   }

   private void startItemCheckTask() {
      if (this.itemCheckTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.itemCheckTaskId);
         this.itemCheckTaskId = -1;
      }

      this.itemCheckTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
         for(Player p : Bukkit.getOnlinePlayers()) {
            this.handlePlayerItems(p);
         }

      }, 0L, 20L).getTaskId();
   }

   private void handlePlayerItems(Player player) {
      boolean inSpawnWorld = this.spawnLocation != null && player.getWorld().equals(this.spawnLocation.getWorld());
      FileConfiguration itemsCfg = this.itemsConfigManager.getConfig();
      if (inSpawnWorld) {
         boolean needsRefill = false;
         if (itemsCfg.getBoolean("gui.open_item.enabled", true)) {
            int slot = itemsCfg.getInt("gui.open_item.slot", 4);
            if (!this.playerItemsManager.isSelectorItem(player.getInventory().getItem(slot))) {
               needsRefill = true;
            }
         }

         if (itemsCfg.getBoolean("profile.item.enabled", true)) {
            int slot = itemsCfg.getInt("profile.item.slot", 0);
            if (!this.playerItemsManager.isProfileItem(player.getInventory().getItem(slot))) {
               needsRefill = true;
            }
         }

         if (itemsCfg.getBoolean("visibility.item.enabled", true)) {
            int slot = itemsCfg.getInt("visibility.item.slot", 8);
            if (!this.playerItemsManager.isVisibilityItem(player.getInventory().getItem(slot))) {
               needsRefill = true;
            }
         }

         if (itemsCfg.getBoolean("cosmetics_item.enabled", true)) {
            int slot = itemsCfg.getInt("cosmetics_item.slot", 3);
            if (!this.playerItemsManager.isCosmeticsItem(player.getInventory().getItem(slot))) {
               needsRefill = true;
            }
         }

         if (needsRefill) {
            this.playerItemsManager.givePlayerItems(player);
         }
      } else {
         boolean removed = false;
         ItemStack[] contents = player.getInventory().getContents();

         for(int i = 0; i < contents.length; ++i) {
            ItemStack item = contents[i];
            if (item != null && this.playerItemsManager.isAnySpecialItem(item)) {
               player.getInventory().setItem(i, (ItemStack)null);
               removed = true;
            }
         }

         if (removed) {
            player.updateInventory();
         }
      }

   }

   private void openGameMenu(Player player) {
      this.requestBungeeData();
      FileConfiguration itemsCfg = this.itemsConfigManager.getConfig();
      String title = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("gui.menu_title", "&8Game Menu"));
      int size = itemsCfg.getInt("gui.size", 27);
      Inventory menu = Bukkit.createInventory((InventoryHolder)null, size, title);
      ConfigurationSection itemsSection = itemsCfg.getConfigurationSection("gui.items");
      if (itemsSection != null) {
         for(String key : itemsSection.getKeys(false)) {
            int slot = itemsCfg.getInt("gui.items." + key + ".slot", 0);
            String matName = itemsCfg.getString("gui.items." + key + ".material", "STONE");
            String rawName = itemsCfg.getString("gui.items." + key + ".name", "");
            String name = ChatColor.translateAlternateColorCodes('&', this.applyPlaceholders(player, rawName));
            List<String> rawLore = itemsCfg.getStringList("gui.items." + key + ".lore");
            List<String> lore = new ArrayList<>();

            for(String l : rawLore) {
               lore.add(ChatColor.translateAlternateColorCodes('&', this.applyPlaceholders(player, l)));
            }

            Material material = Material.matchMaterial(matName);
            if (material == null) {
               material = Material.STONE;
            }

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
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
   }

   private void registerChannels() {
      try {
         if (this.getServer().getMessenger().isOutgoingChannelRegistered(this, "BungeeCord")) {
            this.getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
         }

         if (this.getServer().getMessenger().isIncomingChannelRegistered(this, "BungeeCord")) {
            this.getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord");
         }
      } catch (Exception var3) {
      }

      try {
         this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
         this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
         this.getLogger().info("[Global] ✓ BungeeCord channel registered successfully");
      } catch (Exception e) {
         this.getLogger().severe("[Global] ✗ Failed to register BungeeCord channel: " + e.getMessage());
      }

   }

   private void sendToServer(Player player, String serverName) {
      if (serverName != null && !serverName.isEmpty()) {
         if (!this.getServer().getMessenger().isOutgoingChannelRegistered(this, "BungeeCord")) {
            this.registerChannels();
            if (!this.getServer().getMessenger().isOutgoingChannelRegistered(this, "BungeeCord")) {
               player.sendMessage(String.valueOf(ChatColor.RED) + "✗ BungeeCord channel not available! Please restart the server.");
               this.getLogger().warning("[Global] BungeeCord channel not registered after retry!");
               return;
            }
         }

         try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
            String var10001 = String.valueOf(ChatColor.GREEN);
            player.sendMessage(var10001 + "กำลังเชื่อมต่อเซิร์ฟเวอร์ " + serverName + "...");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
         } catch (Exception e) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้");
            this.getLogger().warning("[Global] Failed to send player to server: " + e.getMessage());
         }

      }
   }

   private String colorize(String s) {
      return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
   }

   private ItemStack makePane(Material mat) {
      ItemStack pane = new ItemStack(mat);
      ItemMeta m = pane.getItemMeta();
      if (m != null) {
         m.setDisplayName(" ");
         m.addItemFlags(ItemFlag.values());
         pane.setItemMeta(m);
      }

      return pane;
   }

   private void openProfileMenu(Player player) {
      FileConfiguration cfg = this.itemsConfigManager.getConfig();
      String base = "profile.gui.";
      String title = this.colorize(cfg.getString("profile.gui.title", "&8Player Profile"));
      int size = cfg.getInt("profile.gui.size", 54);
      if (size % 9 != 0) {
         size = 54;
      }

      size = Math.max(9, Math.min(54, size));
      Inventory menu = Bukkit.createInventory((InventoryHolder)null, size, title);
      Material borderMat = Material.matchMaterial(cfg.getString("profile.gui.border.material", "LIGHT_BLUE_STAINED_GLASS_PANE"));
      if (borderMat == null) {
         borderMat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
      }

      Material fillerMat = Material.matchMaterial(cfg.getString("profile.gui.filler.material", "BLACK_STAINED_GLASS_PANE"));
      if (fillerMat == null) {
         fillerMat = Material.BLACK_STAINED_GLASS_PANE;
      }

      ItemStack border = this.makePane(borderMat);
      ItemStack filler = this.makePane(fillerMat);

      for(int i = 0; i < size; ++i) {
         menu.setItem(i, filler);
      }

      for(int i = 0; i < 9; ++i) {
         menu.setItem(i, border);
      }

      for(int i = size - 9; i < size; ++i) {
         menu.setItem(i, border);
      }

      for(int r = 1; r < size / 9 - 1; ++r) {
         menu.setItem(r * 9, border);
         menu.setItem(r * 9 + 8, border);
      }

      Long lbCoins = this.getLuckyblockCoins(player.getUniqueId());
      long coins = lbCoins != null ? lbCoins : (long)(Integer)this.playerCoins.getOrDefault(player.getUniqueId(), 0);
      int kills = (Integer)this.playerKills.getOrDefault(player.getUniqueId(), 0);
      int deaths = (Integer)this.playerDeaths.getOrDefault(player.getUniqueId(), 0);
      ConfigurationSection items = cfg.getConfigurationSection("profile.gui.items");
      if (items != null) {
         for(String key : items.getKeys(false)) {
            if (!key.equalsIgnoreCase("double_jump_toggle")) {
               String p = "profile.gui.items." + key + ".";
               if (cfg.getBoolean(p + "enabled", true)) {
                  int slot = cfg.getInt(p + "slot", -1);
                  if (slot >= 0 && slot < size) {
                     String name = this.colorize(this.applyPapi(player, cfg.getString(p + "name", ""))).replace("{player}", player.getName());
                     List<String> lore = new ArrayList<>();

                     for(String raw : cfg.getStringList(p + "lore")) {
                        String line = raw.replace("{player}", player.getName()).replace("{coins}", String.valueOf(coins)).replace("{kills}", String.valueOf(kills)).replace("{deaths}", String.valueOf(deaths));
                        lore.add(this.colorize(this.applyPapi(player, line)));
                     }

                     String matName = cfg.getString(p + "material", "STONE");
                     ItemStack item;
                     if (matName.equalsIgnoreCase("PLAYER_HEAD")) {
                        item = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta hm = (SkullMeta)item.getItemMeta();
                        if (hm != null) {
                           String texture = cfg.getString(p + "head_texture", "");
                           if (!texture.isEmpty()) {
                              try {
                                 PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                                 profile.setProperty(new ProfileProperty("textures", texture));
                                 hm.setPlayerProfile(profile);
                              } catch (Throwable t) {
                                 this.getLogger().warning("[Profile] head_texture failed for '" + key + "': " + t.getMessage());
                                 hm.setOwningPlayer(player);
                              }
                           } else {
                              hm.setOwningPlayer(player);
                           }

                           hm.setDisplayName(name);
                           hm.setLore(lore);
                           hm.addItemFlags(ItemFlag.values());
                           item.setItemMeta(hm);
                        }
                     } else {
                        Material mat = Material.matchMaterial(matName);
                        if (mat == null) {
                           mat = Material.STONE;
                        }

                        item = new ItemStack(mat);
                        ItemMeta im = item.getItemMeta();
                        if (im != null) {
                           im.setDisplayName(name);
                           im.setLore(lore);
                           im.addItemFlags(ItemFlag.values());
                           item.setItemMeta(im);
                        }
                     }

                     menu.setItem(slot, item);
                  }
               }
            }
         }
      }

      String dj = "profile.gui.items.double_jump_toggle.";
      if (cfg.getBoolean(dj + "enabled", true)) {
         int slot = cfg.getInt(dj + "slot", 39);
         if (slot >= 0 && slot < size) {
            boolean on = this.playerSettingsManager.isDoubleJumpEnabled(player);
            Material mat = Material.matchMaterial(on ? cfg.getString(dj + "material_on", "FEATHER") : cfg.getString(dj + "material_off", "GRAY_DYE"));
            if (mat == null) {
               mat = on ? Material.FEATHER : Material.GRAY_DYE;
            }

            List<String> lore = new ArrayList<>();

            for(String l : cfg.getStringList(dj + (on ? "lore_on" : "lore_off"))) {
               lore.add(this.colorize(this.applyPapi(player, l)));
            }

            ItemStack djItem = new ItemStack(mat);
            ItemMeta dm = djItem.getItemMeta();
            if (dm != null) {
               dm.setDisplayName(this.colorize(on ? cfg.getString(dj + "name_on", "&a&lDouble Jump: ON") : cfg.getString(dj + "name_off", "&c&lDouble Jump: OFF")));
               dm.setLore(lore);
               dm.addItemFlags(ItemFlag.values());
               djItem.setItemMeta(dm);
            }

            menu.setItem(slot, djItem);
         }
      }

      if (cfg.getBoolean("profile.gui.close.enabled", true)) {
         int slot = cfg.getInt("profile.gui.close.slot", size - 5);
         if (slot >= 0 && slot < size) {
            Material mat = Material.matchMaterial(cfg.getString("profile.gui.close.material", "BARRIER"));
            if (mat == null) {
               mat = Material.BARRIER;
            }

            ItemStack close = new ItemStack(mat);
            ItemMeta cm = close.getItemMeta();
            if (cm != null) {
               cm.setDisplayName(this.colorize(cfg.getString("profile.gui.close.name", "&c&lClose")));
               List<String> lore = new ArrayList<>();

               for(String l : cfg.getStringList("profile.gui.close.lore")) {
                  lore.add(this.colorize(l));
               }

               cm.setLore(lore);
               cm.addItemFlags(ItemFlag.values());
               close.setItemMeta(cm);
            }

            menu.setItem(slot, close);
         }
      }

      player.openInventory(menu);
      player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
   }

   private Plugin getLuckyblockPlugin() {
      Plugin lb = Bukkit.getPluginManager().getPlugin("Luckyblock");
      if (lb == null) {
         lb = Bukkit.getPluginManager().getPlugin("luckyblock");
      }

      if (lb == null) {
         lb = Bukkit.getPluginManager().getPlugin("LuckyBlock");
      }

      return lb;
   }

   private String applyPapi(Player player, String text) {
      if (text != null && !text.isEmpty()) {
         if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
         } else {
            try {
               return PlaceholderAPI.setPlaceholders(player, text);
            } catch (Throwable t) {
               this.getLogger().fine("[Papi] failed: " + t.getMessage());
               return text;
            }
         }
      } else {
         return text;
      }
   }

   private Long getLuckyblockCoins(UUID uuid) {
      try {
         Plugin lb = this.getLuckyblockPlugin();
         if (lb == null || !lb.isEnabled()) {
            this.getLogger().fine("[Coins] Luckyblock not available");
            return null;
         }

         Object dataMgr = lb.getClass().getMethod("getData").invoke(lb);
         if (dataMgr == null) {
            this.getLogger().warning("[Coins] getData() returned null");
            return null;
         }

         Method coinsMethod = null;
         Class<?> argType = null;

         for(Method m : dataMgr.getClass().getMethods()) {
            if (m.getName().equals("getCoins") && m.getParameterCount() == 1) {
               Class<?> p0 = m.getParameterTypes()[0];
               if (p0 == UUID.class || Player.class.isAssignableFrom(p0) || OfflinePlayer.class.isAssignableFrom(p0)) {
                  coinsMethod = m;
                  argType = p0;
                  break;
               }
            }
         }

         if (coinsMethod == null) {
            this.getLogger().warning("[Coins] getCoins(...) with UUID/Player/OfflinePlayer not found");
            return null;
         }

         Object arg;
         if (argType == UUID.class) {
            arg = uuid;
         } else {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
               return null;
            }

            arg = p;
         }

         Object result = coinsMethod.invoke(dataMgr, arg);
         if (result instanceof Number) {
            long v = ((Number)result).longValue();
            this.getLogger().info("[Coins] " + String.valueOf(uuid) + " → " + v + " (arg=" + argType.getSimpleName() + ")");
            return v;
         }

         this.getLogger().warning("[Coins] getCoins returned non-Number: " + String.valueOf(result));
      } catch (Throwable e) {
         this.getLogger().warning("[Coins] reflection failed: " + String.valueOf(e));
      }

      return null;
   }

   private int[] getCosmeticsStats(Player player) {
      int owned = 0;
      int total = 0;
      int equipped = 0;

      try {
         Plugin lb = this.getLuckyblockPlugin();
         if (lb == null || !lb.isEnabled()) {
            return new int[]{0, 0, 0};
         }

         Object configObj = lb.getClass().getMethod("getConfig").invoke(lb);
         if (!(configObj instanceof FileConfiguration)) {
            this.getLogger().warning("[CosStats] getConfig() wrong type");
            return new int[]{0, 0, 0};
         }

         ConfigurationSection cosSec = ((FileConfiguration)configObj).getConfigurationSection("cosmetics");
         if (cosSec == null) {
            this.getLogger().warning("[CosStats] cosmetics section missing");
            return new int[]{0, 0, 0};
         }

         Object dataMgr = lb.getClass().getMethod("getData").invoke(lb);
         if (dataMgr == null) {
            this.getLogger().warning("[CosStats] getData() null");
            return new int[]{0, 0, 0};
         }

         this.getLogger().info("[CosStats] dataMgr class: " + dataMgr.getClass().getName());
         Method ownsMethod = null;
         Class<?> ownsArgType = null;

         for(Method m : dataMgr.getClass().getMethods()) {
            if (m.getName().equals("ownsCosmetic") && m.getParameterCount() == 2) {
               Class<?>[] p = m.getParameterTypes();
               boolean argOk = p[0] == UUID.class || Player.class.isAssignableFrom(p[0]) || OfflinePlayer.class.isAssignableFrom(p[0]);
               if (argOk && p[1] == String.class) {
                  ownsMethod = m;
                  ownsArgType = p[0];
                  break;
               }
            }
         }

         if (ownsMethod == null) {
            this.getLogger().warning("[CosStats] ownsCosmetic not found. Methods available:");

            for(Method m : dataMgr.getClass().getMethods()) {
               if (m.getName().toLowerCase().contains("cosmetic")) {
                  Logger var49 = this.getLogger();
                  String var52 = m.getName();
                  var49.warning("[CosStats]   - " + var52 + " (params: " + Arrays.toString(m.getParameterTypes()) + ")");
               }
            }

            return new int[]{0, 0, 0};
         }

         this.getLogger().info("[CosStats] ownsCosmetic using arg: " + ownsArgType.getSimpleName());
         Object ownsArg = ownsArgType == UUID.class ? player.getUniqueId() : player;

         for(String id : cosSec.getKeys(false)) {
            ++total;

            try {
               Object r = ownsMethod.invoke(dataMgr, ownsArg, id);
               if (Boolean.TRUE.equals(r)) {
                  ++owned;
               }
            } catch (Throwable t) {
               this.getLogger().fine("[CosStats] owns error '" + id + "': " + String.valueOf(t));
            }
         }

         Method equippedMethod = null;
         Class<?> equippedArgType = null;
         Class<?> cosTypeEnum = null;

         for(Method m : dataMgr.getClass().getMethods()) {
            if (m.getName().equals("getEquipped") && m.getParameterCount() == 2) {
               Class<?>[] p = m.getParameterTypes();
               boolean argOk = p[0] == UUID.class || Player.class.isAssignableFrom(p[0]) || OfflinePlayer.class.isAssignableFrom(p[0]);
               if (argOk && p[1].isEnum()) {
                  equippedMethod = m;
                  equippedArgType = p[0];
                  cosTypeEnum = p[1];
                  break;
               }
            }
         }

         if (equippedMethod == null) {
            this.getLogger().warning("[CosStats] getEquipped not found. Methods available:");

            for(Method m : dataMgr.getClass().getMethods()) {
               if (m.getName().toLowerCase().contains("equip")) {
                  Logger var48 = this.getLogger();
                  String var51 = m.getName();
                  var48.warning("[CosStats]   - " + var51 + " (params: " + Arrays.toString(m.getParameterTypes()) + ")");
               }
            }
         } else {
            Object eqArg = equippedArgType == UUID.class ? player.getUniqueId() : player;
            Logger var10000 = this.getLogger();
            String var10001 = equippedArgType.getSimpleName();
            var10000.info("[CosStats] getEquipped using arg: " + var10001 + ", enum: " + cosTypeEnum.getSimpleName());

            for(Object type : cosTypeEnum.getEnumConstants()) {
               try {
                  Object r = equippedMethod.invoke(dataMgr, eqArg, type);
                  if (r instanceof String && !((String)r).isEmpty()) {
                     ++equipped;
                  }
               } catch (Throwable t) {
                  var10000 = this.getLogger();
                  var10001 = String.valueOf(type);
                  var10000.fine("[CosStats] getEquipped error " + var10001 + ": " + String.valueOf(t));
               }
            }
         }

         this.getLogger().info("[CosStats] " + player.getName() + " → owned=" + owned + "/" + total + ", equipped=" + equipped);
      } catch (Throwable e) {
         this.getLogger().warning("[CosStats] failed: " + String.valueOf(e));
         e.printStackTrace();
      }

      return new int[]{owned, total, equipped};
   }

   private void openCosmeticsMenu(Player player) {
      this.getLogger().info("[Global] openCosmeticsMenu called for " + player.getName());
      if (Bukkit.getPluginCommand("cosmetics") != null) {
         try {
            if (Bukkit.dispatchCommand(player, "cosmetics")) {
               this.getLogger().info("[Global] ✓ Opened via /cosmetics command");
               return;
            }
         } catch (Exception e) {
            this.getLogger().warning("[Global] dispatchCommand threw: " + e.getMessage());
         }
      }

      Plugin luckyblock = this.getLuckyblockPlugin();
      if (luckyblock == null) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "❌ Luckyblock plugin not installed!");
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (!luckyblock.isEnabled()) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "❌ Luckyblock is disabled!");
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         try {
            Object cosmeticManager = luckyblock.getClass().getMethod("getCosmeticManager").invoke(luckyblock);
            if (cosmeticManager == null) {
               player.sendMessage(String.valueOf(ChatColor.RED) + "❌ CosmeticManager is null!");
               return;
            }

            cosmeticManager.getClass().getMethod("openMenu", Player.class).invoke(cosmeticManager, player);
            this.getLogger().info("[Global] ✓ Opened via reflection (CosmeticManager.openMenu)");
         } catch (Exception e) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "❌ Reflection failed!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            this.getLogger().severe("[Global] Reflection error: " + e.getMessage());
         }

      }
   }

   private void openCosmeticsShopMenu(Player player) {
      this.getLogger().info("[CosShop] Opening for " + player.getName());
      if (Bukkit.getPluginCommand("cosmeticshop") != null) {
         try {
            if (Bukkit.dispatchCommand(player, "cosmeticshop")) {
               this.getLogger().info("[CosShop] ✓ via /cosmeticshop command");
               return;
            }

            this.getLogger().warning("[CosShop] dispatchCommand returned false");
         } catch (Throwable e) {
            this.getLogger().warning("[CosShop] dispatch threw: " + String.valueOf(e));
         }
      } else {
         this.getLogger().info("[CosShop] /cosmeticshop not registered, using reflection");
      }

      Plugin lb = this.getLuckyblockPlugin();
      if (lb != null && lb.isEnabled()) {
         try {
            Object shopMgr = lb.getClass().getMethod("getShopManager").invoke(lb);
            if (shopMgr == null) {
               this.getLogger().severe("[CosShop] getShopManager() returned null");
               player.sendMessage(String.valueOf(ChatColor.RED) + "❌ ShopManager is null!");
               return;
            }

            this.getLogger().info("[CosShop] got shopManager: " + shopMgr.getClass().getName());
            Method openMethod = null;
            Class<?> originClass = null;

            for(Method m : shopMgr.getClass().getMethods()) {
               if (m.getName().equals("openShop") && m.getParameterCount() == 2) {
                  Class<?>[] p = m.getParameterTypes();
                  if (p[0] == Player.class && p[1].isEnum()) {
                     openMethod = m;
                     originClass = p[1];
                     break;
                  }
               }
            }

            if (openMethod == null || originClass == null) {
               this.getLogger().severe("[CosShop] openShop(Player,Enum) not found");
               player.sendMessage(String.valueOf(ChatColor.RED) + "❌ Method not found! Check console.");
               return;
            }

            this.getLogger().info("[CosShop] found enum: " + originClass.getName());

            Object noneValue;
            try {
               // ✅ FIX #3: cast originClass เป็น raw Class เพื่อให้ Enum.valueOf compile ผ่าน
               // originClass เป็น Class<?> → T ของ Enum.valueOf infer ไม่ได้
               @SuppressWarnings({"unchecked", "rawtypes"})
               Object v = Enum.valueOf((Class) originClass, "NONE");
               noneValue = v;
            } catch (IllegalArgumentException var11) {
               this.getLogger().severe("[CosShop] ShopOrigin.NONE not found");
               player.sendMessage(String.valueOf(ChatColor.RED) + "❌ ShopOrigin.NONE not found!");
               return;
            }

            this.getLogger().info("[CosShop] enum NONE = " + String.valueOf(noneValue));
            openMethod.invoke(shopMgr, player, noneValue);
            this.getLogger().info("[CosShop] ✓ invoked openShop(player, NONE)");
         } catch (Throwable e) {
            this.getLogger().severe("[CosShop] reflection failed: " + String.valueOf(e));
            e.printStackTrace();
            player.sendMessage(String.valueOf(ChatColor.RED) + "❌ Failed to open shop! Check console.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         }

      } else {
         player.sendMessage(String.valueOf(ChatColor.RED) + "❌ Luckyblock not available!");
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      }
   }

   private void handleProfileAction(Player player, String action) {
      if (action != null && !action.isEmpty()) {
         switch (action.toLowerCase()) {
            case "cosmetics":
               this.openCosmeticsMenu(player);
               break;
            case "cosmetics_shop":
               this.openCosmeticsShopMenu(player);
               break;
            case "friends":
               player.closeInventory();
               Bukkit.getScheduler().runTask(this, () -> {
                  if (player.isOnline()) {
                     if (Bukkit.getPluginCommand("friend") != null) {
                        Bukkit.dispatchCommand(player, "friend");
                     } else if (Bukkit.getPluginCommand("friends") != null) {
                        Bukkit.dispatchCommand(player, "friends");
                     } else {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "❌ Friend system not available!");
                     }

                  }
               });
               break;
            default:
               this.getLogger().warning("[Global] Unknown profile action: " + action);
         }

      }
   }

   private void reloadEverything() {
      this.reloadGlobalConfig();
      this.loadSpawnLocation();
      if (this.doubleJumpManager != null) {
         this.doubleJumpManager.reloadJumpConfig();
      }

      if (this.bossBarManager != null) {
         this.bossBarManager.reloadBarConfig();
      }

      if (this.worldRestrictionManager != null) {
         this.worldRestrictionManager.reloadRestrictionConfig();
      }

      if (this.motdManager != null) {
         this.motdManager.reloadMotdConfig();
      }

      if (this.scoreboardManager != null) {
         this.scoreboardManager.reloadScoreboardConfig();
         this.scoreboardManager.start();
      }

      if (this.tablistManager != null) {
         this.tablistManager.reloadTablistConfig();
         this.tablistManager.start();
      }

      if (this.playerSettingsManager != null) {
         this.playerSettingsManager.reload();
      }

      if (this.itemsConfigManager != null) {
         this.itemsConfigManager.reload();
      }

      if (this.launchpadManager != null) {
         this.launchpadManager.reloadConfig();
      }

      if (this.joinEffectManager != null) {
         this.joinEffectManager.reloadConfig();
      }

      this.startAdminBossBarTask();
      this.startItemCheckTask();

      for(Player p : Bukkit.getOnlinePlayers()) {
         this.handlePlayerItems(p);
         if (this.scoreboardManager != null) {
            this.scoreboardManager.updateScoreboard(p);
         }

         if (this.tablistManager != null) {
            this.tablistManager.updateTablist(p);
         }

         if (this.bossBarManager != null) {
            this.bossBarManager.updateBossBar(p);
         }
      }

   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("globalreload")) {
         if (sender instanceof Player && !sender.hasPermission("global.admin")) {
            sender.sendMessage(this.getMsg("admin.no_permission"));
            return true;
         } else {
            this.reloadEverything();
            sender.sendMessage(this.getMsg("reload.success"));
            if (sender instanceof Player) {
               ((Player)sender).playSound(((Player)sender).getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            }

            return true;
         }
      } else if (command.getName().equalsIgnoreCase("menu")) {
         if (!(sender instanceof Player)) {
            return true;
         } else {
            this.openGameMenu((Player)sender);
            return true;
         }
      } else if (command.getName().equalsIgnoreCase("profile")) {
         if (!(sender instanceof Player)) {
            return true;
         } else {
            this.openProfileMenu((Player)sender);
            return true;
         }
      } else if (command.getName().equalsIgnoreCase("build")) {
         if (!sender.hasPermission("global.admin")) {
            sender.sendMessage(this.getMsg("admin.no_permission"));
            return true;
         } else if (!(sender instanceof Player)) {
            sender.sendMessage("Console cannot use build command.");
            return true;
         } else {
            Player player = (Player)sender;
            UUID uuid = player.getUniqueId();
            if (args.length == 0) {
               if (this.bypassBuilders.contains(uuid)) {
                  this.bypassBuilders.remove(uuid);
               } else {
                  this.bypassBuilders.add(uuid);
               }

               return true;
            } else {
               String sub = args[0].toLowerCase();
               if (sub.equals("on")) {
                  this.bypassBuilders.add(uuid);
               } else if (sub.equals("off")) {
                  this.bypassBuilders.remove(uuid);
               } else {
                  player.sendMessage(this.getMsg("admin.build_usage"));
               }

               return true;
            }
         }
      } else if (!(sender instanceof Player)) {
         return true;
      } else {
         Player player = (Player)sender;
         if (command.getName().equalsIgnoreCase("adminmode")) {
            if (!player.hasPermission("global.admin")) {
               player.sendMessage(this.getMsg("admin.no_permission"));
               return true;
            } else {
               UUID uuid = player.getUniqueId();
               if (args.length == 0) {
                  if (this.adminModePlayers.contains(uuid)) {
                     this.adminModePlayers.remove(uuid);
                     this.hideAdminBossBar(player);
                     player.sendMessage(this.getMsg("admin.adminmode_disabled"));
                  } else {
                     this.adminModePlayers.add(uuid);
                     player.sendMessage(this.getMsg("admin.adminmode_enabled"));
                  }

                  return true;
               } else {
                  String sub = args[0].toLowerCase();
                  if (sub.equals("on")) {
                     this.adminModePlayers.add(uuid);
                     player.sendMessage(this.getMsg("admin.adminmode_enabled"));
                  } else if (sub.equals("off")) {
                     this.adminModePlayers.remove(uuid);
                     this.hideAdminBossBar(player);
                     player.sendMessage(this.getMsg("admin.adminmode_disabled"));
                  } else {
                     player.sendMessage(this.getMsg("admin.adminmode_usage"));
                  }

                  return true;
               }
            }
         } else if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("global.admin")) {
               player.sendMessage(this.getMsg("admin.no_permission"));
               return true;
            } else {
               this.saveSpawnLocation(player.getLocation());
               player.sendMessage(this.getMsg("spawn.set_success"));
               player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
               return true;
            }
         } else if (command.getName().equalsIgnoreCase("spawn")) {
            if (this.spawnLocation == null) {
               player.sendMessage(this.getMsg("spawn.not_set"));
               return true;
            } else {
               player.teleport(this.spawnLocation);
               player.sendMessage(this.getMsg("spawn.teleport_success"));
               player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
      if (command.getName().equalsIgnoreCase("adminmode") && sender.hasPermission("global.admin") && args.length == 1) {
         String input = args[0].toLowerCase();
         if ("on".startsWith(input)) {
            completions.add("on");
         }

         if ("off".startsWith(input)) {
            completions.add("off");
         }
      }

      return completions;
   }

   private String getMsgForPlayer(Player player, String path) {
      String msg = this.getConfig().getString("messages." + path, "");
      return msg.isEmpty() ? "" : PlaceholderManager.setPlaceholders(player, msg);
   }

   private List<String> getMsgListForPlayer(Player player, String path) {
      List<String> list = this.getConfig().getStringList("messages." + path);
      List<String> out = new ArrayList<>();

      for(String s : list) {
         out.add(PlaceholderManager.setPlaceholders(player, s));
      }

      return out;
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      String playerName = player.getName();
      String rawJoin = "";
      if (this.joinEffectManager != null) {
         try {
            rawJoin = this.joinEffectManager.getJoinMessage();
         } catch (Exception var7) {
         }
      }

      if (rawJoin == null || rawJoin.isEmpty()) {
         rawJoin = this.getConfig().getString("messages.join.message", "");
      }

      if (rawJoin == null || rawJoin.isEmpty()) {
         rawJoin = "&a[+] {player} joined the server!";
      }

      try {
         String joinMsg = PlaceholderManager.setPlaceholders(player, rawJoin).replace("{player}", playerName);
         event.setJoinMessage((String)null);
         Bukkit.broadcastMessage(joinMsg);
      } catch (Exception e) {
         this.getLogger().severe("[Global] broadcast error: " + e.getMessage());
      }

      player.setGameMode(GameMode.SURVIVAL);
      if (this.spawnLocation != null) {
         player.teleport(this.spawnLocation);
      }

      this.playerItemsManager.onJoin(player);
      this.bypassBuilders.remove(player.getUniqueId());
      this.handlePlayerItems(player);
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      String playerName = player.getName();
      String rawQuit = "";
      if (this.joinEffectManager != null) {
         try {
            rawQuit = this.joinEffectManager.getQuitMessage();
         } catch (Exception var7) {
         }
      }

      if (rawQuit == null || rawQuit.isEmpty()) {
         rawQuit = this.getConfig().getString("messages.quit.message", "");
      }

      if (rawQuit == null || rawQuit.isEmpty()) {
         rawQuit = "&c[-] {player} left the server.";
      }

      try {
         String quitMsg = PlaceholderManager.setPlaceholders(player, rawQuit).replace("{player}", playerName);
         event.setQuitMessage((String)null);
         Bukkit.broadcastMessage(quitMsg);
      } catch (Exception e) {
         this.getLogger().severe("[Global] quit broadcast error: " + e.getMessage());
      }

      if (this.scoreboardManager != null) {
         this.scoreboardManager.clearPlayer(event.getPlayer());
      }

      this.playerItemsManager.onQuit(player);
      this.bypassBuilders.remove(player.getUniqueId());
      this.blockMsgCooldowns.remove(player.getUniqueId());
      this.adminModePlayers.remove(player.getUniqueId());
      this.adminBossBars.remove(player.getUniqueId());
   }

   @EventHandler
   public void onPlayerDeath(PlayerDeathEvent event) {
      event.getItemsToKeep().clear();
      event.getDrops().clear();
      Player player = event.getEntity();
      if (this.isInSpawnZone(player)) {
         event.getItemsToKeep().clear();
         event.getDrops().clear();
         if (this.spawnLocation != null) {
            player.setBedSpawnLocation(this.spawnLocation, true);
         }
      }

      this.playerDeaths.put(player.getUniqueId(), (Integer)this.playerDeaths.getOrDefault(player.getUniqueId(), 0) + 1);
      if (player.getKiller() != null) {
         Player killer = player.getKiller();
         this.playerKills.put(killer.getUniqueId(), (Integer)this.playerKills.getOrDefault(killer.getUniqueId(), 0) + 1);
      }

   }

   @EventHandler
   public void onPlayerRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      player.setGameMode(GameMode.SURVIVAL);
      if (this.isInSpawnZone(player) && this.spawnLocation != null) {
         player.setGameMode(GameMode.SURVIVAL);
         event.setRespawnLocation(this.spawnLocation);
      }

      Bukkit.getScheduler().runTaskLater(this, () -> {
         this.handlePlayerItems(player);
         if (this.bossBarManager != null) {
            this.bossBarManager.updateBossBar(player);
         }

      }, 2L);
   }

   @EventHandler
   public void onPlayerDropItem(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItemDrop().getItemStack();
      if (this.playerItemsManager.isAnySpecialItem(item)) {
         event.setCancelled(true);
      } else {
         if (this.isInSpawnZone(player)) {
            if (player.hasPermission("global.admin") && this.bypassBuilders.contains(player.getUniqueId())) {
               return;
            }

            if (!this.getConfig().getBoolean("spawn_protection.item_drop", true)) {
               return;
            }

            event.setCancelled(true);
            if (player.hasPermission("global.admin")) {
               this.sendBlockWarningMessage(player, "admin.block_drop_admin");
            } else {
               this.sendBlockWarningMessage(player, "admin.block_drop_player");
            }
         }

      }
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      if (item != null && this.playerItemsManager.isAnySpecialItem(item)) {
         if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (this.playerItemsManager.isSelectorItem(item)) {
               this.openGameMenu(player);
            } else if (this.playerItemsManager.isProfileItem(item)) {
               this.openProfileMenu(player);
            } else if (this.playerItemsManager.isVisibilityItem(item)) {
               long now = System.currentTimeMillis();
               long lastUse = (Long)this.visibilityCooldowns.getOrDefault(player.getUniqueId(), 0L);
               if (now - lastUse < 3000L) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "กรุณารอสักครู่ก่อนใช้งานอีกครั้ง!");
                  player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }

               this.visibilityCooldowns.put(player.getUniqueId(), now);
               this.playerItemsManager.togglePlayerVisibility(player);
            } else if (this.playerItemsManager.isCosmeticsItem(item)) {
               this.openCosmeticsMenu(player);
            }
         }

      } else if (this.isInSpawnZone(player)) {
         if (!player.hasPermission("global.admin") || !this.bypassBuilders.contains(player.getUniqueId())) {
            Action action = event.getAction();
            if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
               event.setCancelled(true);
               if (event.getClickedBlock().getType().isInteractable()) {
                  if (player.hasPermission("global.admin")) {
                     this.sendBlockWarningMessage(player, "admin.block_interact_admin");
                  } else {
                     this.sendBlockWarningMessage(player, "admin.block_interact_player");
                  }
               }

            } else if (action == Action.RIGHT_CLICK_AIR) {
               if (item != null && item.getType() != Material.AIR) {
                  event.setCancelled(true);
               }

            }
         }
      }
   }

   @EventHandler(
           priority = EventPriority.LOWEST
   )
   public void onHangingBreak(HangingBreakEvent event) {
      if (event.getEntity().getWorld().equals(this.spawnLocation != null ? this.spawnLocation.getWorld() : null)) {
         if (event instanceof HangingBreakByEntityEvent) {
            HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent)event;
            if (entityEvent.getRemover() instanceof Player) {
               Player player = (Player)entityEvent.getRemover();
               if (!player.hasPermission("global.admin") || !this.bypassBuilders.contains(player.getUniqueId())) {
                  event.setCancelled(true);
                  this.sendBlockWarningMessage(player, "admin.block_entity_damage_player");
               }
            }
         } else {
            event.setCancelled(true);
         }
      }

   }

   @EventHandler
   public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
      Player player = event.getPlayer();
      if (this.isInSpawnZone(player)) {
         if (!player.hasPermission("global.admin") || !this.bypassBuilders.contains(player.getUniqueId())) {
            event.setCancelled(true);
            if (player.hasPermission("global.admin")) {
               this.sendBlockWarningMessage(player, "admin.block_interact_admin");
            } else {
               this.sendBlockWarningMessage(player, "admin.block_interact_player");
            }

         }
      }
   }

   @EventHandler
   public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Player player = (Player)event.getDamager();
         if (this.isInSpawnZone(player)) {
            if (!player.hasPermission("global.admin") || !this.bypassBuilders.contains(player.getUniqueId())) {
               if (this.getConfig().getBoolean("spawn_protection.decorative_entities", true)) {
                  Entity victim = event.getEntity();
                  boolean isDecorative = victim instanceof ArmorStand || victim instanceof ItemFrame || victim instanceof Painting;
                  if (isDecorative) {
                     event.setCancelled(true);
                     if (player.hasPermission("global.admin")) {
                        this.sendBlockWarningMessage(player, "admin.block_entity_damage_admin");
                     } else {
                        this.sendBlockWarningMessage(player, "admin.block_entity_damage_player");
                     }

                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onEntityPickupItem(EntityPickupItemEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         if (this.isInSpawnZone(player)) {
            if (!player.hasPermission("global.admin") || !this.bypassBuilders.contains(player.getUniqueId())) {
               if (this.getConfig().getBoolean("spawn_protection.item_pickup", true)) {
                  event.setCancelled(true);
                  if (this.getConfig().getBoolean("spawn_protection.item_pickup_warning", false)) {
                     if (player.hasPermission("global.admin")) {
                        this.sendBlockWarningMessage(player, "admin.block_pickup_admin");
                     } else {
                        this.sendBlockWarningMessage(player, "admin.block_pickup_player");
                     }
                  }

               }
            }
         }
      }
   }

   @EventHandler(
           priority = EventPriority.LOWEST
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player clicker = (Player)event.getWhoClicked();
         ItemStack current = event.getCurrentItem();
         ItemStack cursor = event.getCursor();
         int hotbarBtn = event.getHotbarButton();
         ItemStack hotbarSwap = hotbarBtn >= 0 && hotbarBtn <= 8 ? clicker.getInventory().getItem(hotbarBtn) : null;
         boolean specialCurrent = current != null && this.playerItemsManager.isAnySpecialItem(current);
         boolean specialCursor = cursor != null && this.playerItemsManager.isAnySpecialItem(cursor);
         boolean specialHotbar = hotbarSwap != null && this.playerItemsManager.isAnySpecialItem(hotbarSwap);
         if (!specialCurrent && !specialCursor && !specialHotbar) {
            FileConfiguration itemsCfg = this.itemsConfigManager.getConfig();
            String menuTitle = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("gui.menu_title", ""));
            String profileTitle = ChatColor.translateAlternateColorCodes('&', itemsCfg.getString("profile.gui.title", "&8Player Profile"));
            int closeSlot = itemsCfg.getInt("profile.gui.close.slot", 49);
            if (event.getView().getTitle().equals(profileTitle) && event.getRawSlot() == closeSlot) {
               event.setCancelled(true);
               Player p = (Player)event.getWhoClicked();
               p.closeInventory();
               p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
            } else if (event.getView().getTitle().equals(profileTitle)) {
               event.setCancelled(true);
               if (current != null && current.hasItemMeta()) {
                  Player player = (Player)event.getWhoClicked();
                  int slot = event.getSlot();
                  if (itemsCfg.getBoolean("profile.gui.items.double_jump_toggle.enabled", true)) {
                     int djSlot = itemsCfg.getInt("profile.gui.items.double_jump_toggle.slot", 31);
                     if (slot == djSlot) {
                        this.playerSettingsManager.toggleDoubleJump(player);
                        boolean nowOn = this.playerSettingsManager.isDoubleJumpEnabled(player);
                        if (!nowOn) {
                           player.setAllowFlight(false);
                           player.setFlying(false);
                        } else if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                           player.setAllowFlight(true);
                        }

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', nowOn ? "&aเปิด Double Jump แล้ว!" : "&cปิด Double Jump แล้ว!"));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, nowOn ? 1.5F : 0.5F);
                        this.openProfileMenu(player);
                        return;
                     }
                  }

                  ConfigurationSection profileItems = itemsCfg.getConfigurationSection("profile.gui.items");
                  if (profileItems != null) {
                     for(String key : profileItems.getKeys(false)) {
                        if (!key.equalsIgnoreCase("double_jump_toggle")) {
                           int itemSlot = itemsCfg.getInt("profile.gui.items." + key + ".slot", -1);
                           if (itemSlot == slot) {
                              String action = itemsCfg.getString("profile.gui.items." + key + ".action", "");
                              if (!action.isEmpty()) {
                                 player.closeInventory();
                                 Bukkit.getScheduler().runTask(this, () -> this.handleProfileAction(player, action));
                              } else {
                                 String link = itemsCfg.getString("profile.gui.items." + key + ".link", "");
                                 if (!link.isEmpty()) {
                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', link));
                                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                                    player.closeInventory();
                                 }
                              }
                              break;
                           }
                        }
                     }
                  }

               }
            } else {
               if (event.getView().getTitle().equals(menuTitle)) {
                  event.setCancelled(true);
                  if (current == null || !current.hasItemMeta()) {
                     return;
                  }

                  Player player = (Player)event.getWhoClicked();
                  int slot = event.getSlot();
                  ConfigurationSection itemsSection = itemsCfg.getConfigurationSection("gui.items");
                  if (itemsSection != null) {
                     for(String key : itemsSection.getKeys(false)) {
                        int itemSlot = itemsCfg.getInt("gui.items." + key + ".slot", -1);
                        if (itemSlot == slot) {
                           player.closeInventory();
                           String action = itemsCfg.getString("gui.items." + key + ".action", "");
                           if (action.startsWith("server:")) {
                              String targetServer = action.substring("server:".length());
                              this.sendToServer(player, targetServer);
                           } else if (!action.isEmpty()) {
                              Bukkit.getScheduler().runTask(this, () -> {
                                 if (player.isOnline()) {
                                    Bukkit.dispatchCommand(player, action);
                                 }

                              });
                           } else {
                              player.sendMessage(this.getGuiMsg("not_configured"));
                           }

                           player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                           break;
                        }
                     }
                  }
               }

            }
         } else {
            event.setCancelled(true);
         }
      }
   }

   @EventHandler(
           priority = EventPriority.LOWEST
   )
   public void onInventoryDragProtect(InventoryDragEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         ItemStack dragged = event.getOldCursor();
         if (dragged != null && this.playerItemsManager.isAnySpecialItem(dragged)) {
            event.setCancelled(true);
         } else {
            for(int rawSlot : event.getRawSlots()) {
               ItemStack slotItem = event.getView().getItem(rawSlot);
               if (slotItem != null && this.playerItemsManager.isAnySpecialItem(slotItem)) {
                  event.setCancelled(true);
                  return;
               }
            }

         }
      }
   }

   @EventHandler(
           priority = EventPriority.LOWEST
   )
   public void onSwapHandItemsProtect(PlayerSwapHandItemsEvent event) {
      ItemStack main = event.getMainHandItem();
      ItemStack off = event.getOffHandItem();
      if (main != null && this.playerItemsManager.isAnySpecialItem(main) || off != null && this.playerItemsManager.isAnySpecialItem(off)) {
         event.setCancelled(true);
      }

   }

   @EventHandler(
           priority = EventPriority.LOWEST
   )
   public void onInventoryMoveProtect(InventoryMoveItemEvent event) {
      if (event.getItem() != null) {
         if (this.playerItemsManager.isAnySpecialItem(event.getItem())) {
            event.setCancelled(true);
         }

      }
   }

   private boolean isInSpawnZone(Player player) {
      return this.spawnLocation == null ? false : player.getWorld().equals(this.spawnLocation.getWorld());
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      if (this.isInSpawnZone(player)) {
         if (!player.hasPermission("global.admin") || !this.bypassBuilders.contains(player.getUniqueId())) {
            event.setCancelled(true);
            if (player.hasPermission("global.admin")) {
               this.sendBlockWarningMessage(player, "admin.block_break_admin");
            } else {
               this.sendBlockWarningMessage(player, "admin.block_break_player");
            }

         }
      }
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      if (this.isInSpawnZone(player)) {
         if (!player.hasPermission("global.admin") || !this.bypassBuilders.contains(player.getUniqueId())) {
            ItemStack item = event.getItemInHand();
            if (item != null && item.getType() == Material.PLAYER_HEAD) {
               event.setCancelled(true);
            } else {
               event.setCancelled(true);
               if (player.hasPermission("global.admin")) {
                  this.sendBlockWarningMessage(player, "admin.block_place_admin");
               } else {
                  this.sendBlockWarningMessage(player, "admin.block_place_player");
               }

            }
         }
      }
   }

   @EventHandler
   public void onLeavesDecay(LeavesDecayEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.leaves_decay", true)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockFade(BlockFadeEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.block_fade", true)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockBurn(BlockBurnEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.block_burn", true)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockSpread(BlockSpreadEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.block_spread", true)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockIgnite(BlockIgniteEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.block_ignite", true)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockFromTo(BlockFromToEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.liquid_flow", false)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onBlockGrow(BlockGrowEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.block_grow", false)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onEntityChangeBlock(EntityChangeBlockEvent event) {
      if (this.getConfig().getBoolean("spawn_protection.entity_change_block", true)) {
         if (this.spawnLocation != null) {
            if (event.getBlock().getWorld().equals(this.spawnLocation.getWorld())) {
               event.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onFoodLevelChange(FoodLevelChangeEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         if (this.isInSpawnZone(player)) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler
   public void onEntityDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         if (!this.isInSpawnZone(player)) {
            return;
         }

         if (event.getCause() == DamageCause.VOID) {
            player.setFallDistance(0.0F);
            if (this.spawnLocation != null) {
               player.teleport(this.spawnLocation);
            }

            event.setCancelled(true);
            return;
         }

         event.setCancelled(true);
      }
   }
}