package org.kcm.bloomery.fawaymc.daily.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent; // ✅ เพิ่ม QuitEvent
import org.kcm.bloomery.fawaymc.daily.Global;
import org.kcm.bloomery.fawaymc.daily.data.PlayerStreak;
import org.kcm.bloomery.fawaymc.daily.manager.DailyManager;

public class PlayerListener implements Listener {
   private final Global plugin;
   private final DailyManager manager;

   public PlayerListener(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getDailyManager();
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();

      this.manager.loadPlayerAsync(player.getUniqueId(), player.getName());

      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (!player.isOnline()) return;

         PlayerStreak streak = this.manager.getCache().get(player.getUniqueId());
         if (streak != null) {
            this.manager.sendReminder(player);
         } else {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               if (player.isOnline()) {
                  PlayerStreak s = this.manager.getCache().get(player.getUniqueId());
                  if (s != null) {
                     this.manager.sendReminder(player);
                  }
               }
            }, 20L);
         }
      }, 20L);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.manager.clearSession(event.getPlayer().getUniqueId());
   }
}