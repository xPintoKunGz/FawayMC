package org.kcm.bloomery.fawaymc.level.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kcm.bloomery.fawaymc.level.Global;

public class PlayerListener implements Listener {
   private final Global plugin;

   public PlayerListener(Global plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onJoin(PlayerJoinEvent event) {
      this.plugin.getDataManager().get(event.getPlayer().getUniqueId());
      this.plugin.getTablistManager().update(event.getPlayer());
      this.plugin.getExpBarManager().update(event.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.plugin.getDataManager().unload(event.getPlayer().getUniqueId());
   }

   @EventHandler
   public void onExpChange(PlayerExpChangeEvent event) {
      if (this.plugin.getConfig().getBoolean("display.exp_bar.block_vanilla_xp", true)) {
         event.setAmount(0);
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPlayerDeath(PlayerDeathEvent event) {
      event.setDroppedExp(0);
      event.setKeepLevel(true);
   }
}
