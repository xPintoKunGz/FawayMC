package org.kcm.bloomery.fawaymc.global;

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
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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

public class ScoreboardManager implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private FileConfiguration scoreboardConfig;
    private File scoreboardFile;
    private final Map<UUID, Scoreboard> activeBoards = new HashMap<>();
    private int taskID = -1;

    private int animationTaskId = -1;
    private int animationFrame = 0;
    private List<String> cachedFrames = null;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final int PREFIX_SUFFIX_LIMIT = 64;

    public ScoreboardManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createScoreboardConfig();

        if (plugin.getCommand("scoreboard") != null) {
            plugin.getCommand("scoreboard").setExecutor(this);
            plugin.getCommand("scoreboard").setTabCompleter(this);
        }
    }

    /**
     * ★ แก้ไขหลัก: merge defaults จาก resource file เสมอ
     * เพื่อให้ config ที่มีอยู่แล้ว (แต่ไม่มี key ใหม่) ใช้ค่าจาก resource ได้
     */
    public void createScoreboardConfig() {
        scoreboardFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!scoreboardFile.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);

        // merge defaults จาก resource
        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("scoreboard.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            scoreboardConfig.setDefaults(defConfig);
        } catch (Exception e) {
            plugin.getLogger().warning("[Scoreboard] โหลด defaults จาก resources ไม่สำเร็จ: " + e.getMessage());
        }

        // ★★★ AUTO-ADD: ถ้าไม่มี title_animation ในไฟล์ → เขียนให้ ★★★
        YamlConfiguration rawCheck = YamlConfiguration.loadConfiguration(scoreboardFile);
        if (!rawCheck.contains("title_animation.enabled")) {
            plugin.getLogger().info("[Scoreboard] ไม่พบ title_animation ใน scoreboard.yml — เพิ่มค่าเริ่มต้นให้อัตโนมัติ");

            scoreboardConfig.set("title_animation.enabled", true);
            scoreboardConfig.set("title_animation.mode", "frames");
            scoreboardConfig.set("title_animation.interval_ticks", 3);

            List<String> frames = new ArrayList<>();
            frames.add("&d&lF&b&la&d&lwayMC");
            frames.add("&d&lFa&bb&d&layMC");
            frames.add("&d&lFaw&ba&d&lyMC");
            frames.add("&d&lFawa&bb&d&lyMC");
            frames.add("&d&lFaway&bb&d&lMC");
            frames.add("&d&lFawayM&bb&d&lC");
            scoreboardConfig.set("title_animation.frames", frames);

            scoreboardConfig.set("title_animation.scroll_text", "&d&lFawayMC Network");
            scoreboardConfig.set("title_animation.scroll_padding", 8);

            try {
                scoreboardConfig.save(scoreboardFile);
                plugin.getLogger().info("[Scoreboard] บันทึก title_animation ลง scoreboard.yml สำเร็จ");
            } catch (IOException e) {
                plugin.getLogger().severe("[Scoreboard] บันทึก scoreboard.yml ไม่สำเร็จ: " + e.getMessage());
            }

            // reload เพื่อให้ค่าใหม่มีผล
            scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);
            try (InputStreamReader reader = new InputStreamReader(
                    plugin.getResource("scoreboard.yml"), StandardCharsets.UTF_8)) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
                scoreboardConfig.setDefaults(defConfig);
            } catch (Exception ignored) {}
        }

        plugin.getLogger().info("[Scoreboard] config loaded | scoreboard.enabled="
                + scoreboardConfig.getBoolean("scoreboard.enabled", true)
                + " | title_animation.enabled=" + scoreboardConfig.getBoolean("title_animation.enabled", false)
                + " | mode=" + scoreboardConfig.getString("title_animation.mode", "frames"));
    }

    public void reloadScoreboardConfig() {
        if (scoreboardFile == null) {
            scoreboardFile = new File(plugin.getDataFolder(), "scoreboard.yml");
        }
        scoreboardConfig = YamlConfiguration.loadConfiguration(scoreboardFile);

        try (InputStreamReader defConfigStream = new InputStreamReader(plugin.getResource("scoreboard.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            scoreboardConfig.setDefaults(defConfig);
        } catch (Exception ignored) {}

        plugin.getLogger().info("[Scoreboard] config reloaded | anim.enabled="
                + scoreboardConfig.getBoolean("title_animation.enabled", false)
                + " | anim.mode=" + scoreboardConfig.getString("title_animation.mode", "frames"));
    }

    public void start() {
        stopTaskOnly();
        stopAnimationTask();
        cachedFrames = null;
        animationFrame = 0;

        if (!scoreboardConfig.getBoolean("scoreboard.enabled", true)) {
            plugin.getLogger().info("[Scoreboard] ปิดใช้งาน (scoreboard.enabled=false)");
            return;
        }

        int updateTicks = scoreboardConfig.getInt("scoreboard.update_ticks", 20);

        taskID = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 0L, updateTicks).getTaskId();

        startAnimationTask();
    }

    private void stopTaskOnly() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    // ===== Animation =====

    private void startAnimationTask() {
        stopAnimationTask();
        cachedFrames = null;
        animationFrame = 0;

        if (!scoreboardConfig.getBoolean("scoreboard.enabled", true)) {
            plugin.getLogger().info("[Anim] ไม่เริ่ม — scoreboard.enabled=false");
            return;
        }

        if (!scoreboardConfig.getBoolean("title_animation.enabled", false)) {
            plugin.getLogger().info("[Anim] ไม่เริ่ม — title_animation.enabled=false (แก้ใน scoreboard.yml)");
            return;
        }

        cachedFrames = buildAnimationFrames();
        if (cachedFrames == null || cachedFrames.isEmpty()) {
            plugin.getLogger().warning("[Anim] ไม่เริ่ม — buildAnimationFrames() ว่างเปล่า (mode="
                    + scoreboardConfig.getString("title_animation.mode", "frames") + ")");
            cachedFrames = null;
            return;
        }

        int interval = Math.max(1, scoreboardConfig.getInt("title_animation.interval_ticks", 3));

        animationTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (cachedFrames == null || cachedFrames.isEmpty()) return;
            animationFrame = (animationFrame + 1) % cachedFrames.size();
            String rawFrame = cachedFrames.get(animationFrame);

            for (Player player : Bukkit.getOnlinePlayers()) {
                Scoreboard board = activeBoards.get(player.getUniqueId());
                if (board == null) continue;
                Objective objective = board.getObjective("faway_sb");
                if (objective == null) continue;
                String title = processText(player, rawFrame);
                if (!objective.getDisplayName().equals(title)) {
                    objective.setDisplayName(title);
                }
            }
        }, 0L, interval).getTaskId();

        plugin.getLogger().info("[Anim] เริ่มทำงาน | mode="
                + scoreboardConfig.getString("title_animation.mode", "frames")
                + " | frames=" + cachedFrames.size()
                + " | interval=" + interval + "t | taskId=" + animationTaskId);
    }

    private void stopAnimationTask() {
        if (animationTaskId != -1) {
            Bukkit.getScheduler().cancelTask(animationTaskId);
            animationTaskId = -1;
        }
    }

    private List<String> buildAnimationFrames() {
        List<String> frames = new ArrayList<>();
        String mode = scoreboardConfig.getString("title_animation.mode", "frames");

        if ("scroll".equalsIgnoreCase(mode)) {
            String base = scoreboardConfig.getString("title_animation.scroll_text", "FawayMC Network");
            int padding = Math.max(0, scoreboardConfig.getInt("title_animation.scroll_padding", 5));

            String colorPrefix = extractColorPrefix(base);
            String plainText = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', base));
            if (plainText == null) plainText = "";

            StringBuilder sb = new StringBuilder(plainText);
            for (int i = 0; i < padding; i++) sb.append(' ');
            String padded = sb.toString();

            if (padded.isEmpty()) return frames;

            for (int i = 0; i < padded.length(); i++) {
                String rotated = padded.substring(i) + padded.substring(0, i);
                frames.add(colorPrefix + rotated);
            }
        } else {
            List<String> cfgFrames = scoreboardConfig.getStringList("title_animation.frames");
            if (cfgFrames.isEmpty()) {
                String fallback = scoreboardConfig.getString("scoreboard.title", "&d&lFawayMC");
                frames.add(fallback);
            } else {
                frames.addAll(cfgFrames);
            }
        }

        return frames;
    }

    private String extractColorPrefix(String text) {
        if (text == null) return "";
        StringBuilder prefix = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < text.length()) {
                prefix.append('§').append(text.charAt(i + 1));
                i += 2;
            } else {
                break;
            }
        }
        return prefix.toString();
    }

    private String getCurrentTitle(Player player) {
        boolean animEnabled = scoreboardConfig.getBoolean("title_animation.enabled", false);
        if (animEnabled && cachedFrames != null && !cachedFrames.isEmpty()) {
            int idx = animationFrame % cachedFrames.size();
            return processText(player, cachedFrames.get(idx));
        }
        return processText(player, scoreboardConfig.getString("scoreboard.title", "&d&lFawayMC"));
    }

    // ===== Scoreboard Update =====

    public void stop() {
        stopTaskOnly();
        stopAnimationTask();
        cachedFrames = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        activeBoards.clear();
    }

    public void updateScoreboard(Player player) {
        if (!scoreboardConfig.getBoolean("scoreboard.enabled", true)) return;

        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = activeBoards.get(player.getUniqueId());
        if (board == null) {
            board = manager.getNewScoreboard();
            activeBoards.put(player.getUniqueId(), board);
        }

        String title = getCurrentTitle(player);

        Objective objective = board.getObjective("faway_sb");
        if (objective == null) {
            objective = board.registerNewObjective("faway_sb", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            if (!objective.getDisplayName().equals(title)) {
                objective.setDisplayName(title);
            }
        }

        List<String> lines = scoreboardConfig.getStringList("scoreboard.lines");
        int size = lines.size();

        for (int i = 0; i < size; i++) {
            int scoreIndex = size - i;
            String rawLine = lines.get(i);
            String translatedLine = processText(player, rawLine);

            String teamName = "line_" + i;
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }

            String entry = getUniqueChatColor(i);
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }

            String[] splitResult = splitStringWithColor(translatedLine, PREFIX_SUFFIX_LIMIT);
            team.setPrefix(splitResult[0]);
            team.setSuffix(splitResult[1]);

            objective.getScore(entry).setScore(scoreIndex);
        }

        for (String entry : board.getEntries()) {
            boolean found = false;
            for (int i = 0; i < size; i++) {
                if (entry.equals(getUniqueChatColor(i))) {
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

    private String translateHexColors(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String processText(Player player, String raw) {
        if (raw == null) return "";
        String result = PlaceholderManager.setPlaceholders(player, raw);
        result = translateHexColors(result);
        result = ChatColor.translateAlternateColorCodes('&', result);
        return result;
    }

    private String[] splitStringWithColor(String text, int limit) {
        if (text.length() <= limit) {
            return new String[]{text, ""};
        }
        String prefix = text.substring(0, limit);
        String lastColors = ChatColor.getLastColors(prefix);
        String suffix = lastColors + text.substring(limit);
        if (suffix.length() > limit) {
            suffix = suffix.substring(0, limit);
        }
        return new String[]{prefix, suffix};
    }

    private String getMsg(CommandSender sender, String path, String defaultMsg) {
        String msg = scoreboardConfig.getString("messages." + path, defaultMsg);
        Player player = (sender instanceof Player) ? (Player) sender : null;
        return processText(player, msg);
    }

    private String getUniqueChatColor(int index) {
        ChatColor[] colors = ChatColor.values();
        StringBuilder sb = new StringBuilder();
        sb.append(colors[index % colors.length]);
        sb.append(colors[(index + 1) % colors.length]);
        return sb.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("global.admin")) {
            sender.sendMessage(getMsg(sender, "no_permission", "&cคุณไม่มีสิทธิ์!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMsg(sender, "usage", "&e/scoreboard <on|off|reload|info>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("on")) {
            scoreboardConfig.set("scoreboard.enabled", true);
            saveScoreboardConfig();
            start();
            sender.sendMessage(getMsg(sender, "enabled", "&aเปิด Scoreboard สำเร็จ!"));
            return true;
        }

        if (subCommand.equals("off")) {
            scoreboardConfig.set("scoreboard.enabled", false);
            saveScoreboardConfig();
            stop();
            sender.sendMessage(getMsg(sender, "disabled", "&cปิด Scoreboard สำเร็จ!"));
            return true;
        }

        if (subCommand.equals("reload")) {
            reloadScoreboardConfig();
            start();
            sender.sendMessage(getMsg(sender, "reload", "&aรีโหลด scoreboard.yml สำเร็จ!"));
            return true;
        }

        if (subCommand.equals("info")) {
            boolean sbEnabled = scoreboardConfig.getBoolean("scoreboard.enabled", true);
            boolean animEnabled = scoreboardConfig.getBoolean("title_animation.enabled", false);
            int framesCount = (cachedFrames == null) ? -1 : cachedFrames.size();

            sender.sendMessage(ChatColor.YELLOW + "=== Scoreboard Debug ===");
            sender.sendMessage(ChatColor.GRAY + "scoreboard.enabled: " + ChatColor.WHITE + sbEnabled);
            sender.sendMessage(ChatColor.GRAY + "title_animation.enabled: " + ChatColor.WHITE
                    + (animEnabled ? ChatColor.GREEN + "" + animEnabled : ChatColor.RED + "" + animEnabled));
            sender.sendMessage(ChatColor.GRAY + "anim.mode: " + ChatColor.WHITE
                    + scoreboardConfig.getString("title_animation.mode", "frames"));
            sender.sendMessage(ChatColor.GRAY + "anim.interval: " + ChatColor.WHITE
                    + scoreboardConfig.getInt("title_animation.interval_ticks", 3) + " ticks");
            sender.sendMessage(ChatColor.GRAY + "cachedFrames: " + ChatColor.WHITE
                    + (framesCount == -1 ? ChatColor.RED + "null" : ChatColor.GREEN + "" + framesCount + " frames"));
            sender.sendMessage(ChatColor.GRAY + "animationTaskId: " + ChatColor.WHITE
                    + (animationTaskId == -1 ? ChatColor.RED + "NOT RUNNING" : ChatColor.GREEN + "" + animationTaskId));
            sender.sendMessage(ChatColor.GRAY + "currentFrame: " + ChatColor.WHITE + animationFrame);

            if (animationTaskId == -1) {
                String reason;
                if (!sbEnabled) {
                    reason = "scoreboard.enabled=false";
                } else if (!animEnabled) {
                    reason = "title_animation.enabled=false → แก้ใน scoreboard.yml";
                } else if (framesCount <= 0) {
                    reason = "cachedFrames ว่าง → ตรวจสอบ frames ใน config";
                } else {
                    reason = "task ไม่ถูกสร้าง — ตรวจสอบ console log";
                }
                sender.sendMessage(ChatColor.RED + "❌ สาเหตุ: " + reason);
            }
            return true;
        }

        sender.sendMessage(getMsg(sender, "usage", "&e/scoreboard <on|off|reload|info>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender.hasPermission("global.admin") && args.length == 1) {
            String input = args[0].toLowerCase();
            if ("on".startsWith(input)) completions.add("on");
            if ("off".startsWith(input)) completions.add("off");
            if ("reload".startsWith(input)) completions.add("reload");
            if ("info".startsWith(input)) completions.add("info");
        }
        return completions;
    }

    private void saveScoreboardConfig() {
        try {
            scoreboardConfig.save(scoreboardFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ scoreboard.yml ได้!");
        }
    }
}