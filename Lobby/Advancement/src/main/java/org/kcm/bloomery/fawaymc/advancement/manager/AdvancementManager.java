package org.kcm.bloomery.fawaymc.advancement.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;
import org.kcm.bloomery.fawaymc.advancement.data.TriggerType;
import org.kcm.bloomery.fawaymc.advancement.gui.AdvancementCategoryGui;
import org.kcm.bloomery.fawaymc.advancement.gui.AdvancementListGui;

public class AdvancementManager {
   private final Global plugin;
   private final RewardDispatcher rewardDispatcher;
   private final Map<String, Advancement> advancements = new HashMap();
   private final Map<TriggerType, List<Advancement>> byTrigger = new EnumMap(TriggerType.class);

   public AdvancementManager(Global plugin) {
      this.plugin = plugin;
      this.rewardDispatcher = new RewardDispatcher(plugin);
   }

   public void loadFromConfig() {
      this.advancements.clear();
      this.byTrigger.clear();
      ConfigurationSection sec = this.plugin.getConfig().getConfigurationSection("advancements");
      if (sec == null) {
         this.plugin.getLogger().warning("No advancements configured!");
      } else {
         for(String id : sec.getKeys(false)) {
            ConfigurationSection cfg = sec.getConfigurationSection(id);
            if (cfg != null) {
               Advancement adv = new Advancement(id, cfg);
               this.advancements.put(id, adv);
               ((List)this.byTrigger.computeIfAbsent(adv.getTriggerType(), (k) -> new ArrayList())).add(adv);
            }
         }

         this.plugin.getLogger().info("Loaded " + this.advancements.size() + " advancements.");
         if (this.plugin.getConfig().getBoolean("settings.auto_cleanup", true)) {
            try {
               int[] result = this.plugin.getDataManager().cleanupInvalid(this.advancements.keySet());
               if (result[1] > 0) {
                  this.plugin.getLogger().info("[Advancement] Cleaned " + result[1] + " invalid entries from " + result[0] + " players");
               }
            } catch (Exception e) {
               this.plugin.getLogger().warning("[Advancement] Cleanup failed: " + e.getMessage());
            }
         }

      }
   }

   public Advancement get(String id) {
      return (Advancement)this.advancements.get(id);
   }

   public Collection<Advancement> getAll() {
      return this.advancements.values();
   }

   public int getAdvancementCount() {
      return this.advancements.size();
   }

   public List<Advancement> getByTrigger(TriggerType t) {
      return (List)this.byTrigger.getOrDefault(t, Collections.emptyList());
   }

   public List<Advancement> getByCategory(String category) {
      List<Advancement> list = new ArrayList();

      for(Advancement a : this.advancements.values()) {
         if (a.getCategory().equalsIgnoreCase(category)) {
            list.add(a);
         }
      }

      return list;
   }

   public void incrementProgress(Player player, String advId, int amount) {
      Advancement adv = (Advancement)this.advancements.get(advId);
      if (adv != null && player != null) {
         PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
         if (!prog.isUnlocked(advId)) {
            int current = prog.getProgress(advId) + amount;
            prog.setProgress(advId, current);
            if (current >= adv.getRequired()) {
               this.unlock(player, advId);
            } else {
               this.plugin.getNotificationManager().notifyProgress(player, adv, current);
               this.refreshOpenGui(player);
            }

         }
      }
   }

   public void setProgress(Player player, String advId, int value) {
      PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
      prog.setProgress(advId, Math.max(0, value));
      Advancement adv = (Advancement)this.advancements.get(advId);
      if (adv != null && value >= adv.getRequired() && !prog.isUnlocked(advId)) {
         this.unlock(player, advId);
      }

   }

   public void unlock(Player player, String advId) {
      Advancement adv = (Advancement)this.advancements.get(advId);
      if (adv != null && player != null) {
         PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
         if (!prog.isUnlocked(advId)) {
            prog.markUnlocked(advId);
            prog.setProgress(advId, adv.getRequired());
            this.plugin.getNotificationManager().notifyUnlock(player, adv);
            Bukkit.getScheduler().runTask(this.plugin, () -> this.rewardDispatcher.dispatch(player, adv));
            if (adv.isBroadcast() && this.plugin.getConfig().getBoolean("settings.broadcast_on_high_tier", true)) {
               this.plugin.getNotificationManager().broadcast(player, adv);
            }

            this.plugin.getNotificationManager().afterUnlock(player, adv);
            this.refreshOpenGui(player);
         }
      }
   }

   public void revoke(Player player, String advId) {
      PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
      prog.getUnlocked().remove(advId);
      prog.setProgress(advId, 0);
   }

   public int getUnlockedCount(Player player) {
      return this.plugin.getDataManager().get(player.getUniqueId()).getUnlocked().size();
   }

   private void refreshOpenGui(Player player) {
      if (player != null && player.isOnline()) {
         try {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top == null) {
               return;
            }

            InventoryHolder holder = top.getHolder();
            if (holder instanceof AdvancementListGui.Holder) {
               String cat = ((AdvancementListGui.Holder)holder).getCategory();
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  if (player.isOnline()) {
                     InventoryHolder cur = player.getOpenInventory().getTopInventory().getHolder();
                     if (cur instanceof AdvancementListGui.Holder && cat.equals(((AdvancementListGui.Holder)cur).getCategory())) {
                        (new AdvancementListGui(this.plugin)).open(player, cat);
                     }
                  }

               });
            } else if (holder instanceof AdvancementCategoryGui.Holder) {
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  if (player.isOnline()) {
                     InventoryHolder cur = player.getOpenInventory().getTopInventory().getHolder();
                     if (cur instanceof AdvancementCategoryGui.Holder) {
                        (new AdvancementCategoryGui(this.plugin)).open(player);
                     }
                  }

               });
            }
         } catch (Exception var5) {
         }

      }
   }
}
