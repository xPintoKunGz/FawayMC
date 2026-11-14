package org.kcm.bloomery.fawaymc.advancement.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;
import org.kcm.bloomery.fawaymc.advancement.data.TriggerType;

public class PlayerListener implements Listener {
   private final Global plugin;
   private int playtimeTaskId = -1;

   public PlayerListener(Global plugin) {
      this.plugin = plugin;
      this.startPlaytimeTask();
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onJoin(PlayerJoinEvent event) {
      this.plugin.getDataManager().get(event.getPlayer().getUniqueId());
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.plugin.getDataManager().unload(event.getPlayer().getUniqueId());
   }

   private void startPlaytimeTask() {
      this.playtimeTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for(Player p : Bukkit.getOnlinePlayers()) {
            PlayerProgress prog = this.plugin.getDataManager().get(p.getUniqueId());
            prog.addPlaytimeMinutes(1L);

            for(Advancement adv : this.plugin.getAdvancementManager().getByTrigger(TriggerType.PLAY_TIME)) {
               if (!prog.isUnlocked(adv.getId())) {
                  if (prog.getPlaytimeMinutes() >= (long)adv.getRequired()) {
                     this.plugin.getAdvancementManager().unlock(p, adv.getId());
                  } else {
                     prog.setProgress(adv.getId(), (int)prog.getPlaytimeMinutes());
                  }
               }
            }
         }

      }, 1200L, 1200L).getTaskId();
   }
}
