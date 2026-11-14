package org.kcm.bloomery.fawaymc.leaderboard.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kcm.bloomery.fawaymc.leaderboard.Global;

public class PlayerListener implements Listener {
   private final Global plugin;

   public PlayerListener(Global plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onJoin(PlayerJoinEvent event) {
      this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
         this.plugin.getLeaderboardManager().refreshPlayer(event.getPlayer());
         this.plugin.getHologramManager().requestRefresh();
      }, 3L);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.plugin.getLeaderboardManager().refreshPlayer(event.getPlayer());
      this.plugin.getHologramManager().requestRefresh();
   }
}
