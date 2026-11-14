package org.kcm.bloomery.fawaymc.global;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;

public class MotdManager implements Listener, CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private FileConfiguration motdConfig;
   private File motdFile;
   private CachedServerIcon serverIcon;
   private final Random random = new Random();
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

   public MotdManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.motdFile = new File(plugin.getDataFolder(), "motd.yml");
      this.loadFromResources(true);
      this.loadServerIcon();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      if (plugin.getCommand("globalmotd") != null) {
         plugin.getCommand("globalmotd").setExecutor(this);
         plugin.getCommand("globalmotd").setTabCompleter(this);
      }

   }

   public void loadFromResources(boolean replace) {
      InputStream resource = this.plugin.getResource("motd.yml");
      if (resource == null) {
         this.plugin.getLogger().severe("[MOTD] ❌ ไม่พบ motd.yml ใน src/main/resources/ — กรุณาสร้างไฟล์ใน resources");
         this.motdConfig = new YamlConfiguration();
      } else {
         try {
            this.plugin.saveResource("motd.yml", replace);
            Logger var10000 = this.plugin.getLogger();
            String var10001 = this.motdFile.getAbsolutePath();
            var10000.info("[MOTD] copy motd.yml จาก resources → " + var10001 + " (replace=" + replace + ")");
         } catch (Exception e) {
            this.plugin.getLogger().warning("[MOTD] saveResource ไม่สำเร็จ (อาจมีไฟล์อยู่แล้ว): " + e.getMessage());
         } finally {
            try {
               resource.close();
            } catch (Exception var15) {
            }

         }

         if (!this.motdFile.exists()) {
            this.plugin.getLogger().severe("[MOTD] ไม่พบ motd.yml ที่ " + this.motdFile.getAbsolutePath());
            this.motdConfig = new YamlConfiguration();
         } else {
            try {
               InputStreamReader reader = new InputStreamReader(new FileInputStream(this.motdFile), StandardCharsets.UTF_8);

               try {
                  this.motdConfig = YamlConfiguration.loadConfiguration(reader);
               } catch (Throwable var16) {
                  try {
                     reader.close();
                  } catch (Throwable var14) {
                     var16.addSuppressed(var14);
                  }

                  throw var16;
               }

               reader.close();
            } catch (Exception e) {
               this.plugin.getLogger().severe("[MOTD] parse ไม่สำเร็จ: " + e.getMessage());
               this.motdConfig = new YamlConfiguration();
               return;
            }

            List<String> line1 = this.motdConfig.getStringList("motd.line_1");
            List<String> line2 = this.motdConfig.getStringList("motd.line_2");
            this.plugin.getLogger().info("[MOTD] === ค่าที่อ่านได้ ===");
            this.plugin.getLogger().info("[MOTD] enabled = " + this.motdConfig.getBoolean("motd.enabled", true));
            this.plugin.getLogger().info("[MOTD] line_1 count = " + line1.size());

            for(int i = 0; i < line1.size(); ++i) {
               this.plugin.getLogger().info("[MOTD] line_1[" + i + "] = " + (String)line1.get(i));
            }

            this.plugin.getLogger().info("[MOTD] line_2 count = " + line2.size());

            for(int i = 0; i < line2.size(); ++i) {
               this.plugin.getLogger().info("[MOTD] line_2[" + i + "] = " + (String)line2.get(i));
            }

         }
      }
   }

   public void reloadMotdConfig() {
      this.loadFromResources(true);
      this.loadServerIcon();
   }

   private void loadServerIcon() {
      this.serverIcon = null;
      if (this.motdConfig.getBoolean("motd.icon.enabled", true)) {
         String iconName = this.motdConfig.getString("motd.icon.file_name", "server-icon.png");
         File iconFile = new File(this.plugin.getDataFolder(), iconName);
         if (!iconFile.exists()) {
            File rootIcon = new File("server-icon.png");
            if (rootIcon.exists()) {
               iconFile = rootIcon;
            }
         }

         if (iconFile.exists()) {
            try {
               this.serverIcon = Bukkit.loadServerIcon(iconFile);
               this.plugin.getLogger().info("[MOTD] โหลด icon สำเร็จ");
            } catch (Exception e) {
               this.plugin.getLogger().warning("[MOTD] โหลด icon ไม่สำเร็จ: " + e.getMessage());
            }
         }

      }
   }

   private String formatColor(String text) {
      if (text == null) {
         return "";
      } else {
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
         return ChatColor.translateAlternateColorCodes('&', buffer.toString());
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onServerListPing(ServerListPingEvent event) {
      if (this.motdConfig.getBoolean("motd.enabled", true)) {
         List<String> line1List = this.motdConfig.getStringList("motd.line_1");
         List<String> line2List = this.motdConfig.getStringList("motd.line_2");
         if (line1List.isEmpty() && line2List.isEmpty()) {
            this.plugin.getLogger().warning("[MOTD] line_1 และ line_2 ว่าง - ไม่ override MOTD");
         } else {
            String line1 = line1List.isEmpty() ? "" : (String)line1List.get(this.random.nextInt(line1List.size()));
            String line2 = line2List.isEmpty() ? "" : (String)line2List.get(this.random.nextInt(line2List.size()));
            String var10000 = this.formatColor(line1);
            String fullMotd = var10000 + "\n" + this.formatColor(line2);
            event.setMotd(fullMotd);
            if (this.motdConfig.getBoolean("motd.custom_max_players.enabled", false)) {
               event.setMaxPlayers(this.motdConfig.getInt("motd.custom_max_players.max", 100));
            }

            if (this.serverIcon != null) {
               event.setServerIcon(this.serverIcon);
            }

         }
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("global.admin")) {
         sender.sendMessage(this.formatColor("&cคุณไม่มีสิทธิ์!"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.formatColor("&e/globalmotd reload &7- copy จาก resources ทับแล้วอ่านใหม่"));
         sender.sendMessage(this.formatColor("&e/globalmotd info &7- ดูค่าปัจจุบัน"));
         return true;
      } else {
         String sub = args[0].toLowerCase();
         if (sub.equals("reload")) {
            this.reloadMotdConfig();
            sender.sendMessage(this.formatColor("&aรีโหลด motd.yml จาก resources สำเร็จ!"));
            if (sender instanceof Player) {
               ((Player)sender).playSound(((Player)sender).getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            }

            return true;
         } else if (!sub.equals("info")) {
            sender.sendMessage(this.formatColor("&cใช้ /globalmotd reload หรือ info"));
            return true;
         } else {
            sender.sendMessage(this.formatColor("&e=== MOTD INFO ==="));
            sender.sendMessage(this.formatColor("&7path: &f" + this.motdFile.getAbsolutePath()));
            sender.sendMessage(this.formatColor("&7enabled: &f" + this.motdConfig.getBoolean("motd.enabled", true)));
            sender.sendMessage(this.formatColor("&7line_1 count: &f" + this.motdConfig.getStringList("motd.line_1").size()));
            sender.sendMessage(this.formatColor("&7line_2 count: &f" + this.motdConfig.getStringList("motd.line_2").size()));
            List<String> l1 = this.motdConfig.getStringList("motd.line_1");

            for(int i = 0; i < l1.size(); ++i) {
               sender.sendMessage(this.formatColor("&7  l1[" + i + "]: &f" + (String)l1.get(i)));
            }

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

         if ("info".startsWith(input)) {
            completions.add("info");
         }
      }

      return completions;
   }
}
