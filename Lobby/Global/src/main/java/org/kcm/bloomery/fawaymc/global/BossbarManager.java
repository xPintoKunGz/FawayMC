package org.kcm.bloomery.fawaymc.global;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BossbarManager implements CommandExecutor, TabCompleter, Listener {
   private final JavaPlugin plugin;
   private FileConfiguration barConfig;
   private File barFile;
   private int taskID = -1;
   private int animTaskId = -1;
   private int frameIndex = 0;
   private List<String> animationFrames = new ArrayList();
   private final Map<UUID, BossBar> playerBars = new HashMap();
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

   public BossbarManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.createBarConfig();
      if (plugin.getCommand("bossbar") != null) {
         plugin.getCommand("bossbar").setExecutor(this);
         plugin.getCommand("bossbar").setTabCompleter(this);
      }

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      this.start();
      this.startAnimation();
   }

   public void createBarConfig() {
      this.barFile = new File(this.plugin.getDataFolder(), "bossbar.yml");
      if (!this.barFile.exists()) {
         this.plugin.saveResource("bossbar.yml", false);
      }

      this.barConfig = YamlConfiguration.loadConfiguration(this.barFile);
   }

   public void reloadBarConfig() {
      if (this.barFile == null) {
         this.barFile = new File(this.plugin.getDataFolder(), "bossbar.yml");
      }

      this.barConfig = YamlConfiguration.loadConfiguration(this.barFile);

      try {
         InputStreamReader defConfigStream = new InputStreamReader(this.plugin.getResource("bossbar.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            this.barConfig.setDefaults(defConfig);
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

      for(Player p : Bukkit.getOnlinePlayers()) {
         this.removeBar(p);
      }

      this.playerBars.clear();
      this.frameIndex = 0;
      this.start();
      this.startAnimation();
   }

   public void start() {
      this.stopTaskOnly();
      if (this.barConfig.getBoolean("bossbar.enabled", true)) {
         int updateTicks = this.barConfig.getInt("bossbar.update_ticks", 20);
         this.taskID = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
               this.updateBossBar(player);
            }

         }, 0L, (long)updateTicks).getTaskId();
      }
   }

   public void startAnimation() {
      if (this.animTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.animTaskId);
         this.animTaskId = -1;
      }

      if (this.barConfig.getBoolean("bossbar.enabled", true)) {
         if (this.barConfig.getBoolean("bossbar.animation.enabled", false)) {
            this.animationFrames = this.barConfig.getStringList("bossbar.animation.frames");
            if (!this.animationFrames.isEmpty()) {
               int animTicks = this.barConfig.getInt("bossbar.animation.update_ticks", 10);
               if (animTicks < 1) {
                  animTicks = 1;
               }

               this.animTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                  ++this.frameIndex;
                  if (this.frameIndex >= this.animationFrames.size()) {
                     this.frameIndex = 0;
                  }

                  for(Player player : Bukkit.getOnlinePlayers()) {
                     this.updateBossBar(player);
                  }

               }, (long)animTicks, (long)animTicks).getTaskId();
            }
         }
      }
   }

   private void stopTaskOnly() {
      if (this.taskID != -1) {
         Bukkit.getScheduler().cancelTask(this.taskID);
         this.taskID = -1;
      }

      if (this.animTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.animTaskId);
         this.animTaskId = -1;
      }

   }

   public void stop() {
      this.stopTaskOnly();

      for(Player p : Bukkit.getOnlinePlayers()) {
         this.removeBar(p);
      }

      this.playerBars.clear();
   }

   private void removeBar(Player player) {
      BossBar bar = (BossBar)this.playerBars.remove(player.getUniqueId());
      if (bar != null) {
         bar.removeAll();
      }

   }

   private boolean isInSpawnWorld(Player player) {
      try {
         FileConfiguration global = this.plugin.getConfig();
         String worldName = global.getString("spawn.world");
         return worldName != null && !worldName.isEmpty() ? player.getWorld().getName().equals(worldName) : false;
      } catch (Exception var4) {
         return false;
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.removeBar(event.getPlayer());
   }

   @EventHandler
   public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
      this.updateBossBar(event.getPlayer());
   }

   public void updateBossBar(Player player) {
      if (!this.barConfig.getBoolean("bossbar.enabled", true)) {
         this.removeBar(player);
      } else if (!this.isInSpawnWorld(player)) {
         this.removeBar(player);
      } else {
         String rawTitle;
         if (this.barConfig.getBoolean("bossbar.animation.enabled", false) && !this.animationFrames.isEmpty()) {
            rawTitle = (String)this.animationFrames.get(Math.min(this.frameIndex, this.animationFrames.size() - 1));
         } else {
            rawTitle = this.barConfig.getString("bossbar.title", "&eFawayMC Network");
         }

         String title = this.processText(player, rawTitle);
         String colorName = this.barConfig.getString("bossbar.color", "YELLOW");
         String styleName = this.barConfig.getString("bossbar.style", "SOLID");
         double progress = this.barConfig.getDouble("bossbar.progress", (double)1.0F);
         progress = Math.max((double)0.0F, Math.min((double)1.0F, progress));

         BarColor color;
         try {
            color = BarColor.valueOf(colorName.toUpperCase());
         } catch (Exception var12) {
            color = BarColor.YELLOW;
         }

         BarStyle style;
         try {
            style = BarStyle.valueOf(styleName.toUpperCase());
         } catch (Exception var11) {
            style = BarStyle.SOLID;
         }

         BossBar bar = (BossBar)this.playerBars.get(player.getUniqueId());
         if (bar == null) {
            bar = Bukkit.createBossBar(title, color, style, new BarFlag[0]);
            bar.setProgress(progress);
            bar.addPlayer(player);
            this.playerBars.put(player.getUniqueId(), bar);
         } else {
            if (!bar.getTitle().equals(title)) {
               bar.setTitle(title);
            }

            if (bar.getColor() != color) {
               bar.setColor(color);
            }

            if (bar.getStyle() != style) {
               bar.setStyle(style);
            }

            if (Math.abs(bar.getProgress() - progress) > 0.001) {
               bar.setProgress(progress);
            }

            if (!bar.getPlayers().contains(player)) {
               bar.addPlayer(player);
            }
         }

      }
   }

   private String translateHexColors(String text) {
      if (text != null && !text.isEmpty()) {
         Matcher matcher = HEX_PATTERN.matcher(text);
         StringBuffer buffer = new StringBuffer();

         while(matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");

            for(char c : color.toCharArray()) {
               replacement.append('§').append(c);
            }

            matcher.appendReplacement(buffer, replacement.toString());
         }

         matcher.appendTail(buffer);
         return buffer.toString();
      } else {
         return "";
      }
   }

   private String processText(Player player, String raw) {
      if (raw == null) {
         return "";
      } else {
         String result = PlaceholderManager.setPlaceholders(player, raw);
         result = this.translateHexColors(result);
         result = ChatColor.translateAlternateColorCodes('&', result);
         return result;
      }
   }

   private String getMsg(CommandSender sender, String path, String defaultMsg) {
      String msg = this.barConfig.getString("messages." + path, defaultMsg);
      Player player = sender instanceof Player ? (Player)sender : null;
      return this.processText(player, msg);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("global.admin")) {
         sender.sendMessage(this.getMsg(sender, "no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.getMsg(sender, "usage", "&eการใช้งาน: /bossbar <on|off|reload>"));
         return true;
      } else {
         String sub = args[0].toLowerCase();
         if (sub.equals("on")) {
            this.barConfig.set("bossbar.enabled", true);
            this.saveBarConfig();
            this.start();
            this.startAnimation();

            for(Player p : Bukkit.getOnlinePlayers()) {
               this.updateBossBar(p);
            }

            sender.sendMessage(this.getMsg(sender, "enabled", "&aเปิดใช้งาน BossBar เรียบร้อยแล้ว!"));
            return true;
         } else if (sub.equals("off")) {
            this.barConfig.set("bossbar.enabled", false);
            this.saveBarConfig();
            this.stop();
            sender.sendMessage(this.getMsg(sender, "disabled", "&cปิดใช้งาน BossBar เรียบร้อยแล้ว!"));
            return true;
         } else if (!sub.equals("reload")) {
            sender.sendMessage(this.getMsg(sender, "usage", "&eการใช้งาน: /bossbar <on|off|reload>"));
            return true;
         } else {
            this.reloadBarConfig();

            for(Player p : Bukkit.getOnlinePlayers()) {
               this.updateBossBar(p);
            }

            sender.sendMessage(this.getMsg(sender, "reload", "&aรีโหลดไฟล์ bossbar.yml เรียบร้อยแล้ว!"));
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

   private void saveBarConfig() {
      try {
         this.barConfig.save(this.barFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ bossbar.yml ได้!");
      }

   }
}
