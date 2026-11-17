package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private final BloomeryX plugin;
    private final Map<UUID, TpaRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> activeTeleports = new ConcurrentHashMap<>();

    public enum TpaType {
        TPA, // sender teleports to receiver
        TPA_HERE // receiver teleports to sender
    }

    public static class TpaRequest {
        private final UUID sender;
        private final UUID target;
        private final TpaType type;
        private final long timestamp;

        public TpaRequest(UUID sender, UUID target, TpaType type) {
            this.sender = sender;
            this.target = target;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public TpaType getType() { return type; }
        public long getTimestamp() { return timestamp; }
    }

    public TeleportManager(BloomeryX plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target, TpaType type) {
        pendingRequests.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId(), target.getUniqueId(), type));

        int timeout = plugin.getConfig().getInt("teleport.tpa-timeout", 60);

        // Schedule timeout expiration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TpaRequest req = pendingRequests.get(target.getUniqueId());
            if (req != null && req.getSender().equals(sender.getUniqueId()) && req.getTimestamp() == req.getTimestamp()) {
                pendingRequests.remove(target.getUniqueId());
            }
        }, timeout * 20L);
    }

    public TpaRequest getRequest(UUID target) {
        return pendingRequests.get(target);
    }

    public void removeRequest(UUID target) {
        pendingRequests.remove(target);
    }

    public void teleportPlayer(Player player, Location destination, Runnable onComplete) {
        if (player == null || destination == null) return;

        int delay = plugin.getConfig().getInt("teleport.teleport-delay", 0);
        // Save current location as back-location before teleporting
        plugin.getPlayerDataManager().setBackLocation(player.getUniqueId(), player.getLocation());

        if (delay <= 0 || player.hasPermission("bloomeryx.bypass.teleportdelay")) {
            player.teleport(destination);
            MessageUtil.playTeleport(player);
            if (onComplete != null) onComplete.run();
            return;
        }

        Location startLoc = player.getLocation().clone();
        MessageUtil.sendMessage(player, "&7Teleporting in &#FF7EB3" + delay + " &7seconds... Do not move!");

        BukkitRunnable task = new BukkitRunnable() {
            int count = delay;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeTeleports.remove(player.getUniqueId());
                    return;
                }

                if (plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) {
                    if (startLoc.getWorld() != player.getWorld() || startLoc.distanceSquared(player.getLocation()) > 0.5) {
                        MessageUtil.sendMessage(player, "&#FF4B4B✕ &cTeleportation cancelled because you moved!");
                        MessageUtil.playError(player);
                        cancel();
                        activeTeleports.remove(player.getUniqueId());
                        return;
                    }
                }

                count--;
                if (count <= 0) {
                    player.teleport(destination);
                    MessageUtil.playTeleport(player);
                    if (onComplete != null) onComplete.run();
                    cancel();
                    activeTeleports.remove(player.getUniqueId());
                }
            }
        };

        activeTeleports.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 20L, 20L);
    }
}

