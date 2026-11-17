package org.kcm.bloomery.fawaymc.bedwars.global;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class DoubleJumpManager implements Listener, CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private final PlayerSettingsManager settingsManager;
   private FileConfiguration jumpConfig;
   private File jumpFile;
   private final Map<UUID, Long> lastJumpTime = new HashMap();
   private int groundTaskId = -1;

   public DoubleJumpManager(JavaPlugin plugin, PlayerSettingsManager settingsManager) {
      this.plugin = plugin;
      this.settingsManager = settingsManager;
      this.createJumpConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      if (plugin.getCommand("doublejump") != null) {
         plugin.getCommand("doublejump").setExecutor(this);
         plugin.getCommand("doublejump").setTabCompleter(this);
      }

      this.startGroundTask();
   }

   private void startGroundTask() {
      if (this.groundTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.groundTaskId);
         this.groundTaskId = -1;
      }

      this.groundTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for(Player p : Bukkit.getOnlinePlayers()) {
            if (!this.isVanillaFlightMode(p)) {
               if (!this.shouldHaveFlight(p)) {
                  if (p.getAllowFlight()) {
                     p.setAllowFlight(false);
                  }
               } else {
                  UUID uuid = p.getUniqueId();
                  if (this.isOnSolidGround(p) && !this.isInCooldown(uuid) && !p.getAllowFlight()) {
                     p.setAllowFlight(true);
                  }
               }
            }
         }

      }, 0L, 1L).getTaskId();
   }

   public void stop() {
      if (this.groundTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.groundTaskId);
         this.groundTaskId = -1;
      }

      this.lastJumpTime.clear();
   }

   private boolean isOnSolidGround(Player player) {
      if (player.getVelocity().getY() > 0.1) {
         return false;
      } else if (player.isOnGround()) {
         return true;
      } else {
         Location loc = player.getLocation();
         int blockY = (int)Math.floor(loc.getY() - 0.1);
         Block b = loc.getWorld().getBlockAt(loc.getBlockX(), blockY, loc.getBlockZ());
         return b.getType().isSolid() && !b.isPassable();
      }
   }

   public void createJumpConfig() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      this.jumpFile = new File(this.plugin.getDataFolder(), "doublejump.yml");
      if (!this.jumpFile.exists()) {
         try {
            if (this.jumpFile.createNewFile()) {
               this.jumpConfig = YamlConfiguration.loadConfiguration(this.jumpFile);
               this.jumpConfig.set("double_jump.enabled", true);
               this.jumpConfig.set("double_jump.jump_power", (double)1.0F);
               this.jumpConfig.set("double_jump.jump_height", (double)0.5F);
               this.jumpConfig.set("double_jump.cooldown_seconds", 3);
               this.jumpConfig.set("double_jump.sound.enabled", true);
               this.jumpConfig.set("double_jump.sound.type", "ENTITY_ENDER_DRAGON_FLAP");
               this.jumpConfig.set("double_jump.sound.volume", (double)1.0F);
               this.jumpConfig.set("double_jump.sound.pitch", (double)1.5F);
               this.jumpConfig.set("double_jump.particle.enabled", true);
               this.jumpConfig.set("double_jump.particle.type", "CLOUD");
               this.jumpConfig.set("double_jump.particle.count", 20);
               this.jumpConfig.set("messages.no_permission", "&cคุณไม่มีสิทธิ์!");
               this.jumpConfig.set("messages.usage", "&e/doublejump <on|off|reload>");
               this.jumpConfig.set("messages.enabled", "&aเปิด Double Jump สำเร็จ!");
               this.jumpConfig.set("messages.disabled", "&cปิด Double Jump สำเร็จ!");
               this.jumpConfig.set("messages.reload", "&aรีโหลด doublejump.yml สำเร็จ!");
               this.jumpConfig.save(this.jumpFile);
            }
         } catch (IOException e) {
            this.plugin.getLogger().severe("ไม่สามารถสร้างไฟล์ doublejump.yml ได้: " + e.getMessage());
         }
      }

      this.jumpConfig = YamlConfiguration.loadConfiguration(this.jumpFile);
   }

   public void reloadJumpConfig() {
      if (this.jumpFile == null) {
         this.jumpFile = new File(this.plugin.getDataFolder(), "doublejump.yml");
      }

      if (!this.jumpFile.exists()) {
         this.createJumpConfig();
      } else {
         this.jumpConfig = YamlConfiguration.loadConfiguration(this.jumpFile);
      }

   }

   private boolean isInSpawnWorld(Player player) {
      try {
         FileConfiguration global = this.plugin.getConfig();
         String worldName = global.getString("spawn.world");
         return worldName == null ? false : player.getWorld().getName().equals(worldName);
      } catch (Exception var4) {
         return false;
      }
   }

   private boolean isVanillaFlightMode(Player player) {
      GameMode gm = player.getGameMode();
      return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
   }

   private boolean shouldHaveFlight(Player player) {
      if (this.isVanillaFlightMode(player)) {
         return false;
      } else if (!this.jumpConfig.getBoolean("double_jump.enabled", true)) {
         return false;
      } else {
         return !this.settingsManager.isDoubleJumpEnabled(player) ? false : this.isInSpawnWorld(player);
      }
   }

   private long getCooldownMs() {
      int sec = this.jumpConfig.getInt("double_jump.cooldown_seconds", 3);
      if (sec < 1) {
         sec = 1;
      }

      return (long)sec * 1000L;
   }

   private long getRemainingMs(UUID uuid) {
      Long last = (Long)this.lastJumpTime.get(uuid);
      if (last == null) {
         return 0L;
      } else {
         long elapsed = System.currentTimeMillis() - last;
         return Math.max(0L, this.getCooldownMs() - elapsed);
      }
   }

   private boolean isInCooldown(UUID uuid) {
      return this.getRemainingMs(uuid) > 0L;
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (this.shouldHaveFlight(player)) {
         if (this.isOnSolidGround(player) && !this.isInCooldown(player.getUniqueId())) {
            player.setAllowFlight(true);
         }

         player.setFlying(false);
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.lastJumpTime.remove(event.getPlayer().getUniqueId());
   }

   @EventHandler
   public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
      Player player = event.getPlayer();
      GameMode newMode = event.getNewGameMode();
      if (newMode != GameMode.CREATIVE && newMode != GameMode.SPECTATOR) {
         if (this.shouldHaveFlight(player) && this.isOnSolidGround(player) && !this.isInCooldown(player.getUniqueId())) {
            player.setAllowFlight(true);
         } else {
            player.setAllowFlight(false);
         }

         player.setFlying(false);
      }
   }

   @EventHandler
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      Player player = event.getPlayer();
      this.lastJumpTime.remove(player.getUniqueId());
      if (!this.isVanillaFlightMode(player)) {
         player.setFlying(false);
         if (this.shouldHaveFlight(player) && this.isOnSolidGround(player)) {
            player.setAllowFlight(true);
         } else {
            player.setAllowFlight(false);
         }

      }
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (!this.isVanillaFlightMode(player)) {
         if (player.isFlying()) {
            player.setFlying(false);
         }

      }
   }

   @EventHandler
   public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
      Player player = event.getPlayer();
      if (!this.isVanillaFlightMode(player)) {
         event.setCancelled(true);
         player.setFlying(false);
         if (player.getAllowFlight()) {
            if (!this.shouldHaveFlight(player)) {
               player.setAllowFlight(false);
            } else {
               UUID uuid = player.getUniqueId();
               if (!this.isOnSolidGround(player)) {
                  if (this.isInCooldown(uuid)) {
                     player.setAllowFlight(false);
                     Vector v = player.getVelocity();
                     if (v.getY() > 0.42) {
                        player.setVelocity(new Vector(v.getX(), 0.42, v.getZ()));
                     }

                  } else {
                     player.setAllowFlight(false);
                     this.lastJumpTime.put(uuid, System.currentTimeMillis());
                     double jumpPower = this.jumpConfig.getDouble("double_jump.jump_power", (double)1.0F);
                     double jumpHeight = this.jumpConfig.getDouble("double_jump.jump_height", (double)0.5F);
                     Vector dir = player.getLocation().getDirection();
                     Vector horizontal = new Vector(dir.getX(), (double)0.0F, dir.getZ());
                     if (horizontal.lengthSquared() < 0.01) {
                        float yaw = player.getLocation().getYaw();
                        double yawRad = Math.toRadians((double)yaw);
                        horizontal = new Vector(-Math.sin(yawRad), (double)0.0F, Math.cos(yawRad));
                     } else {
                        horizontal.normalize();
                     }

                     Vector finalVelocity = horizontal.multiply(jumpPower).setY(jumpHeight);
                     player.setVelocity(finalVelocity);
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        if (player.isOnline() && !player.isOnGround()) {
                           player.setFlying(false);
                           player.setVelocity(finalVelocity);
                        }

                     }, 1L);
                     Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        if (player.isOnline() && !player.isOnGround()) {
                           player.setFlying(false);
                           player.setVelocity(finalVelocity);
                        }

                     }, 2L);
                     if (this.jumpConfig.getBoolean("double_jump.sound.enabled", true)) {
                        String soundName = this.jumpConfig.getString("double_jump.sound.type", "ENTITY_ENDER_DRAGON_FLAP");
                        float volume = (float)this.jumpConfig.getDouble("double_jump.sound.volume", (double)1.0F);
                        float pitch = (float)this.jumpConfig.getDouble("double_jump.sound.pitch", (double)1.5F);
                        Sound sound = this.resolveSound(soundName);
                        if (sound != null) {
                           player.playSound(player.getLocation(), sound, volume, pitch);
                        }
                     }

                     if (this.jumpConfig.getBoolean("double_jump.particle.enabled", true)) {
                        String particleName = this.jumpConfig.getString("double_jump.particle.type", "CLOUD");
                        int count = this.jumpConfig.getInt("double_jump.particle.count", 20);
                        Particle particle = this.resolveParticle(particleName);
                        if (particle != null) {
                           Location loc = player.getLocation();
                           player.getWorld().spawnParticle(particle, loc, count, (double)0.5F, 0.1, (double)0.5F, 0.05);
                        }
                     }

                  }
               }
            }
         }
      }
   }

   private Sound resolveSound(String rawName) {
      if (rawName != null && !rawName.isBlank()) {
         String normalized = rawName.trim().toLowerCase().replace('_', '.');
         NamespacedKey key = normalized.contains(":") ? NamespacedKey.fromString(normalized) : NamespacedKey.minecraft(normalized);
         if (key != null) {
            Sound sound = (Sound)Registry.SOUNDS.get(key);
            if (sound != null) {
               return sound;
            }
         }

         return Sound.ENTITY_ENDER_DRAGON_FLAP;
      } else {
         return Sound.ENTITY_ENDER_DRAGON_FLAP;
      }
   }

   private Particle resolveParticle(String rawName) {
      if (rawName != null && !rawName.isBlank()) {
         String normalized = rawName.trim().toLowerCase().replace('_', '.');
         NamespacedKey key = normalized.contains(":") ? NamespacedKey.fromString(normalized) : NamespacedKey.minecraft(normalized);
         if (key != null) {
            Particle p = (Particle)Registry.PARTICLE_TYPE.get(key);
            if (p != null) {
               return p;
            }
         }

         return Particle.CLOUD;
      } else {
         return Particle.CLOUD;
      }
   }

   private String getMsg(String path, String defaultMsg) {
      String msg = this.jumpConfig.getString("messages." + path, defaultMsg);
      return PlaceholderManager.setPlaceholders((Player)null, msg);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("global.admin")) {
         sender.sendMessage(this.getMsg("no_permission", "&cคุณไม่มีสิทธิ์!"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.getMsg("usage", "&e/doublejump <on|off|reload>"));
         return true;
      } else {
         String sub = args[0].toLowerCase();
         if (sub.equals("on")) {
            this.jumpConfig.set("double_jump.enabled", true);
            this.saveJumpConfig();

            for(Player p : Bukkit.getOnlinePlayers()) {
               if (this.shouldHaveFlight(p) && this.isOnSolidGround(p) && !this.isInCooldown(p.getUniqueId())) {
                  p.setAllowFlight(true);
                  p.setFlying(false);
               }
            }

            sender.sendMessage(this.getMsg("enabled", "&aเปิด Double Jump สำเร็จ!"));
            return true;
         } else if (sub.equals("off")) {
            this.jumpConfig.set("double_jump.enabled", false);
            this.saveJumpConfig();

            for(Player p : Bukkit.getOnlinePlayers()) {
               if (!this.isVanillaFlightMode(p)) {
                  p.setAllowFlight(false);
                  p.setFlying(false);
               }
            }

            this.lastJumpTime.clear();
            sender.sendMessage(this.getMsg("disabled", "&cปิด Double Jump สำเร็จ!"));
            return true;
         } else if (sub.equals("reload")) {
            this.reloadJumpConfig();
            sender.sendMessage(this.getMsg("reload", "&aรีโหลด doublejump.yml สำเร็จ!"));
            return true;
         } else {
            sender.sendMessage(this.getMsg("usage", "&e/doublejump <on|off|reload>"));
            return true;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (sender.hasPermission("global.admin") && args.length == 1) {
         String input = args[0].toLowerCase();
         if ("on".startsWith(input)) {
            completions.add("on");
         }

         if ("off".startsWith(input)) {
            completions.add("off");
         }

         if ("reload".startsWith(input)) {
            completions.add("reload");
         }
      }

      return completions;
   }

   private void saveJumpConfig() {
      try {
         this.jumpConfig.save(this.jumpFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ doublejump.yml ได้!");
      }

   }
}
