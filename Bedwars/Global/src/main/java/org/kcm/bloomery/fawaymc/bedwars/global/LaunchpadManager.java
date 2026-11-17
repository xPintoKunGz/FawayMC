package org.kcm.bloomery.fawaymc.bedwars.global;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class LaunchpadManager implements Listener, CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private final NamespacedKey launchpadKey;
   private FileConfiguration launchConfig;
   private File launchFile;
   private final Map<UUID, Long> cooldowns = new HashMap();

   public LaunchpadManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.launchpadKey = new NamespacedKey(plugin, "launchpad_item");
      this.createConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      if (plugin.getCommand("launchpad") != null) {
         plugin.getCommand("launchpad").setExecutor(this);
         plugin.getCommand("launchpad").setTabCompleter(this);
      }

   }

   public void createConfig() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      this.launchFile = new File(this.plugin.getDataFolder(), "launchpad.yml");
      if (!this.launchFile.exists()) {
         try {
            if (this.launchFile.createNewFile()) {
               this.launchConfig = new YamlConfiguration();
               this.launchConfig.set("launchpad.enabled", true);
               this.launchConfig.set("launchpad.cooldown_ms", 500L);
               this.launchConfig.set("launchpad.only_in_spawn_world", false);
               this.launchConfig.set("launchpad.custom_launchpads", new ArrayList());
               this.launchConfig.set("launchpad.item.material", "STONE_PRESSURE_PLATE");
               this.launchConfig.set("launchpad.item.name", "&a&lLaunchpad");
               this.launchConfig.set("launchpad.pressure_plate.enabled", true);
               this.launchConfig.set("launchpad.pressure_plate.custom_only", false);
               List<String> plateTypes = new ArrayList();
               plateTypes.add("STONE_PRESSURE_PLATE");
               plateTypes.add("OAK_PRESSURE_PLATE");
               plateTypes.add("SPRUCE_PRESSURE_PLATE");
               plateTypes.add("BIRCH_PRESSURE_PLATE");
               plateTypes.add("JUNGLE_PRESSURE_PLATE");
               plateTypes.add("ACACIA_PRESSURE_PLATE");
               plateTypes.add("DARK_OAK_PRESSURE_PLATE");
               plateTypes.add("MANGROVE_PRESSURE_PLATE");
               plateTypes.add("CHERRY_PRESSURE_PLATE");
               plateTypes.add("BAMBOO_PRESSURE_PLATE");
               plateTypes.add("CRIMSON_PRESSURE_PLATE");
               plateTypes.add("WARPED_PRESSURE_PLATE");
               plateTypes.add("LIGHT_WEIGHTED_PRESSURE_PLATE");
               plateTypes.add("HEAVY_WEIGHTED_PRESSURE_PLATE");
               this.launchConfig.set("launchpad.pressure_plate.block_types", plateTypes);
               this.launchConfig.set("launchpad.pressure_plate.launch_power", (double)1.5F);
               this.launchConfig.set("launchpad.pressure_plate.launch_height", 0.8);
               this.launchConfig.set("launchpad.pressure_plate.use_player_facing", true);
               this.launchConfig.set("launchpad.pressure_plate.require_sneak", false);
               this.launchConfig.set("launchpad.pressure_plate.permission", "");
               this.launchConfig.set("launchpad.pressure_plate.message", "");
               this.launchConfig.set("launchpad.pressure_plate.sound.enabled", true);
               this.launchConfig.set("launchpad.pressure_plate.sound.type", "ENTITY_FIREWORK_ROCKET_LAUNCH");
               this.launchConfig.set("launchpad.pressure_plate.sound.volume", (double)1.0F);
               this.launchConfig.set("launchpad.pressure_plate.sound.pitch", (double)1.0F);
               this.launchConfig.set("launchpad.pressure_plate.particle.enabled", true);
               this.launchConfig.set("launchpad.pressure_plate.particle.type", "FIREWORK");
               this.launchConfig.set("launchpad.pressure_plate.particle.count", 30);
               this.launchConfig.set("launchpad.slime_block.enabled", true);
               this.launchConfig.set("launchpad.slime_block.custom_only", false);
               List<String> slimeTypes = new ArrayList();
               slimeTypes.add("SLIME_BLOCK");
               this.launchConfig.set("launchpad.slime_block.block_types", slimeTypes);
               this.launchConfig.set("launchpad.slime_block.launch_power", (double)0.0F);
               this.launchConfig.set("launchpad.slime_block.launch_height", 1.2);
               this.launchConfig.set("launchpad.slime_block.use_player_facing", false);
               this.launchConfig.set("launchpad.slime_block.min_fall_distance", (double)1.5F);
               this.launchConfig.set("launchpad.slime_block.permission", "");
               this.launchConfig.set("launchpad.slime_block.message", "");
               this.launchConfig.set("launchpad.slime_block.sound.enabled", true);
               this.launchConfig.set("launchpad.slime_block.sound.type", "ENTITY_SLIME_SQUISH");
               this.launchConfig.set("launchpad.slime_block.sound.volume", (double)1.0F);
               this.launchConfig.set("launchpad.slime_block.sound.pitch", (double)1.0F);
               this.launchConfig.set("launchpad.slime_block.particle.enabled", true);
               this.launchConfig.set("launchpad.slime_block.particle.type", "CLOUD");
               this.launchConfig.set("launchpad.slime_block.particle.count", 20);
               this.launchConfig.set("messages.no_permission", "&cคุณไม่มีสิทธิ์!");
               this.launchConfig.set("messages.usage", "&e/launchpad <reload|on|off|info|add|remove|give>");
               this.launchConfig.set("messages.reload", "&aรีโหลด launchpad.yml สำเร็จ!");
               this.launchConfig.set("messages.enabled", "&aเปิดใช้งาน Launchpad แล้ว!");
               this.launchConfig.set("messages.disabled", "&cปิดใช้งาน Launchpad แล้ว!");
               this.launchConfig.save(this.launchFile);
            }
         } catch (IOException e) {
            this.plugin.getLogger().severe("ไม่สามารถสร้าง launchpad.yml ได้: " + e.getMessage());
         }
      }

      this.launchConfig = YamlConfiguration.loadConfiguration(this.launchFile);

      try {
         InputStreamReader reader = new InputStreamReader(this.plugin.getResource("launchpad.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            this.launchConfig.setDefaults(defConfig);
         } catch (Throwable var5) {
            try {
               reader.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         reader.close();
      } catch (Exception var6) {
      }

      Logger var10000 = this.plugin.getLogger();
      boolean var10001 = this.launchConfig.getBoolean("launchpad.pressure_plate.enabled", true);
      var10000.info("[Launchpad] config loaded | plate=" + var10001 + " | slime=" + this.launchConfig.getBoolean("launchpad.slime_block.enabled", true));
   }

   public void reloadConfig() {
      if (this.launchFile == null) {
         this.launchFile = new File(this.plugin.getDataFolder(), "launchpad.yml");
      }

      if (!this.launchFile.exists()) {
         this.createConfig();
      } else {
         this.launchConfig = YamlConfiguration.loadConfiguration(this.launchFile);

         try {
            InputStreamReader reader = new InputStreamReader(this.plugin.getResource("launchpad.yml"), StandardCharsets.UTF_8);

            try {
               YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
               this.launchConfig.setDefaults(defConfig);
            } catch (Throwable var5) {
               try {
                  reader.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }

               throw var5;
            }

            reader.close();
         } catch (Exception var6) {
         }

      }
   }

   public void stop() {
      this.cooldowns.clear();
   }

   private boolean isInSpawnWorld(Player player) {
      if (!this.launchConfig.getBoolean("launchpad.only_in_spawn_world", false)) {
         return true;
      } else {
         try {
            String worldName = this.plugin.getConfig().getString("spawn.world");
            return worldName == null ? false : player.getWorld().getName().equals(worldName);
         } catch (Exception var3) {
            return false;
         }
      }
   }

   private boolean isInCooldown(UUID uuid) {
      long cooldownMs = this.launchConfig.getLong("launchpad.cooldown_ms", 500L);
      if (cooldownMs <= 0L) {
         return false;
      } else {
         Long last = (Long)this.cooldowns.get(uuid);
         if (last == null) {
            return false;
         } else {
            return System.currentTimeMillis() - last < cooldownMs;
         }
      }
   }

   private void setCooldown(UUID uuid) {
      this.cooldowns.put(uuid, System.currentTimeMillis());
   }

   private boolean isBlockType(Material type, List<String> blockTypes) {
      if (type != null && type != Material.AIR) {
         for(String s : blockTypes) {
            Material m = Material.matchMaterial(s.toUpperCase());
            if (m == type) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isCustomLaunchpad(Location loc) {
      List<String> customs = this.launchConfig.getStringList("launchpad.custom_launchpads");
      String var10000 = loc.getWorld().getName();
      String locStr = var10000 + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
      return customs.contains(locStr);
   }

   private Sound resolveSound(String rawName) {
      if (rawName != null && !rawName.isBlank()) {
         String input = rawName.trim();
         String lower = input.toLowerCase();
         if (lower.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(lower);
            if (key != null) {
               Sound s = (Sound)Registry.SOUNDS.get(key);
               if (s != null) {
                  return s;
               }
            }
         }

         Sound s = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(lower));
         if (s != null) {
            return s;
         } else {
            String mapped = this.enumNameToKey(input.toUpperCase());
            if (mapped != null) {
               s = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(mapped));
               if (s != null) {
                  return s;
               }
            }

            s = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(lower.replace('_', '.')));
            if (s != null) {
               return s;
            } else {
               this.plugin.getLogger().warning("[Launchpad] sound not found: " + rawName);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private Particle resolveParticle(String rawName) {
      if (rawName != null && !rawName.isBlank()) {
         String input = rawName.trim();
         String lower = input.toLowerCase();
         if (lower.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(lower);
            if (key != null) {
               Particle p = (Particle)Registry.PARTICLE_TYPE.get(key);
               if (p != null) {
                  return p;
               }
            }
         }

         Particle p = (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(lower));
         if (p != null) {
            return p;
         } else {
            p = (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(lower.replace('_', '.')));
            if (p != null) {
               return p;
            } else {
               this.plugin.getLogger().warning("[Launchpad] particle not found: " + rawName);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private String enumNameToKey(String enumName) {
      switch (enumName) {
         case "ENTITY_FIREWORK_ROCKET_LAUNCH" -> {
            return "entity.firework_rocket.launch";
         }
         case "ENTITY_PLAYER_LEVELUP" -> {
            return "entity.player.levelup";
         }
         case "ENTITY_SLIME_SQUISH" -> {
            return "entity.slime.squish";
         }
         case "BLOCK_NOTE_BLOCK_PLING" -> {
            return "block.note_block.pling";
         }
         case "UI_BUTTON_CLICK" -> {
            return "ui.button.click";
         }
         default -> {
            return null;
         }
      }
   }

   private Vector computeLaunchVelocity(Player player, String section) {
      String path = "launchpad." + section;
      double power = this.launchConfig.getDouble(path + ".launch_power", (double)0.0F);
      double height = this.launchConfig.getDouble(path + ".launch_height", (double)1.0F);
      boolean useFacing = this.launchConfig.getBoolean(path + ".use_player_facing", true);
      if (!useFacing) {
         return new Vector((double)0.0F, height, (double)0.0F);
      } else {
         Vector dir = player.getLocation().getDirection();
         Vector horizontal = new Vector(dir.getX(), (double)0.0F, dir.getZ());
         if (horizontal.lengthSquared() < 0.01) {
            float yaw = player.getLocation().getYaw();
            double yawRad = Math.toRadians((double)yaw);
            horizontal = new Vector(-Math.sin(yawRad), (double)0.0F, Math.cos(yawRad));
         } else {
            horizontal.normalize();
         }

         return horizontal.multiply(power).setY(height);
      }
   }

   private void playEffects(Player player, String section) {
      String path = "launchpad." + section;
      if (this.launchConfig.getBoolean(path + ".sound.enabled", true)) {
         String soundName = this.launchConfig.getString(path + ".sound.type", "");
         float volume = (float)this.launchConfig.getDouble(path + ".sound.volume", (double)1.0F);
         float pitch = (float)this.launchConfig.getDouble(path + ".sound.pitch", (double)1.0F);
         Sound sound = this.resolveSound(soundName);
         if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
         }
      }

      if (this.launchConfig.getBoolean(path + ".particle.enabled", true)) {
         String particleName = this.launchConfig.getString(path + ".particle.type", "");
         int count = this.launchConfig.getInt(path + ".particle.count", 20);
         Particle particle = this.resolveParticle(particleName);
         if (particle != null) {
            player.getWorld().spawnParticle(particle, player.getLocation(), count, (double)0.5F, 0.1, (double)0.5F, 0.1);
         }
      }

      String message = this.launchConfig.getString(path + ".message", "");
      if (message != null && !message.isEmpty()) {
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
      }

   }

   private void launchPlayer(Player player, String section) {
      player.setVelocity(this.computeLaunchVelocity(player, section));
      this.playEffects(player, section);
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.cooldowns.remove(event.getPlayer().getUniqueId());
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      ItemStack item = event.getItemInHand();
      if (item != null && item.hasItemMeta()) {
         if (item.getItemMeta().getPersistentDataContainer().has(this.launchpadKey, PersistentDataType.STRING)) {
            Location loc = event.getBlockPlaced().getLocation();
            String var10000 = loc.getWorld().getName();
            String locStr = var10000 + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            List<String> customs = this.launchConfig.getStringList("launchpad.custom_launchpads");
            if (!customs.contains(locStr)) {
               customs.add(locStr);
               this.launchConfig.set("launchpad.custom_launchpads", customs);
               this.saveConfig();
               event.getPlayer().sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Custom Launchpad placed!");
            }

         }
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      Location loc = event.getBlock().getLocation();
      String var10000 = loc.getWorld().getName();
      String locStr = var10000 + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
      List<String> customs = this.launchConfig.getStringList("launchpad.custom_launchpads");
      if (customs.contains(locStr)) {
         customs.remove(locStr);
         this.launchConfig.set("launchpad.custom_launchpads", customs);
         this.saveConfig();
         loc.getWorld().dropItemNaturally(loc.add((double)0.5F, (double)0.5F, (double)0.5F), this.createLaunchpadItem());
         event.getPlayer().sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Custom Launchpad removed!");
      }

   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getAction() == Action.PHYSICAL) {
         if (event.getClickedBlock() != null) {
            if (this.launchConfig.getBoolean("launchpad.enabled", true)) {
               Player player = event.getPlayer();
               Location blockLoc = event.getClickedBlock().getLocation();
               boolean isCustom = this.isCustomLaunchpad(blockLoc);
               if (!isCustom) {
                  if (this.launchConfig.getBoolean("launchpad.pressure_plate.custom_only", false)) {
                     return;
                  }

                  if (!this.launchConfig.getBoolean("launchpad.pressure_plate.enabled", true)) {
                     return;
                  }

                  Material blockType = event.getClickedBlock().getType();
                  List<String> plateTypes = this.launchConfig.getStringList("launchpad.pressure_plate.block_types");
                  if (!this.isBlockType(blockType, plateTypes)) {
                     return;
                  }
               }

               if (this.isInSpawnWorld(player)) {
                  String perm = this.launchConfig.getString("launchpad.pressure_plate.permission", "");
                  if (perm == null || perm.isEmpty() || player.hasPermission(perm)) {
                     if (isCustom || !this.launchConfig.getBoolean("launchpad.pressure_plate.require_sneak", false) || player.isSneaking()) {
                        if (!this.isInCooldown(player.getUniqueId())) {
                           this.setCooldown(player.getUniqueId());
                           this.launchPlayer(player, "pressure_plate");
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      if (this.launchConfig.getBoolean("launchpad.enabled", true)) {
         if (!(event.getFrom().getY() <= event.getTo().getY())) {
            Player player = event.getPlayer();
            Location belowLoc = event.getTo().clone().subtract((double)0.0F, 0.1, (double)0.0F);
            Block below = belowLoc.getBlock();
            Location blockLoc = below.getLocation();
            boolean isCustom = this.isCustomLaunchpad(blockLoc);
            if (!isCustom) {
               if (this.launchConfig.getBoolean("launchpad.slime_block.custom_only", false)) {
                  return;
               }

               if (!this.launchConfig.getBoolean("launchpad.slime_block.enabled", true)) {
                  return;
               }

               List<String> slimeTypes = this.launchConfig.getStringList("launchpad.slime_block.block_types");
               if (!this.isBlockType(below.getType(), slimeTypes)) {
                  return;
               }
            }

            if (this.isInSpawnWorld(player)) {
               double minFall = this.launchConfig.getDouble("launchpad.slime_block.min_fall_distance", (double)1.5F);
               if (!((double)player.getFallDistance() < minFall)) {
                  String perm = this.launchConfig.getString("launchpad.slime_block.permission", "");
                  if (perm == null || perm.isEmpty() || player.hasPermission(perm)) {
                     if (!this.isInCooldown(player.getUniqueId())) {
                        this.setCooldown(player.getUniqueId());
                        player.setFallDistance(0.0F);
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                           if (player.isOnline()) {
                              this.launchPlayer(player, "slime_block");
                           }

                        });
                     }
                  }
               }
            }
         }
      }
   }

   private ItemStack createLaunchpadItem() {
      Material mat = Material.matchMaterial(this.launchConfig.getString("launchpad.item.material", "STONE_PRESSURE_PLATE"));
      if (mat == null) {
         mat = Material.STONE_PRESSURE_PLATE;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.launchConfig.getString("launchpad.item.name", "&a&lLaunchpad")));
         meta.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', "&7Place this block to create"), ChatColor.translateAlternateColorCodes('&', "&7a custom launchpad!"), "", ChatColor.translateAlternateColorCodes('&', "&ePlace on the ground")));
         meta.getPersistentDataContainer().set(this.launchpadKey, PersistentDataType.STRING, "launchpad");
         item.setItemMeta(meta);
      }

      return item;
   }

   private String getMsg(String path, String def) {
      String msg = this.launchConfig.getString("messages." + path, def);
      return ChatColor.translateAlternateColorCodes('&', msg == null ? def : msg);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("global.admin")) {
         sender.sendMessage(this.getMsg("no_permission", "&cคุณไม่มีสิทธิ์!"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.getMsg("usage", "&e/launchpad <reload|on|off|info|add|remove|give>"));
         return true;
      } else {
         String sub = args[0].toLowerCase();
         if (sub.equals("reload")) {
            this.reloadConfig();
            sender.sendMessage(this.getMsg("reload", "&aรีโหลด launchpad.yml สำเร็จ!"));
            return true;
         } else if (sub.equals("on")) {
            this.launchConfig.set("launchpad.enabled", true);
            this.saveConfig();
            sender.sendMessage(this.getMsg("enabled", "&aเปิดใช้งาน Launchpad แล้ว!"));
            return true;
         } else if (sub.equals("off")) {
            this.launchConfig.set("launchpad.enabled", false);
            this.saveConfig();
            sender.sendMessage(this.getMsg("disabled", "&cปิดใช้งาน Launchpad แล้ว!"));
            return true;
         } else if (sub.equals("give")) {
            if (!(sender instanceof Player)) {
               return true;
            } else {
               Player p = (Player)sender;
               p.getInventory().addItem(new ItemStack[]{this.createLaunchpadItem()});
               p.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ ได้รับไอเท็ม Launchpad แล้ว!");
               return true;
            }
         } else if (sub.equals("add")) {
            if (!(sender instanceof Player)) {
               return true;
            } else {
               Player p = (Player)sender;
               Location loc = p.getLocation().getBlock().getLocation();
               String var15 = loc.getWorld().getName();
               String locStr = var15 + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
               List<String> customs = this.launchConfig.getStringList("launchpad.custom_launchpads");
               if (customs.contains(locStr)) {
                  p.sendMessage(String.valueOf(ChatColor.RED) + "บล็อกนี้เป็น Launchpad อยู่แล้ว!");
                  return true;
               } else {
                  customs.add(locStr);
                  this.launchConfig.set("launchpad.custom_launchpads", customs);
                  this.saveConfig();
                  p.sendMessage(String.valueOf(ChatColor.GREEN) + "เพิ่ม Launchpad ที่ตำแหน่งนี้สำเร็จ!");
                  return true;
               }
            }
         } else if (sub.equals("remove")) {
            if (!(sender instanceof Player)) {
               return true;
            } else {
               Player p = (Player)sender;
               Location loc = p.getLocation().getBlock().getLocation();
               String var10000 = loc.getWorld().getName();
               String locStr = var10000 + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
               List<String> customs = this.launchConfig.getStringList("launchpad.custom_launchpads");
               if (!customs.contains(locStr)) {
                  p.sendMessage(String.valueOf(ChatColor.RED) + "บล็อกนี้ไม่ใช่ Launchpad แบบเฉพาะเจาะจง!");
                  return true;
               } else {
                  customs.remove(locStr);
                  this.launchConfig.set("launchpad.custom_launchpads", customs);
                  this.saveConfig();
                  p.sendMessage(String.valueOf(ChatColor.GREEN) + "ลบ Launchpad ที่ตำแหน่งนี้สำเร็จ!");
                  return true;
               }
            }
         } else if (sub.equals("info")) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "=== Launchpad Debug ===");
            String var10001 = String.valueOf(ChatColor.GRAY);
            sender.sendMessage(var10001 + "custom_launchpads: " + String.valueOf(ChatColor.WHITE) + this.launchConfig.getStringList("launchpad.custom_launchpads").size() + " จุด");
            return true;
         } else {
            sender.sendMessage(this.getMsg("usage", "&e/launchpad <reload|on|off|info|add|remove|give>"));
            return true;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (sender.hasPermission("global.admin") && args.length == 1) {
         String input = args[0].toLowerCase();
         if ("reload".startsWith(input)) {
            completions.add("reload");
         }

         if ("on".startsWith(input)) {
            completions.add("on");
         }

         if ("off".startsWith(input)) {
            completions.add("off");
         }

         if ("info".startsWith(input)) {
            completions.add("info");
         }

         if ("add".startsWith(input)) {
            completions.add("add");
         }

         if ("remove".startsWith(input)) {
            completions.add("remove");
         }

         if ("give".startsWith(input)) {
            completions.add("give");
         }
      }

      return completions;
   }

   private void saveConfig() {
      try {
         this.launchConfig.save(this.launchFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("ไม่สามารถบันทึก launchpad.yml ได้!");
      }

   }
}
