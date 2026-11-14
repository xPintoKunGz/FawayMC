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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablistManager implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private FileConfiguration tablistConfig;
    private File tablistFile;
    private int taskID = -1;

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public TablistManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createTablistConfig();

        if (plugin.getCommand("tablist") != null) {
            plugin.getCommand("tablist").setExecutor(this);
            plugin.getCommand("tablist").setTabCompleter(this);
        }
    }

    public void createTablistConfig() {
        tablistFile = new File(plugin.getDataFolder(), "tablist.yml");
        if (!tablistFile.exists()) {
            plugin.saveResource("tablist.yml", false);
        }
        tablistConfig = YamlConfiguration.loadConfiguration(tablistFile);
    }

    public void reloadTablistConfig() {
        if (tablistFile == null) {
            tablistFile = new File(plugin.getDataFolder(), "tablist.yml");
        }
        tablistConfig = YamlConfiguration.loadConfiguration(tablistFile);

        try (InputStreamReader defConfigStream = new InputStreamReader(plugin.getResource("tablist.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            tablistConfig.setDefaults(defConfig);
        } catch (Exception ignored) {}
    }

    public void start() {
        stopTaskOnly();
        if (!tablistConfig.getBoolean("tablist.enabled", true)) return;

        int updateTicks = tablistConfig.getInt("tablist.update_ticks", 20);

        taskID = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateTablist(player);
            }
        }, 0L, updateTicks).getTaskId();
    }

    private void stopTaskOnly() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    public void stop() {
        stopTaskOnly();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListHeaderFooter("", "");
        }
    }

    public void updateTablist(Player player) {
        if (!tablistConfig.getBoolean("tablist.enabled", true)) return;

        List<String> headerList = tablistConfig.getStringList("tablist.header");
        List<String> footerList = tablistConfig.getStringList("tablist.footer");

        String header = formatList(player, headerList);
        String footer = formatList(player, footerList);

        player.setPlayerListHeaderFooter(header, footer);
    }

    /**
     * แปลง HEX color &#RRGGBB → §x§R§R§G§G§B§B
     * ต้องทำ "หลัง" PlaceholderAPI แทนที่เสร็จ เพราะ PAPI อาจคืนค่า hex มาด้วย
     */
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

    /**
     * ลำดับการแปลง (สำคัญมาก):
     * 1. PlaceholderAPI + internal placeholders ({player}, {online}, ...)
     * 2. HEX color (&#RRGGBB)
     * 3. Standard color (&a, &b, ...) — PlaceholderManager แปลงให้แล้ว แต่ทำซ้ำก็ไม่เสียหาย
     */
    private String processText(Player player, String raw) {
        if (raw == null) return "";
        // 1) PAPI + internal — PlaceholderManager จะ translate & เป็น § ให้แล้ว
        String result = PlaceholderManager.setPlaceholders(player, raw);
        // 2) HEX translation ต้องทำหลัง PAPI
        result = translateHexColors(result);
        // 3) Safety: translate & ที่อาจเหลือ (เช่น PAPI คืน &a มา)
        result = ChatColor.translateAlternateColorCodes('&', result);
        return result;
    }

    private String formatList(Player player, List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(processText(player, list.get(i)));
            if (i < list.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String getMsg(CommandSender sender, String path, String defaultMsg) {
        String msg = tablistConfig.getString("messages." + path, defaultMsg);
        Player player = (sender instanceof Player) ? (Player) sender : null;
        return processText(player, msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("global.admin")) {
            sender.sendMessage(getMsg(sender, "no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMsg(sender, "usage", "&eการใช้งาน: /tablist <on|off|reload>"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("on")) {
            tablistConfig.set("tablist.enabled", true);
            saveTablistConfig();
            start();
            sender.sendMessage(getMsg(sender, "enabled", "&aเปิดใช้งาน Tablist เรียบร้อยแล้ว!"));
            return true;
        }

        if (subCommand.equals("off")) {
            tablistConfig.set("tablist.enabled", false);
            saveTablistConfig();
            stop();
            sender.sendMessage(getMsg(sender, "disabled", "&cปิดใช้งาน Tablist เรียบร้อยแล้ว!"));
            return true;
        }

        if (subCommand.equals("reload")) {
            reloadTablistConfig();
            start();
            sender.sendMessage(getMsg(sender, "reload", "&aรีโหลดไฟล์ tablist.yml เรียบร้อยแล้ว!"));
            return true;
        }

        sender.sendMessage(getMsg(sender, "usage", "&eการใช้งาน: /tablist <on|off|reload>"));
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

    private void saveTablistConfig() {
        try {
            tablistConfig.save(tablistFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ tablist.yml ได้!");
        }
    }
}