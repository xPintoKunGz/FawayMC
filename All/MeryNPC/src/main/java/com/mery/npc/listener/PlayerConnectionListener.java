package com.mery.npc.listener;

import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import com.mery.npc.util.PlaceholderUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerConnectionListener implements Listener {

    private final MeryNPC plugin;

    public PlayerConnectionListener(MeryNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getNpcManager().refreshAllHolograms();
            PlaceholderUtil.pollBungeeCounts(plugin);
        }, 1L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateViewersForPlayer(player);
            }
            plugin.getNpcManager().refreshAllHolograms();
        }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            data.getViewers().remove(player.getUniqueId());
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getNpcManager().refreshAllHolograms();
            PlaceholderUtil.pollBungeeCounts(plugin);
        }, 1L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        // Remove from previous NPCs
        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            if (data.getViewers().contains(player.getUniqueId())) {
                if (data.getServerPlayer() != null) {
                    plugin.getNmsPlayerSpawner().destroyForPlayer(player, data.getServerPlayer());
                }
                data.getViewers().remove(player.getUniqueId());
            }
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getNpcManager().refreshAllHolograms();
        }, 1L);

        // Spawn for new world
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateViewersForPlayer(player);
            }
            plugin.getNpcManager().refreshAllHolograms();
        }, 10L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateViewersForPlayer(player);
            }
        }, 10L);
    }

    public void updateViewersForPlayer(Player player) {
        Location pLoc = player.getLocation();
        if (pLoc.getWorld() == null) return;

        double maxDistSq = 64.0 * 64.0;

        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            if (data.getLocation() == null || !data.getLocation().getWorld().equals(pLoc.getWorld())) continue;

            double distSq = data.getLocation().distanceSquared(pLoc);
            boolean inRange = distSq <= maxDistSq;
            boolean isViewing = data.getViewers().contains(player.getUniqueId());

            if (inRange && !isViewing) {
                if (data.getServerPlayer() != null) {
                    plugin.getNmsPlayerSpawner().spawnForPlayer(player, data.getServerPlayer(), data);
                    data.getViewers().add(player.getUniqueId());
                }
            } else if (!inRange && isViewing) {
                if (data.getServerPlayer() != null) {
                    plugin.getNmsPlayerSpawner().destroyForPlayer(player, data.getServerPlayer());
                }
                data.getViewers().remove(player.getUniqueId());
            }
        }
    }
}

