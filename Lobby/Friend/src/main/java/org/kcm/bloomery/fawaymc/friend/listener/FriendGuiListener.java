package org.kcm.bloomery.fawaymc.friend.listener;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.data.PlayerSettings;
import org.kcm.bloomery.fawaymc.friend.gui.AddFriendGui;
import org.kcm.bloomery.fawaymc.friend.gui.BlocklistGui;
import org.kcm.bloomery.fawaymc.friend.gui.FriendGuiHolder;
import org.kcm.bloomery.fawaymc.friend.gui.FriendListGui;
import org.kcm.bloomery.fawaymc.friend.gui.FriendMainGui;
import org.kcm.bloomery.fawaymc.friend.gui.FriendRequestGui;
import org.kcm.bloomery.fawaymc.friend.gui.FriendSettingsGui;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendGuiListener implements Listener {
   private final Global plugin;
   private final FriendManager manager;
   private static final Set<UUID> transitioning = ConcurrentHashMap.newKeySet();
   private static final long CLICK_COOLDOWN_MS = 300L;
   private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap();
   private static final Map<UUID, String> lastClickAction = new ConcurrentHashMap();

   public FriendGuiListener(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof FriendGuiHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (event.getClick() != ClickType.DOUBLE_CLICK) {
               if (event.getClickedInventory() != null && event.getClickedInventory() == event.getView().getTopInventory()) {
                  ItemStack clicked = event.getCurrentItem();
                  if (clicked != null && clicked.hasItemMeta()) {
                     String action = (String)clicked.getItemMeta().getPersistentDataContainer().get(FriendMainGui.ACTION_KEY, PersistentDataType.STRING);
                     if (action != null) {
                        UUID uuid = player.getUniqueId();
                        long now = System.currentTimeMillis();
                        Long lastTime = (Long)lastClickTime.get(uuid);
                        String lastAct = (String)lastClickAction.get(uuid);
                        if (lastTime == null || lastAct == null || !lastAct.equals(action) || now - lastTime >= 300L) {
                           lastClickTime.put(uuid, now);
                           lastClickAction.put(uuid, action);
                           if (!transitioning.contains(uuid)) {
                              transitioning.add(uuid);
                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> transitioning.remove(uuid), 3L);
                              transitioning.add(player.getUniqueId());
                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> transitioning.remove(player.getUniqueId()), 3L);

                              try {
                                 this.handleAction(player, action, event.getClick());
                              } catch (Throwable t) {
                                 this.plugin.getLogger().warning("[FriendGui] action '" + action + "' error: " + t.getMessage());
                                 transitioning.remove(player.getUniqueId());
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

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onDrag(InventoryDragEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof FriendGuiHolder) {
         event.setCancelled(true);
      }

   }

   private void handleAction(Player player, String action, ClickType click) {
      switch (action) {
         case "noop":
            transitioning.remove(player.getUniqueId());
            return;
         case "close":
            transitioning.remove(player.getUniqueId());
            player.closeInventory();
            return;
         case "open_list":
            (new FriendListGui(this.plugin)).open(player, 1);
            return;
         case "open_requests":
            (new FriendRequestGui(this.plugin)).open(player);
            return;
         case "open_settings":
            (new FriendSettingsGui(this.plugin)).open(player);
            return;
         case "open_add":
            (new AddFriendGui(this.plugin)).open(player);
            return;
         case "open_blocklist":
            (new BlocklistGui(this.plugin)).open(player);
            return;
         case "back":
         case "back_settings":
            Bukkit.getScheduler().runTask(this.plugin, () -> {
               if (action.equals("back_settings")) {
                  (new FriendSettingsGui(this.plugin)).open(player);
               } else {
                  (new FriendMainGui(this.plugin)).open(player);
               }

            });
            return;
         case "toggle_requests":
            this.toggleRequests(player);
            return;
         case "toggle_notify":
            this.toggleNotify(player);
            return;
         default:
            if (action.startsWith("page:")) {
               try {
                  int page = Integer.parseInt(action.substring(5));
                  (new FriendMainGui(this.plugin)).open(player, page);
               } catch (Exception var8) {
                  transitioning.remove(player.getUniqueId());
               }

            } else if (action.startsWith("add:")) {
               UUID targetUuid;
               try {
                  targetUuid = UUID.fromString(action.substring(4));
               } catch (Exception var9) {
                  transitioning.remove(player.getUniqueId());
                  return;
               }

               Player target = Bukkit.getPlayer(targetUuid);
               if (target == null) {
                  player.sendMessage(this.color("&c&lFRIENDS &8» &cPlayer is offline!"));
                  transitioning.remove(player.getUniqueId());
               } else {
                  this.manager.sendRequest(player, target.getName(), (r) -> {
                     player.sendMessage(r.getMessage());

                     try {
                        player.playSound(player.getLocation(), r.isSuccess() ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                     } catch (Exception var4) {
                     }

                     Bukkit.getScheduler().runTask(this.plugin, () -> {
                        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof FriendGuiHolder) {
                           (new AddFriendGui(this.plugin)).open(player);
                        }

                     });
                  });
               }
            } else if (action.startsWith("unblock:")) {
               UUID targetUuid;
               try {
                  targetUuid = UUID.fromString(action.substring(8));
               } catch (Exception var10) {
                  transitioning.remove(player.getUniqueId());
                  return;
               }

               String targetName = this.manager.resolveNamePublic(targetUuid);
               this.manager.unblockPlayer(player, targetName, (r) -> {
                  player.sendMessage(r.getMessage());

                  try {
                     player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
                  } catch (Exception var4) {
                  }

                  Bukkit.getScheduler().runTask(this.plugin, () -> (new BlocklistGui(this.plugin)).open(player));
               });
            } else if (action.startsWith("friend:")) {
               UUID friendUuid;
               try {
                  friendUuid = UUID.fromString(action.substring(7));
               } catch (Exception var13) {
                  transitioning.remove(player.getUniqueId());
                  return;
               }

               String friendName = this.manager.resolveNamePublic(friendUuid);
               Player friend = Bukkit.getPlayer(friendUuid);
               if (click == ClickType.SHIFT_LEFT) {
                  this.manager.removeFriend(player, friendName, (r) -> {
                     player.sendMessage(r.getMessage());

                     try {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                     } catch (Exception var4) {
                     }

                     Bukkit.getScheduler().runTask(this.plugin, () -> (new FriendMainGui(this.plugin)).open(player));
                  });
               } else if (click != ClickType.DROP && click != ClickType.CONTROL_DROP) {
                  if (click == ClickType.LEFT) {
                     if (friend != null && friend.isOnline()) {
                        player.sendMessage(this.color("&b&lFRIENDS &8» &7Use &f/msg " + friendName + " <message> &7to chat."));

                        try {
                           player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
                        } catch (Exception var12) {
                        }
                     } else {
                        player.sendMessage(this.color("&c&lFRIENDS &8» &c" + friendName + " is offline!"));
                     }

                     transitioning.remove(player.getUniqueId());
                  } else if (click == ClickType.RIGHT) {
                     if (this.plugin.getNetworkManager() != null && this.plugin.getNetworkManager().isEnabled()) {
                        this.plugin.getNetworkManager().jumpToPlayer(player, friendUuid);
                     } else if (friend != null && friend.isOnline()) {
                        player.sendMessage(this.color("&b&lFRIENDS &8» &7Teleporting to &f" + friendName));
                        player.teleport(friend.getLocation());

                        try {
                           player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                        } catch (Exception var11) {
                        }
                     } else {
                        player.sendMessage(this.color("&c&lFRIENDS &8» &c" + friendName + " is offline!"));
                     }

                     transitioning.remove(player.getUniqueId());
                  } else {
                     transitioning.remove(player.getUniqueId());
                  }
               } else {
                  this.toggleFavorite(player, friendUuid);
                  transitioning.remove(player.getUniqueId());
                  Bukkit.getScheduler().runTask(this.plugin, () -> (new FriendMainGui(this.plugin)).open(player));
               }

            } else {
               if (action.startsWith("req:")) {
                  UUID senderUuid;
                  try {
                     senderUuid = UUID.fromString(action.substring(4));
                  } catch (Exception var14) {
                     transitioning.remove(player.getUniqueId());
                     return;
                  }

                  if (click == ClickType.LEFT) {
                     this.manager.acceptRequestByUuid(player, senderUuid, (r) -> {
                        player.sendMessage(r.getMessage());

                        try {
                           player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                        } catch (Exception var4) {
                        }

                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                           if (player.isOnline()) {
                              (new FriendRequestGui(this.plugin)).open(player);
                           }

                        });
                     });
                  } else if (click == ClickType.RIGHT) {
                     this.manager.denyRequestByUuid(player, senderUuid, (r) -> {
                        player.sendMessage(r.getMessage());

                        try {
                           player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        } catch (Exception var4) {
                        }

                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                           if (player.isOnline()) {
                              (new FriendRequestGui(this.plugin)).open(player);
                           }

                        });
                     });
                  } else {
                     transitioning.remove(player.getUniqueId());
                  }
               }

            }
      }
   }

   private void toggleRequests(Player player) {
      PlayerSettings s = this.manager.getSettingsCache(player.getUniqueId());
      s.setAcceptRequests(!s.isAcceptRequests());
      this.manager.getDataManager().saveSettings(s);
      player.sendMessage(this.color("&b&lFRIENDS &8» &7Friend requests: " + (s.isAcceptRequests() ? "&aON" : "&cOFF")));

      try {
         player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
      } catch (Exception var4) {
      }

      Bukkit.getScheduler().runTask(this.plugin, () -> (new FriendSettingsGui(this.plugin)).open(player));
   }

   private void toggleNotify(Player player) {
      PlayerSettings s = this.manager.getSettingsCache(player.getUniqueId());
      s.setNotifyOnline(!s.isNotifyOnline());
      this.manager.getDataManager().saveSettings(s);
      player.sendMessage(this.color("&b&lFRIENDS &8» &7Online notify: " + (s.isNotifyOnline() ? "&aON" : "&cOFF")));

      try {
         player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
      } catch (Exception var4) {
      }

      Bukkit.getScheduler().runTask(this.plugin, () -> (new FriendSettingsGui(this.plugin)).open(player));
   }

   private void toggleFavorite(Player player, UUID friendUuid) {
      for(FriendData fd : this.manager.getFriendsCache(player.getUniqueId())) {
         if (fd.getFriendUuid().equals(friendUuid)) {
            fd.setFavorite(!fd.isFavorite());
            player.sendMessage(this.color("&b&lFRIENDS &8» &7" + (fd.isFavorite() ? "&e★ Favorited" : "&7Unfavorited")));

            try {
               player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
            } catch (Exception var6) {
            }

            return;
         }
      }

   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}
