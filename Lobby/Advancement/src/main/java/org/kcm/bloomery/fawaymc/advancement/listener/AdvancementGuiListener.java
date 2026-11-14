package org.kcm.bloomery.fawaymc.advancement.listener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.gui.AdvancementCategoryGui;
import org.kcm.bloomery.fawaymc.advancement.gui.AdvancementListGui;

public class AdvancementGuiListener implements Listener {
   private final Global plugin;
   private final Set<UUID> locked = ConcurrentHashMap.newKeySet();
   private static final long LOCK_TICKS = 5L;

   public AdvancementGuiListener(Global plugin) {
      this.plugin = plugin;
   }

   private boolean isOurGui(InventoryHolder holder) {
      return holder instanceof AdvancementCategoryGui.Holder || holder instanceof AdvancementListGui.Holder;
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onClick(InventoryClickEvent event) {
      InventoryHolder holder = event.getView().getTopInventory().getHolder();
      if (this.isOurGui(holder)) {
         event.setCancelled(true);
         event.setResult(Result.DENY);
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            UUID uuid = player.getUniqueId();
            if (!this.locked.contains(uuid)) {
               ClickType type = event.getClick();
               if (type == ClickType.LEFT || type == ClickType.RIGHT) {
                  int topSize = event.getView().getTopInventory().getSize();
                  if (event.getRawSlot() >= 0 && event.getRawSlot() < topSize) {
                     ItemStack clicked = event.getCurrentItem();
                     if (clicked != null && clicked.hasItemMeta()) {
                        if (holder instanceof AdvancementCategoryGui.Holder) {
                           String catId = (String)clicked.getItemMeta().getPersistentDataContainer().get(AdvancementCategoryGui.CATEGORY_KEY, PersistentDataType.STRING);
                           if (catId != null) {
                              this.applyLock(uuid);

                              try {
                                 player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.4F);
                              } catch (Exception var11) {
                              }

                              try {
                                 (new AdvancementListGui(this.plugin)).open(player, catId);
                              } catch (Exception e) {
                                 this.plugin.getLogger().warning("[AdvGUI] openList failed: " + e.getMessage());
                              }

                           }
                        } else {
                           String back = (String)clicked.getItemMeta().getPersistentDataContainer().get(AdvancementListGui.BACK_KEY, PersistentDataType.STRING);
                           if (back != null) {
                              this.applyLock(uuid);

                              try {
                                 player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
                              } catch (Exception var13) {
                              }

                              try {
                                 (new AdvancementCategoryGui(this.plugin)).open(player);
                              } catch (Exception e) {
                                 this.plugin.getLogger().warning("[AdvGUI] openCategory failed: " + e.getMessage());
                              }

                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void applyLock(UUID uuid) {
      this.locked.add(uuid);
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.locked.remove(uuid), 5L);
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onDrag(InventoryDragEvent event) {
      if (this.isOurGui(event.getView().getTopInventory().getHolder())) {
         event.setCancelled(true);
      }

   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onMove(InventoryMoveItemEvent event) {
      if (this.isOurGui(event.getSource().getHolder()) || this.isOurGui(event.getDestination().getHolder())) {
         event.setCancelled(true);
      }

   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (this.locked.contains(player.getUniqueId())) {
         event.setCancelled(true);
      } else {
         Inventory top = player.getOpenInventory().getTopInventory();
         if (top != null && this.isOurGui(top.getHolder())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onSwapHands(PlayerSwapHandItemsEvent event) {
      Player player = event.getPlayer();
      if (this.locked.contains(player.getUniqueId())) {
         event.setCancelled(true);
      } else {
         Inventory top = player.getOpenInventory().getTopInventory();
         if (top != null && this.isOurGui(top.getHolder())) {
            event.setCancelled(true);
         }

      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.locked.remove(event.getPlayer().getUniqueId());
   }
}
