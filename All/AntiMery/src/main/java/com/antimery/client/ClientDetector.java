package com.antimery.client;

import com.antimery.AntiMery;
import com.antimery.check.CheckType;
import com.antimery.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDetector implements Listener, PluginMessageListener {

    private final AntiMery plugin;
    private final Map<UUID, ClientInfo> playerClients = new ConcurrentHashMap<>();

    public ClientDetector(AntiMery plugin) {
        this.plugin = plugin;
    }

    public void registerChannels() {
        try {
            // In older Bukkit versions, minecraft:brand was registered via messenger.
            // In modern Paper, minecraft: is reserved and throws IllegalArgumentException,
            // which we safely catch so it never crashes onEnable!
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
        } catch (Throwable ignored) {
        }
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "MC|Brand", this);
        } catch (Throwable ignored) {
        }
    }

    public void unregisterChannels() {
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, "minecraft:brand", this);
        } catch (Throwable ignored) {
        }
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, "MC|Brand", this);
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Delay 3 ticks so client handshake brand packet is fully received by Paper/Spigot
        Bukkit.getScheduler().runTaskLater(plugin, () -> fetchAndCheckClient(player), 3L);
    }

    public void fetchAndCheckClient(Player player) {
        if (player == null || !player.isOnline()) return;

        String brand = null;
        try {
            // Paper / modern Spigot native method
            Method method = player.getClass().getMethod("getClientBrandName");
            Object res = method.invoke(player);
            if (res != null) {
                brand = res.toString().trim();
            }
        } catch (Throwable ignored) {
        }

        if (brand == null || brand.isEmpty()) {
            ClientInfo existing = playerClients.get(player.getUniqueId());
            if (existing != null) {
                return;
            }
            brand = "vanilla";
        }

        ClientInfo info = parseClient(brand);
        playerClients.put(player.getUniqueId(), info);

        // Check Hack Client or Banned Mod
        if (info.getType().isHackClient() || isBannedMod(brand)) {
            handleHackClientDetected(player, info);
            return;
        }

        // Notify staff on join if enabled
        boolean notifyStaff = plugin.getConfigManager().getConfig().getBoolean("client-detection.staff-notify-on-join", true);
        if (notifyStaff) {
            notifyStaffClient(player, info);
        }
    }

    public void handleQuit(Player player) {
        playerClients.remove(player.getUniqueId());
    }

    public ClientInfo getClientInfo(Player player) {
        ClientInfo cached = playerClients.get(player.getUniqueId());
        if (cached != null) {
            return cached;
        }

        // Try live lookup
        try {
            Method method = player.getClass().getMethod("getClientBrandName");
            Object res = method.invoke(player);
            if (res != null) {
                ClientInfo parsed = parseClient(res.toString());
                playerClients.put(player.getUniqueId(), parsed);
                return parsed;
            }
        } catch (Throwable ignored) {
        }

        return new ClientInfo("Vanilla / Unknown", "Vanilla", ClientType.VANILLA);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try {
            String raw = new String(message, StandardCharsets.UTF_8).trim();
            String cleanBrand = raw.replaceAll("^[\\x00-\\x1F]+", "").trim();

            ClientInfo info = parseClient(cleanBrand);
            playerClients.put(player.getUniqueId(), info);

            if (info.getType().isHackClient()) {
                handleHackClientDetected(player, info);
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isBannedMod(String name) {
        if (name == null || name.isEmpty()) return false;
        String lower = name.toLowerCase();

        java.util.List<String> bannedList = plugin.getConfigManager().getConfig().getStringList("client-detection.banned-mods");
        if (bannedList != null && !bannedList.isEmpty()) {
            for (String banned : bannedList) {
                if (banned != null && !banned.trim().isEmpty() && lower.contains(banned.toLowerCase().trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    public ClientInfo parseClient(String brand) {
        if (brand == null) {
            return new ClientInfo("Vanilla", "Vanilla", ClientType.VANILLA);
        }
        String clean = brand.replaceAll("^[\\x00-\\x1F]+", "").trim();
        String lower = clean.toLowerCase();

        // 1. Check custom banned-mods list from config
        java.util.List<String> bannedList = plugin.getConfigManager().getConfig().getStringList("client-detection.banned-mods");
        if (bannedList != null && !bannedList.isEmpty()) {
            for (String banned : bannedList) {
                if (banned != null && !banned.trim().isEmpty() && lower.contains(banned.toLowerCase().trim())) {
                    return new ClientInfo(clean, clean + " (Banned Mod)", ClientType.HACK_CLIENT);
                }
            }
        }

        // 2. Check known HACK CLIENTS
        if (lower.contains("wurst")) return new ClientInfo(clean, "Wurst Client", ClientType.HACK_CLIENT);
        if (lower.contains("meteor")) return new ClientInfo(clean, "Meteor Client", ClientType.HACK_CLIENT);
        if (lower.contains("liquidbounce")) return new ClientInfo(clean, "LiquidBounce", ClientType.HACK_CLIENT);
        if (lower.contains("aristois")) return new ClientInfo(clean, "Aristois", ClientType.HACK_CLIENT);
        if (lower.contains("sigma")) return new ClientInfo(clean, "Sigma Client", ClientType.HACK_CLIENT);
        if (lower.contains("impact")) return new ClientInfo(clean, "Impact Client", ClientType.HACK_CLIENT);
        if (lower.contains("inertia")) return new ClientInfo(clean, "Inertia Client", ClientType.HACK_CLIENT);
        if (lower.contains("bleach")) return new ClientInfo(clean, "BleachHack", ClientType.HACK_CLIENT);
        if (lower.contains("rusher")) return new ClientInfo(clean, "RusherHack", ClientType.HACK_CLIENT);
        if (lower.contains("future")) return new ClientInfo(clean, "Future Client", ClientType.HACK_CLIENT);
        if (lower.contains("vape")) return new ClientInfo(clean, "Vape Client", ClientType.HACK_CLIENT);
        if (lower.contains("fdp")) return new ClientInfo(clean, "FDP Client", ClientType.HACK_CLIENT);
        if (lower.contains("thunder")) return new ClientInfo(clean, "ThunderHack", ClientType.HACK_CLIENT);
        if (lower.contains("mathax")) return new ClientInfo(clean, "Mathax Client", ClientType.HACK_CLIENT);
        if (lower.contains("augustus")) return new ClientInfo(clean, "Augustus Client", ClientType.HACK_CLIENT);
        if (lower.contains("rise")) return new ClientInfo(clean, "Rise Client", ClientType.HACK_CLIENT);
        if (lower.contains("novoline")) return new ClientInfo(clean, "Novoline", ClientType.HACK_CLIENT);
        if (lower.contains("astolfo")) return new ClientInfo(clean, "Astolfo", ClientType.HACK_CLIENT);
        if (lower.contains("flux")) return new ClientInfo(clean, "Flux Client", ClientType.HACK_CLIENT);
        if (lower.contains("baritone")) return new ClientInfo(clean, "Baritone Mod", ClientType.HACK_CLIENT);
        if (lower.contains("xray")) return new ClientInfo(clean, "X-Ray Mod", ClientType.HACK_CLIENT);

        // 3. Check popular PVP CLIENTS
        if (lower.contains("lunar")) return new ClientInfo(clean, "Lunar Client", ClientType.PVP_CLIENT);
        if (lower.contains("labymod")) return new ClientInfo(clean, "LabyMod", ClientType.PVP_CLIENT);
        if (lower.contains("badlion")) return new ClientInfo(clean, "Badlion Client", ClientType.PVP_CLIENT);
        if (lower.contains("feather")) return new ClientInfo(clean, "Feather Client", ClientType.PVP_CLIENT);

        // 3. Check VANILLA / OPTIFINE
        if (lower.equals("vanilla")) return new ClientInfo(clean, "Vanilla Minecraft", ClientType.VANILLA);
        if (lower.contains("optifine")) return new ClientInfo(clean, "OptiFine", ClientType.VANILLA);

        // 4. Check standard MOD LOADERS
        if (lower.contains("fabric")) return new ClientInfo(clean, "Fabric", ClientType.MOD_LOADER);
        if (lower.contains("forge") || lower.contains("fml")) return new ClientInfo(clean, "Minecraft Forge", ClientType.MOD_LOADER);
        if (lower.contains("quilt")) return new ClientInfo(clean, "Quilt", ClientType.MOD_LOADER);
        if (lower.contains("neoforge")) return new ClientInfo(clean, "NeoForge", ClientType.MOD_LOADER);
        if (lower.contains("pojav")) return new ClientInfo(clean, "PojavLauncher", ClientType.MOD_LOADER);

        return new ClientInfo(clean, clean.isEmpty() ? "Vanilla" : clean, ClientType.UNKNOWN);
    }

    private void handleHackClientDetected(Player player, ClientInfo info) {
        boolean blockHacks = plugin.getConfigManager().getConfig().getBoolean("client-detection.block-hack-clients", true);
        if (!blockHacks) return;

        String punishStr = plugin.getConfigManager().getConfig().getString("client-detection.punishment", "KICK");
        PunishmentType punishment = PunishmentType.fromString(punishStr);

        plugin.getLogger().warning("[AntiMery] ตรวจพบแฮคไคลเอนต์/มอดโกง: " + player.getName() + " ใช้ " + info.getCleanName() + " (" + info.getRawBrand() + ")");

        String kickMsg = plugin.getConfigManager().colorize(plugin.getConfigManager().getConfig().getString(
                "client-detection.kick-message",
                "&c[AntiMery]\n&fตรวจพบการใช้งานโปรแกรมช่วยเล่น/แฮคไคลเอนต์ (&e%client%&f)\n&cไม่อนุญาตให้เข้าใช้งานเซิร์ฟเวอร์!"
        )).replace("%client%", info.getCleanName()).replace("%player%", player.getName());

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (punishment == PunishmentType.BAN) {
                plugin.getPunishmentManager().executePunishment(player, CheckType.FLY, PunishmentType.BAN, "ตรวจพบแฮคไคลเอนต์: " + info.getCleanName());
            } else {
                player.kickPlayer(kickMsg);
            }
        });
    }

    private void notifyStaffClient(Player player, ClientInfo info) {
        // Silent Mode: ไม่สแปมข้อความในแชทแอดมินตามที่สั่ง
    }
}
