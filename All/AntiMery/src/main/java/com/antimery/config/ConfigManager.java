package com.antimery.config;

import com.antimery.AntiMery;
import com.antimery.punishment.PunishmentType;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private final AntiMery plugin;
    private FileConfiguration config;

    // General
    private String prefix;
    private PunishmentType defaultPunishment;
    private String customCommand;
    private String banCommand;
    private String kickMessage;
    private String warnMessage;
    private String staffAlertMessage;
    private int violationResetSeconds;
    private boolean alertSound;

    public ConfigManager(AntiMery plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.prefix = colorize(config.getString("general.prefix", "&8[&cAntiMery&8] &r"));
        this.defaultPunishment = PunishmentType.fromString(config.getString("general.default-punishment", "KICK"));
        this.customCommand = config.getString("general.custom-command", "tempban %player% 1d [AntiMery] %reason%");
        this.banCommand = config.getString("general.ban-command", "ban %player% &c[AntiMery] %reason%");
        this.kickMessage = colorize(config.getString("general.kick-message", "&c[AntiMery]\n&fคุณถูกเตะออกจากเซิร์ฟเวอร์\n&7สาเหตุ: &e%reason%"));
        this.warnMessage = colorize(config.getString("general.warn-message", "&c[AntiMery] &eคำเตือน: %reason%"));
        this.staffAlertMessage = colorize(config.getString("general.staff-alert", "&8[&cAntiMery&8] &e%player% &7ละเมิดกฎ &c%check% &8(&7VL: &e%vl%/%max_vl%&8) &7รายละเอียด: &f%details%"));
        this.violationResetSeconds = config.getInt("general.violation-reset-seconds", 120);
        this.alertSound = config.getBoolean("general.alert-sound", true);
    }

    public String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getPrefix() {
        return prefix;
    }

    public PunishmentType getDefaultPunishment() {
        return defaultPunishment;
    }

    public String getCustomCommand() {
        return customCommand;
    }

    public String getBanCommand() {
        return banCommand;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public String getWarnMessage() {
        return warnMessage;
    }

    public String getStaffAlertMessage() {
        return staffAlertMessage;
    }

    public int getViolationResetSeconds() {
        return violationResetSeconds;
    }

    public boolean isAlertSound() {
        return alertSound;
    }

    // Anti-Hijack (ป้องกันการเข้าแทรกบัญชี)
    public boolean isAntiHijackEnabled() {
        return config.getBoolean("anti-hijack.enabled", true);
    }

    public String getAntiHijackKickMessage() {
        return colorize(config.getString("anti-hijack.kick-message",
                "&c[AntiMery]\n&fไม่สามารถเข้าสู่ระบบได้!\n&eมีผู้เล่นชื่อนี้กำลังออนไลน์อยู่ในเซิร์ฟเวอร์แล้ว\n&8(ระบบป้องกันการเข้าแทรก / Session Hijack)"));
    }

    // Prevent Port Bypass (BungeeCord / Proxy Check)
    public boolean isPortBypassEnabled() {
        return config.getBoolean("prevent-port-bypass.enabled", true);
    }

    public boolean isPortBypassAutoDetectBungee() {
        return config.getBoolean("prevent-port-bypass.auto-detect-bungee", true);
    }

    public boolean isPortBypassForceEnable() {
        return config.getBoolean("prevent-port-bypass.force-enable", false);
    }

    public List<String> getPortBypassProxyAddresses() {
        List<String> addresses = config.getStringList("prevent-port-bypass.proxy-addresses");
        if (addresses == null || addresses.isEmpty()) {
            String single = config.getString("prevent-port-bypass.proxy-address");
            if (single != null && !single.isEmpty()) {
                addresses = Arrays.asList(single.split(";"));
            } else {
                addresses = Arrays.asList("127.0.0.1", "localhost");
            }
        }
        return addresses;
    }

    public String getPortBypassKickMessage() {
        return colorize(config.getString("prevent-port-bypass.kick-message",
                "&c[AntiMery]\n&fกรุณาเข้าเซิร์ฟเวอร์ผ่าน IP/Port หลักของทางเซิร์ฟเวอร์เท่านั้น!\n&e(ห้ามเชื่อมต่อ Port ด้านหลังโดยตรง / Port Bypass Blocked)"));
    }

    public boolean isPortBypassDebug() {
        return config.getBoolean("prevent-port-bypass.debug", false);
    }

    // Check helpers
    public boolean isCheckEnabled(String path) {
        return config.getBoolean("checks." + path + ".enabled", true);
    }

    public PunishmentType getCheckPunishment(String path) {
        String p = config.getString("checks." + path + ".punishment", "DEFAULT");
        PunishmentType pt = PunishmentType.fromString(p);
        return (pt == PunishmentType.DEFAULT) ? defaultPunishment : pt;
    }

    public int getCheckMaxVL(String path, int defaultVal) {
        return config.getInt("checks." + path + ".max-vl", defaultVal);
    }

    public double getCheckDouble(String path, String key, double defaultVal) {
        return config.getDouble("checks." + path + "." + key, defaultVal);
    }

    public boolean getCheckBoolean(String path, String key, boolean defaultVal) {
        return config.getBoolean("checks." + path + "." + key, defaultVal);
    }

    public int getCheckInt(String path, String key, int defaultVal) {
        return config.getInt("checks." + path + "." + key, defaultVal);
    }

    public String getCheckString(String path, String key, String defaultVal) {
        return config.getString("checks." + path + "." + key, defaultVal);
    }

    public List<String> getCheckStringList(String path, String key) {
        List<String> list = config.getStringList("checks." + path + "." + key);
        return list != null ? list : Collections.emptyList();
    }
}

