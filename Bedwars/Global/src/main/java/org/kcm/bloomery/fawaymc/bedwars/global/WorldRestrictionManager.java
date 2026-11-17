package org.kcm.bloomery.fawaymc.bedwars.global;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldRestrictionManager implements Listener, CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private FileConfiguration restrictionConfig;
   private File restrictionFile;
   private int freezeTaskId = -1;

   public WorldRestrictionManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.createRestrictionConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      this.startFreezeTask();
      if (plugin.getCommand("worldrestriction") != null) {
         plugin.getCommand("worldrestriction").setExecutor(this);
         plugin.getCommand("worldrestriction").setTabCompleter(this);
      }

   }

   public void createRestrictionConfig() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      this.restrictionFile = new File(this.plugin.getDataFolder(), "worldrestriction.yml");
      if (!this.restrictionFile.exists()) {
         try {
            if (this.restrictionFile.createNewFile()) {
               this.restrictionConfig = YamlConfiguration.loadConfiguration(this.restrictionFile);
               this.restrictionConfig.set("restrictions.disable_nether", true);
               this.restrictionConfig.set("restrictions.disable_end", true);
               this.restrictionConfig.set("restrictions.disable_monsters", true);
               this.restrictionConfig.set("restrictions.disable_all_mobs", true);
               this.restrictionConfig.set("restrictions.disable_hunger", true);
               this.restrictionConfig.set("restrictions.disable_damage", true);
               this.restrictionConfig.set("restrictions.freeze_time", true);
               this.restrictionConfig.set("restrictions.freeze_weather", true);
               List<String> allowedWorlds = new ArrayList();
               allowedWorlds.add("world_survival");
               this.restrictionConfig.set("restrictions.allowed_spawn_worlds", allowedWorlds);
               this.restrictionConfig.set("messages.nether_disabled", "&cระบบมิติ Nether ถูกปิดใช้งานในขณะนี้!");
               this.restrictionConfig.set("messages.end_disabled", "&cระบบมิติ The End ถูกปิดใช้งานในขณะนี้!");
               this.restrictionConfig.set("messages.no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!");
               this.restrictionConfig.set("messages.reload", "&aรีโหลดไฟล์ worldrestriction.yml เรียบร้อยแล้ว!");
               this.restrictionConfig.set("messages.usage", "&eการใช้งาน: /worldrestriction reload");
               this.restrictionConfig.save(this.restrictionFile);
            }
         } catch (IOException e) {
            this.plugin.getLogger().severe("ไม่สามารถสร้างไฟล์ worldrestriction.yml ได้: " + e.getMessage());
         }
      }

      this.restrictionConfig = YamlConfiguration.loadConfiguration(this.restrictionFile);
   }

   public void reloadRestrictionConfig() {
      if (this.restrictionFile == null) {
         this.restrictionFile = new File(this.plugin.getDataFolder(), "worldrestriction.yml");
      }

      if (!this.restrictionFile.exists()) {
         this.createRestrictionConfig();
      } else {
         this.restrictionConfig = YamlConfiguration.loadConfiguration(this.restrictionFile);
      }

      this.startFreezeTask();
   }

   @SuppressWarnings("removal")
   private void startFreezeTask() {
      if (this.freezeTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.freezeTaskId);
      }

      boolean freezeTime = this.restrictionConfig.getBoolean("restrictions.freeze_time", true);
      boolean freezeWeather = this.restrictionConfig.getBoolean("restrictions.freeze_weather", true);
      if (freezeTime || freezeWeather) {
         Set<String> failedTimeWorlds = new HashSet();
         this.freezeTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for(World world : Bukkit.getWorlds()) {
               List<String> allowed = this.restrictionConfig.getStringList("restrictions.allowed_spawn_worlds");
               if (!allowed.contains(world.getName())) {
                  if (freezeTime && !failedTimeWorlds.contains(world.getName())) {
                     try {
                        world.setTime(6000L);
                        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                     } catch (IllegalArgumentException var8) {
                        failedTimeWorlds.add(world.getName());
                        this.plugin.getLogger().warning("[WorldRestriction] World '" + world.getName() + "' does not have a world clock, skipping time freeze.");
                     } catch (Exception var9) {
                     }
                  }

                  if (freezeWeather) {
                     try {
                        world.setStorm(false);
                        world.setThundering(false);
                        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                     } catch (Exception var10) {
                     }
                  }
               }
            }

         }, 0L, 100L).getTaskId();
      }
   }

   @EventHandler
   public void onPlayerPortal(PlayerPortalEvent event) {
      Player player = event.getPlayer();
      World.Environment targetEnv = event.getTo().getWorld().getEnvironment();
      this.checkDimension(player, targetEnv, event);
   }

   @EventHandler
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      if (event.getTo() != null && event.getTo().getWorld() != null) {
         if (event.getCause() != TeleportCause.PLUGIN) {
            Player player = event.getPlayer();
            World.Environment fromEnv = player.getWorld().getEnvironment();
            World.Environment toEnv = event.getTo().getWorld().getEnvironment();
            if (fromEnv != toEnv) {
               this.checkDimension(player, toEnv, event);
            }

         }
      }
   }

   private void checkDimension(Player player, World.Environment targetEnv, Cancellable event) {
      if (targetEnv == Environment.NETHER && this.restrictionConfig.getBoolean("restrictions.disable_nether", true)) {
         event.setCancelled(true);
         String msg = this.restrictionConfig.getString("messages.nether_disabled", "&cระบบมิติ Nether ถูกปิดใช้งานในขณะนี้!");
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (targetEnv == Environment.THE_END && this.restrictionConfig.getBoolean("restrictions.disable_end", true)) {
         event.setCancelled(true);
         String msg = this.restrictionConfig.getString("messages.end_disabled", "&cระบบมิติ The End ถูกปิดใช้งานในขณะนี้!");
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      }

   }

   @EventHandler
   public void onCreatureSpawn(CreatureSpawnEvent event) {
      String worldName = event.getLocation().getWorld().getName();
      List<String> allowedWorlds = this.restrictionConfig.getStringList("restrictions.allowed_spawn_worlds");
      if (!allowedWorlds.contains(worldName)) {
         boolean disableMonsters = this.restrictionConfig.getBoolean("restrictions.disable_monsters", true);
         boolean disableAllMobs = this.restrictionConfig.getBoolean("restrictions.disable_all_mobs", true);
         if (disableAllMobs) {
            event.setCancelled(true);
         } else {
            if (disableMonsters && event.getEntity() instanceof Monster) {
               event.setCancelled(true);
            }

         }
      }
   }

   @EventHandler
   public void onFoodLevelChange(FoodLevelChangeEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         List<String> allowedWorlds = this.restrictionConfig.getStringList("restrictions.allowed_spawn_worlds");
         if (!allowedWorlds.contains(player.getWorld().getName()) && this.restrictionConfig.getBoolean("restrictions.disable_hunger", true)) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler
   public void onEntityDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player)event.getEntity();
         List<String> allowedWorlds = this.restrictionConfig.getStringList("restrictions.allowed_spawn_worlds");
         if (!allowedWorlds.contains(player.getWorld().getName()) && this.restrictionConfig.getBoolean("restrictions.disable_damage", true)) {
            event.setCancelled(true);
         }

      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("global.admin")) {
         sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.restrictionConfig.getString("messages.no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!")));
         return true;
      } else if (args.length != 0 && args[0].equalsIgnoreCase("reload")) {
         this.reloadRestrictionConfig();
         sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.restrictionConfig.getString("messages.reload", "&aรีโหลดไฟล์ worldrestriction.yml เรียบร้อยแล้ว!")));
         if (sender instanceof Player) {
            ((Player)sender).playSound(((Player)sender).getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
         }

         return true;
      } else {
         sender.sendMessage(ChatColor.translateAlternateColorCodes('&', this.restrictionConfig.getString("messages.usage", "&eการใช้งาน: /worldrestriction reload")));
         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (sender.hasPermission("global.admin") && args.length == 1 && "reload".startsWith(args[0].toLowerCase())) {
         completions.add("reload");
      }

      return completions;
   }
}
