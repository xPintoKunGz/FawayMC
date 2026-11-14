package org.kcm.bloomery.fawaymc.level.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.level.Global;
import org.kcm.bloomery.fawaymc.level.data.PlayerLevel;

public class ExpBarManager {
   private final Global plugin;
   private int taskId = -1;

   public ExpBarManager(Global plugin) {
      this.plugin = plugin;
   }

   public void start() {
      if (this.plugin.getConfig().getBoolean("display.exp_bar.enabled", true)) {
         int interval = this.plugin.getConfig().getInt("display.exp_bar.update_ticks", 20);
         this.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for(Player p : Bukkit.getOnlinePlayers()) {
               this.update(p);
            }

         }, 0L, (long)Math.max(1, interval)).getTaskId();
      }
   }

   public void update(Player player) {
      if (this.plugin.getConfig().getBoolean("display.exp_bar.enabled", true)) {
         if (player != null && player.isOnline()) {
            PlayerLevel pl = this.plugin.getDataManager().get(player.getUniqueId());
            int level = pl.getLevel();
            long exp = pl.getExp();
            long required = this.plugin.getLevelManager().getRequiredExp(level);
            float progress;
            if (!this.plugin.getLevelManager().isMaxLevel(level) && required > 0L) {
               progress = Math.max(0.0F, Math.min(1.0F, (float)exp / (float)required));
            } else {
               progress = 1.0F;
            }

            try {
               player.setLevel(level);
               player.setExp(progress);
            } catch (Exception var10) {
            }

         }
      }
   }
}
