package org.kcm.bloomery.fawaymc.global;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
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

public class BossbarManager implements CommandExecutor, TabCompleter, Listener {

    private final JavaPlugin plugin;
    private FileConfiguration barConfig;
    private File barFile;
    private int taskID = -1;

    // BossBar per-player (เพราะ PAPI ต่างกันแต่ละคน)
    private final Map<UUID, BossBar> playerBars = new HashMap<>();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public BossbarManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createBarConfig();

        if (plugin.getCommand("bossbar") != null) {
            plugin.getCommand("bossbar").setExecutor(this);
            plugin.getCommand("bossbar").setTabCompleter(this);
        }

        // ★ ลงทะเบียน listener เพื่อจับ world change / quit
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        start();
    }

    public void createBarConfig() {
        barFile = new File(plugin.getDataFolder(), "bossbar.yml");
        if (!barFile.exists()) {
            plugin.saveResource("bossbar.yml", false);
        }
        barConfig = YamlConfiguration.loadConfiguration(barFile);
    }

    public void reloadBarConfig() {
        if (barFile == null) barFile = new File(plugin.getDataFolder(), "bossbar.yml");
        barConfig = YamlConfiguration.loadConfiguration(barFile);

        try (InputStreamReader defConfigStream = new InputStreamReader(plugin.getResource("bossbar.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            barConfig.setDefaults(defConfig);
        } catch (Exception ignored) {}

        for (Player p : Bukkit.getOnlinePlayers()) {
            removeBar(p);
        }
        playerBars.clear();

        start();
    }

    public void start() {
        stopTaskOnly();
        if (!barConfig.getBoolean("bossbar.enabled", true)) return;

        int updateTicks = barConfig.getInt("bossbar.update_ticks", 20);

        taskID = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateBossBar(player);
            }
        }, 0L, updateTicks).getTaskId();
    }

    private void stopTaskOnly() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    /**
     * ★ stop(): ยกเลิก task + ลบ bossbar ออกจากผู้เล่นทุกคน
     * ★ ต้องเรียกตอน onDisable เพื่อไม่ให้ bossbar ค้างข้ามการ reload plugin
     */
    public void stop() {
        stopTaskOnly();
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeBar(p);
        }
        playerBars.clear();
    }

    private void removeBar(Player player) {
        BossBar bar = playerBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    // ===== Spawn world check =====

    /**
     * อ่าน spawn.world จาก global.yml (config ของ Global plugin)
     * เพื่อตรวจว่าผู้เล่นอยู่ในโลกที่ตั้ง spawn ไว้หรือไม่
     */
    private boolean isInSpawnWorld(Player player) {
        try {
            FileConfiguration global = plugin.getConfig();
            String worldName = global.getString("spawn.world");
            if (worldName == null || worldName.isEmpty()) return false;
            return player.getWorld().getName().equals(worldName);
        } catch (Exception e) {
            return false;
        }
    }

    // ===== Events =====

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // กัน memory leak
        removeBar(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // ★ อัปเดตทันทีเมื่อเปลี่ยน world (ไม่ต้องรอ periodic task)
        updateBossBar(event.getPlayer());
    }

    // ===== Update =====

    /**
     * อัปเดต bossbar สำหรับผู้เล่น 1 คน
     * - ปิดใช้งาน / ไม่อยู่ใน spawn world → ลบออก
     * - อยู่ใน spawn world → สร้างหรืออัปเดต
     */
    public void updateBossBar(Player player) {
        if (!barConfig.getBoolean("bossbar.enabled", true)) {
            removeBar(player);
            return;
        }

        // ★ แสดงเฉพาะในโลกที่ตั้ง spawn ไว้
        if (!isInSpawnWorld(player)) {
            removeBar(player);
            return;
        }

        String rawTitle = barConfig.getString("bossbar.title", "&eFawayMC Network");
        String title = processText(player, rawTitle);

        String colorName = barConfig.getString("bossbar.color", "YELLOW");
        String styleName = barConfig.getString("bossbar.style", "SOLID");
        double progress = barConfig.getDouble("bossbar.progress", 1.0);
        progress = Math.max(0.0, Math.min(1.0, progress));

        BarColor color;
        try {
            color = BarColor.valueOf(colorName.toUpperCase());
        } catch (Exception e) {
            color = BarColor.YELLOW;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(styleName.toUpperCase());
        } catch (Exception e) {
            style = BarStyle.SOLID;
        }

        BossBar bar = playerBars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar(title, color, style);
            bar.setProgress(progress);
            bar.addPlayer(player);
            playerBars.put(player.getUniqueId(), bar);
        } else {
            if (!bar.getTitle().equals(title)) bar.setTitle(title);
            if (bar.getColor() != color) bar.setColor(color);
            if (bar.getStyle() != style) bar.setStyle(style);
            if (Math.abs(bar.getProgress() - progress) > 0.001) bar.setProgress(progress);
            if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
        }
    }

    // ===== Text processing =====

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

    private String getMsg(CommandSender sender, String path, String defaultMsg) {
        String msg = barConfig.getString("messages." + path, defaultMsg);
        Player player = (sender instanceof Player) ? (Player) sender : null;
        return processText(player, msg);
    }

    // ===== Command =====

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("global.admin")) {
            sender.sendMessage(getMsg(sender, "no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMsg(sender, "usage", "&eการใช้งาน: /bossbar <on|off|reload>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("on")) {
            barConfig.set("bossbar.enabled", true);
            saveBarConfig();
            start();
            for (Player p : Bukkit.getOnlinePlayers()) updateBossBar(p);
            sender.sendMessage(getMsg(sender, "enabled", "&aเปิดใช้งาน BossBar เรียบร้อยแล้ว!"));
            return true;
        }

        if (sub.equals("off")) {
            barConfig.set("bossbar.enabled", false);
            saveBarConfig();
            stop();
            sender.sendMessage(getMsg(sender, "disabled", "&cปิดใช้งาน BossBar เรียบร้อยแล้ว!"));
            return true;
        }

        if (sub.equals("reload")) {
            reloadBarConfig();
            for (Player p : Bukkit.getOnlinePlayers()) updateBossBar(p);
            sender.sendMessage(getMsg(sender, "reload", "&aรีโหลดไฟล์ bossbar.yml เรียบร้อยแล้ว!"));
            return true;
        }

        sender.sendMessage(getMsg(sender, "usage", "&eการใช้งาน: /bossbar <on|off|reload>"));
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
        }
        return completions;
    }

    private void saveBarConfig() {
        try {
            barConfig.save(barFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ bossbar.yml ได้!");
        }
    }
}