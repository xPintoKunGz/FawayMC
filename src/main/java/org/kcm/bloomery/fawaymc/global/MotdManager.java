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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.CachedServerIcon;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // *** ดึงค่าจาก src/main/resources/motd.yml (ใน jar) ***
        loadFromResources(true);

        loadServerIcon();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (plugin.getCommand("globalmotd") != null) {
            plugin.getCommand("globalmotd").setExecutor(this);
            plugin.getCommand("globalmotd").setTabCompleter(this);
        }
    }

    /**
     * โหลด motd.yml จาก src/main/resources/motd.yml (ใน jar)
     * - copy ไปยัง plugins/Global/motd.yml (overwrite ถ้า replace=true)
     * - อ่านค่าจาก plugins/Global/motd.yml ด้วย UTF-8
     */
    public void loadFromResources(boolean replace) {
        // *** ตรวจสอบว่ามีไฟล์ใน resources หรือไม่ ***
        InputStream resource = plugin.getResource("motd.yml");
        if (resource == null) {
            plugin.getLogger().severe("[MOTD] ❌ ไม่พบ motd.yml ใน src/main/resources/ — กรุณาสร้างไฟล์ใน resources");
            motdConfig = new YamlConfiguration();
            return;
        }

        try {
            // *** ใช้ saveResource(path, replace) เพื่อ copy จาก jar ไป plugins/Global/ ***
            plugin.saveResource("motd.yml", replace);
            plugin.getLogger().info("[MOTD] copy motd.yml จาก resources → " + motdFile.getAbsolutePath()
                    + " (replace=" + replace + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("[MOTD] saveResource ไม่สำเร็จ (อาจมีไฟล์อยู่แล้ว): " + e.getMessage());
        } finally {
            try { resource.close(); } catch (Exception ignored) {}
        }

        // *** อ่านไฟล์ที่ copy ไปแล้วด้วย UTF-8 ***
        if (!motdFile.exists()) {
            plugin.getLogger().severe("[MOTD] ไม่พบ motd.yml ที่ " + motdFile.getAbsolutePath());
            motdConfig = new YamlConfiguration();
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(motdFile), StandardCharsets.UTF_8)) {
            motdConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            plugin.getLogger().severe("[MOTD] parse ไม่สำเร็จ: " + e.getMessage());
            motdConfig = new YamlConfiguration();
            return;
        }

        // *** log ค่าที่อ่านได้ ***
        List<String> line1 = motdConfig.getStringList("motd.line_1");
        List<String> line2 = motdConfig.getStringList("motd.line_2");

        plugin.getLogger().info("[MOTD] === ค่าที่อ่านได้ ===");
        plugin.getLogger().info("[MOTD] enabled = " + motdConfig.getBoolean("motd.enabled", true));
        plugin.getLogger().info("[MOTD] line_1 count = " + line1.size());
        for (int i = 0; i < line1.size(); i++) {
            plugin.getLogger().info("[MOTD] line_1[" + i + "] = " + line1.get(i));
        }
        plugin.getLogger().info("[MOTD] line_2 count = " + line2.size());
        for (int i = 0; i < line2.size(); i++) {
            plugin.getLogger().info("[MOTD] line_2[" + i + "] = " + line2.get(i));
        }
    }

    /**
     * reload: copy จาก resources ทับไฟล์ใน plugins/Global/ แล้วอ่านใหม่
     * ใช้เมื่อ /globalreload ถูกเรียก
     */
    public void reloadMotdConfig() {
        loadFromResources(true);  // force overwrite จาก resources
        loadServerIcon();
    }

    private void loadServerIcon() {
        serverIcon = null;
        if (!motdConfig.getBoolean("motd.icon.enabled", true)) return;

        String iconName = motdConfig.getString("motd.icon.file_name", "server-icon.png");
        File iconFile = new File(plugin.getDataFolder(), iconName);

        if (!iconFile.exists()) {
            File rootIcon = new File("server-icon.png");
            if (rootIcon.exists()) iconFile = rootIcon;
        }

        if (iconFile.exists()) {
            try {
                serverIcon = Bukkit.loadServerIcon(iconFile);
                plugin.getLogger().info("[MOTD] โหลด icon สำเร็จ");
            } catch (Exception e) {
                plugin.getLogger().warning("[MOTD] โหลด icon ไม่สำเร็จ: " + e.getMessage());
            }
        }
    }

    private String formatColor(String text) {
        if (text == null) return "";
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
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent event) {
        if (!motdConfig.getBoolean("motd.enabled", true)) return;

        List<String> line1List = motdConfig.getStringList("motd.line_1");
        List<String> line2List = motdConfig.getStringList("motd.line_2");

        if (line1List.isEmpty() && line2List.isEmpty()) {
            plugin.getLogger().warning("[MOTD] line_1 และ line_2 ว่าง - ไม่ override MOTD");
            return;
        }

        String line1 = line1List.isEmpty() ? "" : line1List.get(random.nextInt(line1List.size()));
        String line2 = line2List.isEmpty() ? "" : line2List.get(random.nextInt(line2List.size()));

        String fullMotd = formatColor(line1) + "\n" + formatColor(line2);
        event.setMotd(fullMotd);

        if (motdConfig.getBoolean("motd.custom_max_players.enabled", false)) {
            event.setMaxPlayers(motdConfig.getInt("motd.custom_max_players.max", 100));
        }

        if (serverIcon != null) {
            event.setServerIcon(serverIcon);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("global.admin")) {
            sender.sendMessage(formatColor("&cคุณไม่มีสิทธิ์!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(formatColor("&e/globalmotd reload &7- copy จาก resources ทับแล้วอ่านใหม่"));
            sender.sendMessage(formatColor("&e/globalmotd info &7- ดูค่าปัจจุบัน"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            reloadMotdConfig();
            sender.sendMessage(formatColor("&aรีโหลด motd.yml จาก resources สำเร็จ!"));
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(),
                        org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            return true;
        }

        if (sub.equals("info")) {
            sender.sendMessage(formatColor("&e=== MOTD INFO ==="));
            sender.sendMessage(formatColor("&7path: &f" + motdFile.getAbsolutePath()));
            sender.sendMessage(formatColor("&7enabled: &f" + motdConfig.getBoolean("motd.enabled", true)));
            sender.sendMessage(formatColor("&7line_1 count: &f" + motdConfig.getStringList("motd.line_1").size()));
            sender.sendMessage(formatColor("&7line_2 count: &f" + motdConfig.getStringList("motd.line_2").size()));

            List<String> l1 = motdConfig.getStringList("motd.line_1");
            for (int i = 0; i < l1.size(); i++) {
                sender.sendMessage(formatColor("&7  l1[" + i + "]: &f" + l1.get(i)));
            }
            return true;
        }

        sender.sendMessage(formatColor("&cใช้ /globalmotd reload หรือ info"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender.hasPermission("global.admin") && args.length == 1) {
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) completions.add("reload");
            if ("info".startsWith(input)) completions.add("info");
        }
        return completions;
    }
}