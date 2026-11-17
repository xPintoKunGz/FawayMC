package org.kcm.bloomery.fawaymc.global;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
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
import org.bukkit.scoreboard.DisplaySlot;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager implements CommandExecutor, TabCompleter {
   private final JavaPlugin plugin;
   private FileConfiguration scoreboardConfig;
   private File scoreboardFile;
   private final Map<UUID, Scoreboard> activeBoards = new HashMap<>();
   private final Map<UUID, Integer> playerPageIndex = new HashMap<>();
   private final Map<UUID, Long> playerLastSwitch = new HashMap<>();
   private final List<ScoreboardPage> cachedPages = new ArrayList<>();
   private int taskID = -1;
   private int animationTaskId = -1;
   private int animationFrame = 0;
   private List<String> cachedFrames = null;
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
   private static final int PREFIX_SUFFIX_LIMIT = 64;
   private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
   private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

   public ScoreboardManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.createScoreboardConfig();
      if (plugin.getCommand("scoreboard") != null) {
         plugin.getCommand("scoreboard").setExecutor(this);
         plugin.getCommand("scoreboard").setTabCompleter(this);
      }

   }

   public void createScoreboardConfig() {
      this.scoreboardFile = new File(this.plugin.getDataFolder(), "scoreboard.yml");
      if (!this.scoreboardFile.exists()) {
         this.plugin.saveResource("scoreboard.yml", false);
      }

      this.scoreboardConfig = YamlConfiguration.loadConfiguration(this.scoreboardFile);

      try {
         InputStreamReader reader = new InputStreamReader(this.plugin.getResource("scoreboard.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            this.scoreboardConfig.setDefaults(defConfig);
         } catch (Throwable var11) {
            try {
               reader.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }

            throw var11;
         }

         reader.close();
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Scoreboard] โหลด defaults จาก resources ไม่สำเร็จ: " + e.getMessage());
      }

      YamlConfiguration rawCheck = YamlConfiguration.loadConfiguration(this.scoreboardFile);
      if (!rawCheck.contains("title_animation.enabled")) {
         this.plugin.getLogger().info("[Scoreboard] ไม่พบ title_animation ใน scoreboard.yml — เพิ่มค่าเริ่มต้นให้อัตโนมัติ");
         this.scoreboardConfig.set("title_animation.enabled", true);
         this.scoreboardConfig.set("title_animation.mode", "frames");
         this.scoreboardConfig.set("title_animation.interval_ticks", 3);
         List<String> frames = new ArrayList<>();
         frames.add("&d&lF&b&la&d&lwayMC");
         frames.add("&d&lFa&bb&d&layMC");
         frames.add("&d&lFaw&ba&d&lyMC");
         frames.add("&d&lFawa&bb&d&lyMC");
         frames.add("&d&lFaway&bb&d&lMC");
         frames.add("&d&lFawayM&bb&d&lC");
         this.scoreboardConfig.set("title_animation.frames", frames);
         this.scoreboardConfig.set("title_animation.scroll_text", "&d&lFawayMC Network");
         this.scoreboardConfig.set("title_animation.scroll_padding", 8);

         try {
            this.scoreboardConfig.save(this.scoreboardFile);
            this.plugin.getLogger().info("[Scoreboard] บันทึก title_animation ลง scoreboard.yml สำเร็จ");
         } catch (IOException e) {
            this.plugin.getLogger().severe("[Scoreboard] บันทึก scoreboard.yml ไม่สำเร็จ: " + e.getMessage());
         }

         this.scoreboardConfig = YamlConfiguration.loadConfiguration(this.scoreboardFile);

         try {
            InputStreamReader reader = new InputStreamReader(this.plugin.getResource("scoreboard.yml"), StandardCharsets.UTF_8);

            try {
               YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
               this.scoreboardConfig.setDefaults(defConfig);
            } catch (Throwable var7) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            reader.close();
         } catch (Exception var8) {
         }
      }

      Logger var10000 = this.plugin.getLogger();
      boolean var10001 = this.scoreboardConfig.getBoolean("scoreboard.enabled", true);
      var10000.info("[Scoreboard] config loaded | enabled=" + var10001 + " | anim=" + this.scoreboardConfig.getBoolean("title_animation.enabled", false) + " | mode=" + this.scoreboardConfig.getString("title_animation.mode", "frames"));
   }

   public void reloadScoreboardConfig() {
      if (this.scoreboardFile == null) {
         this.scoreboardFile = new File(this.plugin.getDataFolder(), "scoreboard.yml");
      }

      this.scoreboardConfig = YamlConfiguration.loadConfiguration(this.scoreboardFile);

      try {
         InputStreamReader defConfigStream = new InputStreamReader(this.plugin.getResource("scoreboard.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            this.scoreboardConfig.setDefaults(defConfig);
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

      this.cachedPages.clear();
      this.playerPageIndex.clear();
      this.playerLastSwitch.clear();
      Logger var10000 = this.plugin.getLogger();
      boolean var10001 = this.scoreboardConfig.getBoolean("title_animation.enabled", false);
      var10000.info("[Scoreboard] config reloaded | anim.enabled=" + var10001 + " | anim.mode=" + this.scoreboardConfig.getString("title_animation.mode", "frames"));
   }

   public void start() {
      this.stopTaskOnly();
      this.stopAnimationTask();
      this.cachedFrames = null;
      this.animationFrame = 0;
      this.cachedPages.clear();
      if (!this.scoreboardConfig.getBoolean("scoreboard.enabled", true)) {
         this.plugin.getLogger().info("[Scoreboard] ปิดใช้งาน (scoreboard.enabled=false)");
      } else {
         this.loadPages();
         int updateTicks = this.scoreboardConfig.getInt("scoreboard.update_ticks", 20);
         this.taskID = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
               this.updateScoreboard(player);
            }

         }, 0L, (long)updateTicks).getTaskId();
         this.startAnimationTask();
      }
   }

   private void stopTaskOnly() {
      if (this.taskID != -1) {
         Bukkit.getScheduler().cancelTask(this.taskID);
         this.taskID = -1;
      }

   }

   private void loadPages() {
      this.cachedPages.clear();
      if (this.scoreboardConfig.contains("scoreboard.pages")) {
         for(Map<?, ?> map : this.scoreboardConfig.getMapList("scoreboard.pages")) {
            String title = (String)map.get("title");
            List<String> lines = (List)map.get("lines");
            if (title != null && lines != null) {
               this.cachedPages.add(new ScoreboardPage(title, lines));
            }
         }
      }

      if (this.cachedPages.isEmpty()) {
         String title = this.scoreboardConfig.getString("scoreboard.title", "&6&lFawayMC");
         List<String> lines = this.scoreboardConfig.getStringList("scoreboard.lines");
         this.cachedPages.add(new ScoreboardPage(title, lines));
      }

   }

   private void startAnimationTask() {
      this.stopAnimationTask();
      this.cachedFrames = null;
      this.animationFrame = 0;
      if (this.scoreboardConfig.getBoolean("scoreboard.enabled", true)) {
         if (this.scoreboardConfig.getBoolean("title_animation.enabled", false)) {
            this.cachedFrames = this.buildAnimationFrames();
            if (this.cachedFrames != null && !this.cachedFrames.isEmpty()) {
               int interval = Math.max(1, this.scoreboardConfig.getInt("title_animation.interval_ticks", 3));
               this.animationTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                  if (this.cachedFrames != null && !this.cachedFrames.isEmpty()) {
                     this.animationFrame = (this.animationFrame + 1) % this.cachedFrames.size();
                     String rawFrame = (String)this.cachedFrames.get(this.animationFrame);

                     for(Player player : Bukkit.getOnlinePlayers()) {
                        Scoreboard board = (Scoreboard)this.activeBoards.get(player.getUniqueId());
                        if (board != null) {
                           Objective objective = board.getObjective("faway_sb");
                           if (objective != null) {
                              String title = this.processText(player, rawFrame);
                              if (!objective.getDisplayName().equals(title)) {
                                 objective.setDisplayName(title);
                              }
                           }
                        }
                     }

                  }
               }, 0L, (long)interval).getTaskId();
            } else {
               this.cachedFrames = null;
            }
         }
      }
   }

   private void stopAnimationTask() {
      if (this.animationTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.animationTaskId);
         this.animationTaskId = -1;
      }

   }

   private List<String> buildAnimationFrames() {
      List<String> frames = new ArrayList<>();
      String mode = this.scoreboardConfig.getString("title_animation.mode", "frames");
      if ("scroll".equalsIgnoreCase(mode)) {
         String base = this.scoreboardConfig.getString("title_animation.scroll_text", "FawayMC Network");
         int padding = Math.max(0, this.scoreboardConfig.getInt("title_animation.scroll_padding", 5));
         String colorPrefix = this.extractColorPrefix(base);
         String plainText = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', base));
         if (plainText == null) {
            plainText = "";
         }

         StringBuilder sb = new StringBuilder(plainText);

         for(int i = 0; i < padding; ++i) {
            sb.append(' ');
         }

         String padded = sb.toString();
         if (padded.isEmpty()) {
            return frames;
         }

         for(int i = 0; i < padded.length(); ++i) {
            String var10000 = padded.substring(i);
            String rotated = var10000 + padded.substring(0, i);
            frames.add(colorPrefix + rotated);
         }
      } else {
         List<String> cfgFrames = this.scoreboardConfig.getStringList("title_animation.frames");
         if (cfgFrames.isEmpty()) {
            String fallback = this.scoreboardConfig.getString("scoreboard.title", "&d&lFawayMC");
            frames.add(fallback);
         } else {
            frames.addAll(cfgFrames);
         }
      }

      return frames;
   }

   private String extractColorPrefix(String text) {
      if (text == null) {
         return "";
      } else {
         StringBuilder prefix = new StringBuilder();

         for(int i = 0; i < text.length(); i += 2) {
            char c = text.charAt(i);
            if (c != 167 && c != '&' || i + 1 >= text.length()) {
               break;
            }

            prefix.append('§').append(text.charAt(i + 1));
         }

         return prefix.toString();
      }
   }

   public void stop() {
      this.stopTaskOnly();
      this.stopAnimationTask();
      this.cachedFrames = null;
      this.cachedPages.clear();
      this.playerPageIndex.clear();
      this.playerLastSwitch.clear();

      for(Player player : Bukkit.getOnlinePlayers()) {
         player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
      }

      this.activeBoards.clear();
   }

   public void updateScoreboard(Player player) {
      if (this.scoreboardConfig.getBoolean("scoreboard.enabled", true)) {
         if (!this.cachedPages.isEmpty()) {
            org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager != null) {
               Scoreboard board = (Scoreboard)this.activeBoards.get(player.getUniqueId());
               if (board == null) {
                  board = manager.getNewScoreboard();
                  this.activeBoards.put(player.getUniqueId(), board);
               }

               int pageSwitchSecs = this.scoreboardConfig.getInt("scoreboard.page_switch_seconds", 10);
               long now = System.currentTimeMillis();
               if (!this.playerLastSwitch.containsKey(player.getUniqueId())) {
                  this.playerLastSwitch.put(player.getUniqueId(), now);
               }

               long lastSwitch = (Long)this.playerLastSwitch.get(player.getUniqueId());
               int currentIndex = (Integer)this.playerPageIndex.getOrDefault(player.getUniqueId(), 0);
               if (now - lastSwitch >= (long)pageSwitchSecs * 1000L) {
                  currentIndex = (currentIndex + 1) % this.cachedPages.size();
                  this.playerPageIndex.put(player.getUniqueId(), currentIndex);
                  this.playerLastSwitch.put(player.getUniqueId(), now);
               }

               ScoreboardPage currentPage = (ScoreboardPage)this.cachedPages.get(currentIndex);
               boolean animEnabled = this.scoreboardConfig.getBoolean("title_animation.enabled", false);
               String title;
               if (animEnabled && this.cachedFrames != null && !this.cachedFrames.isEmpty()) {
                  int idx = this.animationFrame % this.cachedFrames.size();
                  title = this.processText(player, (String)this.cachedFrames.get(idx));
               } else {
                  title = this.processText(player, currentPage.title);
               }

               Objective objective = board.getObjective("faway_sb");
               if (objective == null) {
                  objective = board.registerNewObjective("faway_sb", "dummy", title);
                  objective.setDisplaySlot(DisplaySlot.SIDEBAR);
               } else if (!objective.getDisplayName().equals(title)) {
                  objective.setDisplayName(title);
               }

               try {
                  objective.numberFormat(NumberFormat.blank());
               } catch (Throwable ignored) {}

               List<String> lines = currentPage.lines;
               int size = lines.size();

               for(int i = 0; i < size; ++i) {
                  int scoreIndex = size - i;
                  String rawLine = (String)lines.get(i);
                  String translatedLine = this.processText(player, rawLine);
                  String teamName = "line_" + i;
                  Team team = board.getTeam(teamName);
                  if (team == null) {
                     team = board.registerNewTeam(teamName);
                  }

                  String entry = this.getUniqueChatColor(i);
                  if (!team.hasEntry(entry)) {
                     team.addEntry(entry);
                  }

                  String[] splitResult = this.splitStringWithColor(translatedLine, 64);
                  team.setPrefix(splitResult[0]);
                  team.setSuffix(splitResult[1]);
                  objective.getScore(entry).setScore(scoreIndex);
               }

               for(String entry : board.getEntries()) {
                  boolean found = false;

                  for(int i = 0; i < size; ++i) {
                     if (entry.equals(this.getUniqueChatColor(i))) {
                        found = true;
                        break;
                     }
                  }

                  if (!found) {
                     board.resetScores(entry);
                  }
               }

               player.setScoreboard(board);
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
         Date now = new Date();
         String result = raw.replace("{date}", this.dateFormat.format(now)).replace("{time}", this.timeFormat.format(now));
         result = PlaceholderManager.setPlaceholders(player, result);
         result = this.translateHexColors(result);
         result = ChatColor.translateAlternateColorCodes('&', result);
         return result;
      }
   }

   private String[] splitStringWithColor(String text, int limit) {
      if (text.length() <= limit) {
         return new String[]{text, ""};
      } else {
         String prefix = text.substring(0, limit);
         String lastColors = ChatColor.getLastColors(prefix);
         String suffix = lastColors + text.substring(limit);
         if (suffix.length() > limit) {
            suffix = suffix.substring(0, limit);
         }

         return new String[]{prefix, suffix};
      }
   }

   private String getMsg(CommandSender sender, String path, String defaultMsg) {
      String msg = this.scoreboardConfig.getString("messages." + path, defaultMsg);
      Player player = sender instanceof Player ? (Player)sender : null;
      return this.processText(player, msg);
   }

   private String getUniqueChatColor(int index) {
      ChatColor[] colors = ChatColor.values();
      StringBuilder sb = new StringBuilder();
      sb.append(colors[index % colors.length]);
      sb.append(colors[(index + 1) % colors.length]);
      return sb.toString();
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("global.admin")) {
         sender.sendMessage(this.getMsg(sender, "no_permission", "&cคุณไม่มีสิทธิ์!"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.getMsg(sender, "usage", "&e/scoreboard <on|off|reload|info>"));
         return true;
      } else {
         String subCommand = args[0].toLowerCase();
         if (subCommand.equals("on")) {
            this.scoreboardConfig.set("scoreboard.enabled", true);
            this.saveScoreboardConfig();
            this.start();
            sender.sendMessage(this.getMsg(sender, "enabled", "&aเปิด Scoreboard สำเร็จ!"));
            return true;
         } else if (subCommand.equals("off")) {
            this.scoreboardConfig.set("scoreboard.enabled", false);
            this.saveScoreboardConfig();
            this.stop();
            sender.sendMessage(this.getMsg(sender, "disabled", "&cปิด Scoreboard สำเร็จ!"));
            return true;
         } else if (subCommand.equals("reload")) {
            this.reloadScoreboardConfig();
            this.start();
            sender.sendMessage(this.getMsg(sender, "reload", "&aรีโหลด scoreboard.yml สำเร็จ!"));
            return true;
         } else if (subCommand.equals("info")) {
            boolean sbEnabled = this.scoreboardConfig.getBoolean("scoreboard.enabled", true);
            boolean animEnabled = this.scoreboardConfig.getBoolean("title_animation.enabled", false);
            int framesCount = this.cachedFrames == null ? -1 : this.cachedFrames.size();
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "=== Scoreboard Debug ===");
            String var10001 = String.valueOf(ChatColor.GRAY);
            sender.sendMessage(var10001 + "scoreboard.enabled: " + String.valueOf(ChatColor.WHITE) + sbEnabled);
            var10001 = String.valueOf(ChatColor.GRAY);
            sender.sendMessage(var10001 + "pages loaded: " + String.valueOf(ChatColor.WHITE) + this.cachedPages.size());
            var10001 = String.valueOf(ChatColor.GRAY);
            sender.sendMessage(var10001 + "page_switch_seconds: " + String.valueOf(ChatColor.WHITE) + this.scoreboardConfig.getInt("scoreboard.page_switch_seconds", 10) + "s");
            var10001 = String.valueOf(ChatColor.GRAY);
            sender.sendMessage(var10001 + "title_animation.enabled: " + String.valueOf(ChatColor.WHITE) + (animEnabled ? String.valueOf(ChatColor.GREEN) + animEnabled : String.valueOf(ChatColor.RED) + animEnabled));
            var10001 = String.valueOf(ChatColor.GRAY);
            sender.sendMessage(var10001 + "cachedFrames: " + String.valueOf(ChatColor.WHITE) + (framesCount == -1 ? String.valueOf(ChatColor.RED) + "null" : String.valueOf(ChatColor.GREEN) + framesCount + " frames"));
            return true;
         } else {
            sender.sendMessage(this.getMsg(sender, "usage", "&e/scoreboard <on|off|reload|info>"));
            return true;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList<>();
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

         if ("info".startsWith(input)) {
            completions.add("info");
         }
      }

      return completions;
   }

   private void saveScoreboardConfig() {
      try {
         this.scoreboardConfig.save(this.scoreboardFile);
      } catch (IOException var2) {
         this.plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ scoreboard.yml ได้!");
      }

   }

   public void clearPlayer(Player player) {
      if (player != null) {
         UUID uuid = player.getUniqueId();
         this.playerPageIndex.remove(uuid);
         this.playerLastSwitch.remove(uuid);
         this.activeBoards.remove(uuid);
      }
   }

   private static class ScoreboardPage {
      String title;
      List<String> lines;

      ScoreboardPage(String title, List<String> lines) {
         this.title = title;
         this.lines = lines;
      }
   }
}