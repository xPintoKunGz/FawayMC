package com.antimery.listener;

import com.antimery.AntiMery;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.File;
import java.net.InetAddress;
import java.util.List;

/**
 * PreventPortBypass (กันเจาะพอร์ตตรง)
 * ตรวจสอบว่าเซิร์ฟเวอร์เป็นเครือข่าย BungeeCord/Velocity Proxy หรือไม่
 * - ถ้าเป็น BungeeCord: บังคับให้เข้าผ่าน IP/Port ของ Proxy ที่อนุญาตเท่านั้น
 * - ถ้าไม่ใช่ BungeeCord (Standalone Server): ปล่อยผ่านตามปกติ ไม่บล็อค
 */
public class PortBypassListener implements Listener {

    private final AntiMery plugin;
    private Boolean isBungeeEnvironment = null;

    public PortBypassListener(AntiMery plugin) {
        this.plugin = plugin;
        checkEnvironment();
    }

    /**
     * ตรวจสอบว่าเซิร์ฟเวอร์ทำงานในโหมด BungeeCord / Proxy หรือไม่
     */
    public void checkEnvironment() {
        if (plugin.getConfigManager().isPortBypassForceEnable()) {
            this.isBungeeEnvironment = true;
            plugin.getLogger().info("[AntiMery PreventPortBypass] โหมดบังคับใช้งานเปิดอยู่ (Force Enable)");
            return;
        }

        if (!plugin.getConfigManager().isPortBypassAutoDetectBungee()) {
            this.isBungeeEnvironment = false;
            return;
        }

        boolean bungeeDetected = false;

        // 1. ตรวจสอบผ่าน Bukkit / Spigot Config API
        try {
            org.bukkit.Server server = Bukkit.getServer();
            org.bukkit.Server.Spigot spigot = server.spigot();
            if (spigot != null) {
                org.bukkit.configuration.file.YamlConfiguration spigotConfig = spigot.getConfig();
                if (spigotConfig != null && spigotConfig.getBoolean("settings.bungeecord", false)) {
                    bungeeDetected = true;
                }
            }
        } catch (Throwable ignored) {
        }

        // 2. ตรวจสอบไฟล์ spigot.yml ตรงๆ หากเข้าถึงได้
        if (!bungeeDetected) {
            try {
                File spigotFile = new File("spigot.yml");
                if (spigotFile.exists()) {
                    org.bukkit.configuration.file.YamlConfiguration yaml = 
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(spigotFile);
                    if (yaml.getBoolean("settings.bungeecord", false)) {
                        bungeeDetected = true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 3. ตรวจสอบ Paper Velocity config
        if (!bungeeDetected) {
            try {
                File paperFile = new File("paper.yml");
                if (paperFile.exists()) {
                    org.bukkit.configuration.file.YamlConfiguration paperYaml = 
                            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(paperFile);
                    if (paperYaml.getBoolean("settings.velocity-support.enabled", false)) {
                        bungeeDetected = true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        this.isBungeeEnvironment = bungeeDetected;
        if (bungeeDetected) {
            plugin.getLogger().info("[AntiMery PreventPortBypass] ตรวจพบเซิร์ฟเวอร์อยู่ในเครือข่าย BungeeCord/Proxy! เปิดระบบบล็อค Port Bypass อัตโนมัติ");
        } else {
            plugin.getLogger().info("[AntiMery PreventPortBypass] เซิร์ฟเวอร์นี้เป็น Standalone (ไม่ใช่ BungeeCord) -> ระบบปล่อยผ่านให้ผู้เล่นเข้าปกติ");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!plugin.getConfigManager().isPortBypassEnabled()) {
            return;
        }

        // หากไม่ใช่เซิร์ฟเวอร์ BungeeCord ให้ปล่อยผ่านตามปกติ
        if (isBungeeEnvironment != null && !isBungeeEnvironment) {
            return;
        }

        // ตรวจสอบที่อยู่ IP จริงของคนที่เชื่อมต่อเข้ามา (RealAddress)
        InetAddress realAddress = null;
        try {
            realAddress = event.getRealAddress();
        } catch (NoSuchMethodError | Exception ignored) {
        }

        if (realAddress == null && event.getAddress() != null) {
            realAddress = event.getAddress();
        }

        if (realAddress == null) {
            return;
        }

        String incomingIp = realAddress.getHostAddress().trim();
        List<String> allowedProxies = plugin.getConfigManager().getPortBypassProxyAddresses();

        boolean allowed = false;

        for (String allowedEntry : allowedProxies) {
            if (allowedEntry == null || allowedEntry.trim().isEmpty()) continue;

            // รองรับทั้งแบบคั่นด้วย ; ในตัวเดียว
            String[] splits = allowedEntry.split(";");
            for (String split : splits) {
                String proxy = split.trim();
                if (proxy.isEmpty()) continue;

                if (proxy.equalsIgnoreCase("localhost") || proxy.equalsIgnoreCase("127.0.0.1")) {
                    if (incomingIp.equals("127.0.0.1") || incomingIp.equals("0:0:0:0:0:0:0:1") || incomingIp.equalsIgnoreCase("localhost")) {
                        allowed = true;
                        break;
                    }
                }

                if (incomingIp.equalsIgnoreCase(proxy)) {
                    allowed = true;
                    break;
                }
            }

            if (allowed) {
                break;
            }
        }

        if (plugin.getConfigManager().isPortBypassDebug()) {
            plugin.getLogger().info("[PreventPortBypass DEBUG] ผู้เล่น " + event.getPlayer().getName() 
                    + " เข้ามาจาก IP: " + incomingIp + " | ผลการตรวจ: " + (allowed ? "ผ่าน (ALLOWED)" : "ถูกบล็อค (BLOCKED)"));
        }

        if (!allowed) {
            String kickMessage = plugin.getConfigManager().getPortBypassKickMessage();
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);

            plugin.getLogger().warning("[AntiMery PreventPortBypass] สกัดกั้นการเชื่อมต่อตรง! ผู้เล่น: " 
                    + event.getPlayer().getName() + " พยายามเชื่อมต่อจาก IP: " + incomingIp 
                    + " (ไม่อยู่ในรายการ Proxy ที่อนุญาต)");
        }
    }
}

