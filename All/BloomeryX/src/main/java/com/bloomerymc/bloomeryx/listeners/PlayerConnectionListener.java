package com.bloomerymc.bloomeryx.listeners;

import com.bloomerymc.bloomeryx.BloomeryX;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final BloomeryX plugin;

    public PlayerConnectionListener(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data into cache
        plugin.getPlayerDataManager().getData(player.getUniqueId());

        // Initial update for display names and tablist
        plugin.getNametagManager().updatePlayer(player);

        // Schedule delayed full sync (5 ticks) so all client entity trackers receive nametags cleanly
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getNametagManager().updateAllPlayers();
            }
        }, 5L);

        // Register initial activity for AFK tracking
        plugin.getAfkManager().updateActivity(player);

        // Handle auto-play music on join
        plugin.getMusicManager().handleJoinAutoplay(player);

        // Hide vanished players from the joining player
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getPlayerDataManager().isVanished(online.getUniqueId())) {
                if (!player.hasPermission("bloomeryx.vanish.see")) {
                    player.hidePlayer(plugin, online);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save last location for /back
        plugin.getPlayerDataManager().setBackLocation(player.getUniqueId(), player.getLocation());

        // Unregister AFK & clean up
        plugin.getNametagManager().removePlayer(player);
        plugin.getMusicManager().cleanupPlayer(player);
        plugin.getAfkManager().unregisterPlayer(player);
        plugin.getPlayerDataManager().unloadData(player.getUniqueId());
        plugin.getTeleportManager().removeRequest(player.getUniqueId());
    }
}
