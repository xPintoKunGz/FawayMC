package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkManager {

    private final BloomeryX plugin;
    private final File afkFile;
    private FileConfiguration afkConfig;
    private Location afkLocation;

    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> afkStatus = new ConcurrentHashMap<>();
    private final Map<UUID, Location> preAfkLocation = new ConcurrentHashMap<>();
    private BukkitTask afkCheckTask;

    public AfkManager(BloomeryX plugin) {
        this.plugin = plugin;
        this.afkFile = new File(plugin.getDataFolder(), "afk.yml");
        loadAfkPoint();
        startAfkTask();
    }

    public void loadAfkPoint() {
        if (!afkFile.exists()) return;
        afkConfig = YamlConfiguration.loadConfiguration(afkFile);
        if (afkConfig.contains("afk-point")) {
            afkLocation = PlayerDataManager.loadLocation(afkConfig.getConfigurationSection("afk-point"));
        }
    }

    public void setAfkPoint(Location location) {
        this.afkLocation = location;
        if (afkConfig == null) {
            afkConfig = new YamlConfiguration();
        }
        PlayerDataManager.saveLocation(afkConfig.createSection("afk-point"), location);
        try {
            afkConfig.save(afkFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save AFK point to afk.yml");
        }
    }

    public boolean deleteAfkPoint() {
        if (!hasAfkPoint()) return false;
        afkLocation = null;
        if (afkConfig != null) {
            afkConfig.set("afk-point", null);
            try {
                afkConfig.save(afkFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to remove AFK point from afk.yml");
            }
        }
        return true;
    }

    public Location getAfkPoint() {
        return afkLocation;
    }

    public boolean hasAfkPoint() {
        return afkLocation != null && afkLocation.getWorld() != null;
    }

    public void updateActivity(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        if (isAfk(uuid)) {
            exitAfk(player);
        }
    }

    public boolean isAfk(UUID uuid) {
        return afkStatus.getOrDefault(uuid, false);
    }

    public void toggleAfk(Player player, String reason) {
        if (isAfk(player.getUniqueId())) {
            exitAfk(player);
        } else {
            enterAfk(player, reason);
        }
    }

    public void enterAfk(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        afkStatus.put(uuid, true);

        // Save pre-AFK location if returning is enabled
        preAfkLocation.put(uuid, player.getLocation());

        // Broadcast or notify
        if (plugin.getConfig().getBoolean("afk.announce", true)) {
            String format = plugin.getConfig().getString("afk.messages.enter-broadcast", "&#FF758C[AFK] &f{player} &7is now AFK");
            if (reason != null && !reason.trim().isEmpty()) {
                format += " &8(&f" + reason + "&8)";
            }
            Bukkit.broadcastMessage(ColorUtil.colorize(format.replace("{player}", player.getName())));
        }

        MessageUtil.sendConfigMessage(player, "afk-self-enter");
        MessageUtil.playSuccess(player);

        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }

        // Teleport to AFK point if configured
        if (plugin.getConfig().getBoolean("afk.teleport-on-afk", true) && hasAfkPoint()) {
            player.teleport(afkLocation);
            MessageUtil.sendConfigMessage(player, "afk-teleported");
        }
    }

    public void exitAfk(Player player) {
        UUID uuid = player.getUniqueId();
        afkStatus.put(uuid, false);
        lastActivity.put(uuid, System.currentTimeMillis());

        // Broadcast or notify
        if (plugin.getConfig().getBoolean("afk.announce", true)) {
            String format = plugin.getConfig().getString("afk.messages.exit-broadcast", "&#70E000[AFK] &f{player} &7is no longer AFK!");
            Bukkit.broadcastMessage(ColorUtil.colorize(format.replace("{player}", player.getName())));
        }

        MessageUtil.sendConfigMessage(player, "afk-self-exit");
        MessageUtil.playSuccess(player);

        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }

        // Teleport back if return-on-active is enabled
        if (plugin.getConfig().getBoolean("afk.return-on-active", true)) {
            Location prev = preAfkLocation.remove(uuid);
            if (prev != null && prev.getWorld() != null) {
                player.teleport(prev);
                MessageUtil.sendConfigMessage(player, "afk-returned");
            }
        }
    }

    public void unregisterPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.remove(uuid);
        afkStatus.remove(uuid);
        preAfkLocation.remove(uuid);
    }

    private void startAfkTask() {
        if (afkCheckTask != null) {
            afkCheckTask.cancel();
        }

        afkCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("afk.enabled", true)) return;

            long thresholdMinutes = plugin.getConfig().getLong("afk.auto-afk-minutes", 5);
            if (thresholdMinutes <= 0) return;

            long thresholdMillis = thresholdMinutes * 60 * 1000L;
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("bloomeryx.bypass.afk")) continue;

                UUID uuid = player.getUniqueId();
                long last = lastActivity.getOrDefault(uuid, now);

                if (!isAfk(uuid) && (now - last) >= thresholdMillis) {
                    enterAfk(player, "Inactive for " + thresholdMinutes + "m");
                }
            }
        }, 100L, 100L); // check every 5 seconds
    }

    public void stop() {
        if (afkCheckTask != null) {
            afkCheckTask.cancel();
            afkCheckTask = null;
        }
    }
}

