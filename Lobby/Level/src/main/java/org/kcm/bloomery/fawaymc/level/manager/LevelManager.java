package org.kcm.bloomery.fawaymc.level.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.level.Global;
import org.kcm.bloomery.fawaymc.level.data.PlayerLevel;

public class LevelManager {
   private final Global plugin;
   private int playtimeCheckTaskId = -1;

   public LevelManager(Global plugin) {
      this.plugin = plugin;
   }

   public long getRequiredExp(int level) {
      String type = this.plugin.getConfig().getString("level.formula.type", "LINEAR").toUpperCase();
      long base = this.plugin.getConfig().getLong("level.formula.base", 100L);
      long increment = this.plugin.getConfig().getLong("level.formula.increment", 50L);
      double multiplier = this.plugin.getConfig().getDouble("level.formula.multiplier", (double)1.5F);
      switch (type) {
         case "EXPONENTIAL":
            return (long)((double)base * Math.pow(multiplier, (double)Math.max(0, level - 1)));
         case "LINEAR":
         default:
            return base + (long)(level - 1) * increment;
      }
   }

   public long getExpRemaining(PlayerLevel pl) {
      return this.isMaxLevel(pl.getLevel()) ? 0L : Math.max(0L, this.getRequiredExp(pl.getLevel()) - pl.getExp());
   }

   public boolean isMaxLevel(int level) {
      int max = this.plugin.getConfig().getInt("level.max_level", 100);
      return max > 0 && level >= max;
   }

   public int addExp(Player player, long amount) {
      if (player != null && amount > 0L) {
         PlayerLevel pl = this.plugin.getDataManager().get(player.getUniqueId());
         pl.addTotalExp(amount);
         pl.setExp(pl.getExp() + amount);

         int levelsGained;
         for(levelsGained = 0; !this.isMaxLevel(pl.getLevel()); ++levelsGained) {
            long required = this.getRequiredExp(pl.getLevel());
            if (pl.getExp() < required) {
               break;
            }

            pl.setExp(pl.getExp() - required);
            pl.setLevel(pl.getLevel() + 1);
         }

         if (this.isMaxLevel(pl.getLevel())) {
            pl.setExp(0L);
         }

         this.refreshDisplay(player);
         if (levelsGained > 0) {
            this.onLevelUp(player, pl.getLevel());
         }

         return levelsGained;
      } else {
         return 0;
      }
   }

   public void setExp(Player player, long amount) {
      PlayerLevel pl = this.plugin.getDataManager().get(player.getUniqueId());
      pl.setExp(Math.max(0L, amount));

      while(!this.isMaxLevel(pl.getLevel())) {
         long required = this.getRequiredExp(pl.getLevel());
         if (pl.getExp() < required) {
            break;
         }

         pl.setExp(pl.getExp() - required);
         pl.setLevel(pl.getLevel() + 1);
      }

      this.refreshDisplay(player);
   }

   public void setLevel(Player player, int level) {
      PlayerLevel pl = this.plugin.getDataManager().get(player.getUniqueId());
      int max = this.plugin.getConfig().getInt("level.max_level", 100);
      int clamped = Math.max(1, max > 0 ? Math.min(max, level) : level);
      pl.setLevel(clamped);
      pl.setExp(0L);
      this.refreshDisplay(player);
   }

   private void refreshDisplay(Player player) {
      this.plugin.getExpBarManager().update(player);
      this.plugin.getTablistManager().update(player);
   }

   private void onLevelUp(Player player, int newLevel) {
      String raw = this.plugin.getConfig().getString("messages.level_up", "&a&lLEVEL UP! &eYou reached level &f{level}&e!");
      player.sendMessage(ChatColor.translateAlternateColorCodes('&', raw.replace("{level}", String.valueOf(newLevel))));

      try {
         player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
      } catch (Exception var5) {
      }

      if (this.plugin.getConfig().getBoolean("messages.level_up_broadcast", false)) {
         String bf = this.plugin.getConfig().getString("messages.level_up_broadcast_format", "&6{player} &ehas reached level &6{level}&e!");
         Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', bf.replace("{player}", player.getName()).replace("{level}", String.valueOf(newLevel))));
      }

      this.runRewards(player, newLevel);
   }

   private void runRewards(Player player, int level) {
      ConfigurationSection rewards = this.plugin.getConfig().getConfigurationSection("rewards");
      if (rewards != null) {
         for(String key : rewards.getKeys(false)) {
            if (rewards.getInt(key + ".level", -1) == level) {
               for(String cmd : rewards.getStringList(key + ".commands")) {
                  String run = cmd.replace("{player}", player.getName());
                  Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run));
               }
            }
         }

      }
   }

   public void startPlaytimeTask() {
      if (this.plugin.getConfig().getBoolean("level.exp_sources.playtime.enabled", true)) {
         int minutes = this.plugin.getConfig().getInt("level.exp_sources.playtime.interval_minutes", 5);
         long amount = this.plugin.getConfig().getLong("level.exp_sources.playtime.amount", 25L);
         long ticks = (long)Math.max(1, minutes) * 60L * 20L;
         if (this.playtimeCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.playtimeCheckTaskId);
         }

         this.playtimeCheckTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            for(Player p : Bukkit.getOnlinePlayers()) {
               this.addExp(p, amount);
            }

         }, ticks, ticks).getTaskId();
      }
   }
}
