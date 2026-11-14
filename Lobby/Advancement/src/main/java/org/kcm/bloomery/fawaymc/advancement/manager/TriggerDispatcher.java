package org.kcm.bloomery.fawaymc.advancement.manager;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;
import org.kcm.bloomery.fawaymc.advancement.data.TriggerType;

public class TriggerDispatcher implements Listener {
   private final Global plugin;
   private int levelPollTaskId = -1;

   public TriggerDispatcher(Global plugin) {
      this.plugin = plugin;
      this.startLevelPolling();
   }

   private void startLevelPolling() {
      if (this.levelPollTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.levelPollTaskId);
      }

      this.levelPollTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for(Player p : Bukkit.getOnlinePlayers()) {
            int level = this.getPlayerLevel(p);
            if (level > 0) {
               for(Advancement adv : this.plugin.getAdvancementManager().getByTrigger(TriggerType.LEVEL)) {
                  PlayerProgress prog = this.plugin.getDataManager().get(p.getUniqueId());
                  if (!prog.isUnlocked(adv.getId()) && level >= adv.getRequired()) {
                     this.plugin.getAdvancementManager().unlock(p, adv.getId());
                  }
               }
            }
         }

      }, 100L, 100L).getTaskId();
   }

   private int getPlayerLevel(Player player) {
      try {
         Plugin fl = Bukkit.getPluginManager().getPlugin("FawayLevel");
         if (fl != null && fl.isEnabled()) {
            Object dataMgr = fl.getClass().getMethod("getDataManager").invoke(fl);
            if (dataMgr == null) {
               return 0;
            } else {
               Object levelObj = dataMgr.getClass().getMethod("get", UUID.class).invoke(dataMgr, player.getUniqueId());
               return levelObj == null ? 0 : (Integer)levelObj.getClass().getMethod("getLevel").invoke(levelObj);
            }
         } else {
            return 0;
         }
      } catch (Exception var5) {
         return 0;
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();

      for(Advancement adv : this.plugin.getAdvancementManager().getByTrigger(TriggerType.JOIN)) {
         this.plugin.getAdvancementManager().incrementProgress(player, adv.getId(), 1);
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      String material = event.getBlock().getType().name();

      for(Advancement adv : this.plugin.getAdvancementManager().getByTrigger(TriggerType.BLOCK_BREAK)) {
         if (this.matchesMaterial(adv, material)) {
            this.plugin.getAdvancementManager().incrementProgress(player, adv.getId(), 1);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      String material = event.getBlock().getType().name();

      for(Advancement adv : this.plugin.getAdvancementManager().getByTrigger(TriggerType.BLOCK_PLACE)) {
         if (this.matchesMaterial(adv, material)) {
            this.plugin.getAdvancementManager().incrementProgress(player, adv.getId(), 1);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onMobKill(EntityDeathEvent event) {
      LivingEntity dead = event.getEntity();
      if (!(dead instanceof Player)) {
         Player killer = dead.getKiller();
         if (killer != null) {
            String entityType = dead.getType().name();
            boolean hostile = dead instanceof Monster;

            for(Advancement adv : this.plugin.getAdvancementManager().getByTrigger(TriggerType.MOB_KILL)) {
               if (this.matchesEntity(adv, entityType, hostile)) {
                  this.plugin.getAdvancementManager().incrementProgress(killer, adv.getId(), 1);
               }
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerKill(PlayerDeathEvent event) {
      Player killer = event.getEntity().getKiller();
      if (killer != null) {
         for(Advancement adv : this.plugin.getAdvancementManager().getByTrigger(TriggerType.PLAYER_KILL)) {
            this.plugin.getAdvancementManager().incrementProgress(killer, adv.getId(), 1);
         }

      }
   }

   private boolean matchesMaterial(Advancement adv, String material) {
      ConfigurationSection data = adv.getTriggerData();
      if (data == null) {
         return true;
      } else {
         String wanted = data.getString("material");
         return wanted != null && !wanted.isEmpty() && !wanted.equals("*") ? wanted.equalsIgnoreCase(material) : true;
      }
   }

   private boolean matchesEntity(Advancement adv, String entityType, boolean hostile) {
      ConfigurationSection data = adv.getTriggerData();
      if (data == null) {
         return true;
      } else {
         String wanted = data.getString("entity_type");
         if (wanted != null && !wanted.isEmpty()) {
            return wanted.equals("*") ? hostile : wanted.equalsIgnoreCase(entityType);
         } else {
            return true;
         }
      }
   }
}
