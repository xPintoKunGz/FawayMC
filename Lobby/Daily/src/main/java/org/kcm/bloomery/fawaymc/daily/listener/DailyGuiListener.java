package org.kcm.bloomery.fawaymc.daily.listener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.daily.Global;
import org.kcm.bloomery.fawaymc.daily.data.PlayerStreak;
import org.kcm.bloomery.fawaymc.daily.gui.DailyGuiHolder;
import org.kcm.bloomery.fawaymc.daily.gui.DailyMainGui;
import org.kcm.bloomery.fawaymc.daily.manager.DailyManager;

public class DailyGuiListener implements Listener {
   private static final Set<UUID> transitioning = ConcurrentHashMap.newKeySet();
   private static final long CLICK_COOLDOWN_MS = 300L;
   private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap();
   private static final Map<UUID, Long> lastRemaining = new ConcurrentHashMap();
   private static final int COUNTDOWN_SLOT = 22;
   private final Global plugin;
   private final DailyManager manager;
   private int refreshTaskId = -1;

   public DailyGuiListener(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getDailyManager();
      this.startRefreshTask();
   }

   public void stop() {
      if (this.refreshTaskId != -1) {
         try {
            Bukkit.getScheduler().cancelTask(this.refreshTaskId);
         } catch (Exception var2) {
         }

         this.refreshTaskId = -1;
      }

   }

   private void startRefreshTask() {
      this.refreshTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for(Player p : Bukkit.getOnlinePlayers()) {
            if (!(p.getOpenInventory().getTopInventory().getHolder() instanceof DailyGuiHolder)) {
               lastRemaining.remove(p.getUniqueId());
            } else {
               PlayerStreak streak = this.manager.getCache().get(p.getUniqueId());
               if (streak != null) {
                  long remaining = this.manager.getTimeUntilNextClaim(streak);
                  Long prev = (Long)lastRemaining.put(p.getUniqueId(), remaining);
                  if (prev != null && prev > 0L && remaining == 0L) {
                     try {
                        (new DailyMainGui(this.plugin)).open(p);
                     } catch (Throwable var9) {
                     }
                  } else {
                     try {
                        Inventory inv = p.getOpenInventory().getTopInventory();
                        if (inv != null) {
                           (new DailyMainGui(this.plugin)).refreshTimeBased(p, inv);
                        }
                     } catch (Throwable var8) {
                     }
                  }
               }
            }
         }

      }, 20L, 20L).getTaskId();
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof DailyGuiHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (event.getClick() != ClickType.DOUBLE_CLICK) {
               if (event.getClickedInventory() != null && event.getClickedInventory() == event.getView().getTopInventory()) {
                  ItemStack clicked = event.getCurrentItem();
                  if (clicked != null && clicked.hasItemMeta()) {
                     String action = (String)clicked.getItemMeta().getPersistentDataContainer().get(DailyMainGui.ACTION_KEY, PersistentDataType.STRING);
                     if (action != null) {
                        UUID uuid = player.getUniqueId();
                        long now = System.currentTimeMillis();
                        Long last = (Long)lastClickTime.get(uuid);
                        if (last == null || now - last >= 300L) {
                           lastClickTime.put(uuid, now);
                           if (!transitioning.contains(uuid)) {
                              transitioning.add(uuid);
                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> transitioning.remove(uuid), 5L);
                              this.handleAction(player, action);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onDrag(InventoryDragEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof DailyGuiHolder) {
         event.setCancelled(true);
      }

   }

   private void handleAction(Player player, String action) {
      switch (action) {
         case "close":
            transitioning.remove(player.getUniqueId());
            player.closeInventory();
            return;
         case "noop":
            transitioning.remove(player.getUniqueId());
            return;
         case "claim":
            this.handleClaim(player);
            return;
         default:
            transitioning.remove(player.getUniqueId());
      }
   }

   private void handleClaim(Player player) {
      this.manager.claimAsync(player, (result) -> {
         transitioning.remove(player.getUniqueId());
         String prefix = this.color(this.plugin.getConfig().getString("messages.prefix", ""));
         switch (result) {
            case SUCCESS:
               try {
                  player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
               } catch (Exception var6) {
               }

               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof DailyGuiHolder) {
                     (new DailyMainGui(this.plugin)).open(player);
                  }

               }, 3L);
               break;
            case ALREADY_CLAIMED:
               player.sendMessage(prefix + this.color(this.plugin.getConfig().getString("messages.already_claimed", "&cAlready claimed!")));

               try {
                  player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               } catch (Exception var5) {
               }
               break;
            case NO_REWARD:
               player.sendMessage(prefix + this.color(this.plugin.getConfig().getString("messages.no_rewards", "&cNo reward configured.")));
               break;
            case ERROR:
               player.sendMessage(prefix + this.color("&cAn error occurred."));
         }

      });
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}
