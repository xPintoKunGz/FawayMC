package org.kcm.bloomery.fawaymc.bedwars.global;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TablistManager implements CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private FileConfiguration tablistConfig;
   private File tablistFile;
   private int taskID = -1;
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

   public TablistManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.createTablistConfig();
      if (plugin.getCommand("tablist") != null) {
         plugin.getCommand("tablist").setExecutor(this);
         plugin.getCommand("tablist").setTabCompleter(this);
      }

   }

   public void createTablistConfig() {
      this.tablistFile = new File(this.plugin.getDataFolder(), "tablist.yml");
      if (!this.tablistFile.exists()) {
         this.plugin.saveResource("tablist.yml", false);
      }

      this.tablistConfig = YamlConfiguration.loadConfiguration(this.tablistFile);
   }

   public void reloadTablistConfig() {
      if (this.tablistFile == null) {
         this.tablistFile = new File(this.plugin.getDataFolder(), "tablist.yml");
      }

      this.tablistConfig = YamlConfiguration.loadConfiguration(this.tablistFile);

      try {
         InputStreamReader defConfigStream = new InputStreamReader(this.plugin.getResource("tablist.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            this.tablistConfig.setDefaults(defConfig);
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

   public void start() {
      this.stopTaskOnly();
      if (this.tablistConfig.getBoolean("tablist.enabled", true)) {
         int updateTicks = this.tablistConfig.getInt("tablist.update_ticks", 20);
         this.taskID = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
               this.updateTablist(player);
            }

         }, 0L, (long)updateTicks).getTaskId();
      }
   }

   private void stopTaskOnly() {
      if (this.taskID != -1) {
         Bukkit.getScheduler().cancelTask(this.taskID);
         this.taskID = -1;
      }

   }

   public void stop() {
      this.stopTaskOnly();

      for(Player player : Bukkit.getOnlinePlayers()) {
         player.setPlayerListHeaderFooter("", "");
      }

   }

   public void updateTablist(Player player) {
      if (this.tablistConfig.getBoolean("tablist.enabled", true)) {
         List<String> headerList = this.tablistConfig.getStringList("tablist.header");
         List<String> footerList = this.tablistConfig.getStringList("tablist.footer");
         String header = this.formatList(player, headerList);
         String footer = this.formatList(player, footerList);
         player.setPlayerListHeaderFooter(header, footer);
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

   private String formatList(Player player, List<String> list) {
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < list.size(); ++i) {
         sb.append(this.processText(player, (String)list.get(i)));
         if (i < list.size() - 1) {
            sb.append("\n");
         }
      }

      return sb.toString();
   }

   private String getMsg(CommandSender sender, String path, String defaultMsg) {
      String msg = this.tablistConfig.getString("messages." + path, defaultMsg);
      Player player = sender instanceof Player ? (Player)sender : null;
      return this.processText(player, msg);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("global.admin")) {
         sender.sendMessage(this.getMsg(sender, "no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.getMsg(sender, "usage", "&eการใช้งาน: /tablist <on|off|reload>"));
         return true;
      } else {
         String subCommand = args[0].toLowerCase();
         if (subCommand.equals("on")) {
            this.tablistConfig.set("tablist.enabled", true);
            this.saveTablistConfig();
            this.start();
            sender.sendMessage(this.getMsg(sender, "enabled", "&aเปิดใช้งาน Tablist เรียบร้อยแล้ว!"));
            return true;
         } else if (subCommand.equals("off")) {
            this.tablistConfig.set("tablist.enabled", false);
            this.saveTablistConfig();
            this.stop();
            sender.sendMessage(this.getMsg(sender, "disabled", "&cปิดใช้งาน Tablist เรียบร้อยแล้ว!"));
            return true;
         } else if (subCommand.equals("reload")) {
            this.reloadTablistConfig();
            this.start();
            sender.sendMessage(this.getMsg(sender, "reload", "&aรีโหลดไฟล์ tablist.yml เรียบร้อยแล้ว!"));
            return true;
         } else {
            sender.sendMessage(this.getMsg(sender, "usage", "&eการใช้งาน: /tablist <on|off|reload>"));
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

   private void saveTablistConfig() {
      try {
         this.tablistConfig.save(this.tablistFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ tablist.yml ได้!");
      }

   }
}
