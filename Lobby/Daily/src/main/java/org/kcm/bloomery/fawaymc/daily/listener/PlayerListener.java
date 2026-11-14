package org.kcm.bloomery.fawaymc.daily.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
         if (player.isOnline()) {
            PlayerStreak streak = this.manager.getCache().get(player.getUniqueId());
            if (streak != null) {
               if (this.manager.canClaim(streak)) {
                  String prefix = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("messages.prefix", ""));
                  String msg = this.plugin.getConfig().getString("messages.login_notify", "&e&l⭐ &fYou have a daily reward to claim! &7(/daily claim)");
                  player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', msg));
               }
            }
         }
      }, 40L);
   }
}
