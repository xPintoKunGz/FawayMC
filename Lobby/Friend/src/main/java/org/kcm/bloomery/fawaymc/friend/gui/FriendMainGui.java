package org.kcm.bloomery.fawaymc.friend.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendMainGui {
   private final Global plugin;
   private final FriendManager manager;
   public static final NamespacedKey ACTION_KEY = new NamespacedKey("fawayfriend", "gui_action");
   private static final int ITEMS_PER_PAGE = 36;

   public FriendMainGui(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public void open(Player player) {
      this.open(player, 0);
   }

   public void open(Player player, int page) {
      String title = this.color(this.plugin.getConfig().getString("gui.title", "&8&l✦ &b&lFRIENDS &8&l✦"));
      FriendGuiHolder holder = new FriendGuiHolder(FriendGuiHolder.Type.MAIN);
      Inventory inv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(inv);
      ItemStack border = this.createBorder();
      ItemStack filler = this.createFiller();

      for(int i = 0; i < 54; ++i) {
         inv.setItem(i, filler);
      }

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      inv.setItem(4, this.createPlayerHead(player));
      List<FriendData> friends = this.manager.getFriendsCache(player.getUniqueId());
      this.sortFriends(friends);
      int totalPages = Math.max(1, (int)Math.ceil((double)friends.size() / (double)36.0F));
      page = Math.max(0, Math.min(page, totalPages - 1));
      int start = page * 36;
      int end = Math.min(start + 36, friends.size());

      for(int i = start; i < end; ++i) {
         int slot = 9 + (i - start);
         inv.setItem(slot, this.createFriendItem((FriendData)friends.get(i)));
      }

      inv.setItem(45, this.createButton(Material.WRITABLE_BOOK, "&e&l➕ &e&lAdd Friend", List.of("&7Send a friend request", "&7to another player.", "", "&eClick to open!"), "open_add"));
      if (page > 0) {
         inv.setItem(46, this.createButton(Material.ARROW, "&a← &aPrevious Page", List.of("&7Page: &f" + page + "/" + totalPages), "page:" + (page - 1)));
      }

      inv.setItem(47, this.createButton(Material.PAPER, "&f&lPage &e" + (page + 1) + "&7/&f" + totalPages, List.of("&7Total friends: &e" + friends.size(), "", "&7Use arrows to navigate"), "noop"));
      if (page < totalPages - 1) {
         inv.setItem(48, this.createButton(Material.ARROW, "&aNext Page →", List.of("&7Page: &f" + (page + 2) + "/" + totalPages), "page:" + (page + 1)));
      }

      int pendingCount = this.manager.getPendingCache(player.getUniqueId()).size();
      Material reqMat = pendingCount > 0 ? Material.WRITABLE_BOOK : Material.BOOK;
      List<String> reqLore = new ArrayList();
      reqLore.add(this.color("&7View incoming friend requests."));
      reqLore.add("");
      if (pendingCount > 0) {
         reqLore.add(this.color("&e&lPending: &f" + pendingCount));
         reqLore.add("");
         reqLore.add(this.color("&aClick to view!"));
      } else {
         reqLore.add(this.color("&7No pending requests."));
      }

      inv.setItem(49, this.createButton(reqMat, "&b&lFriend Requests", reqLore, "open_requests"));
      inv.setItem(50, this.createButton(Material.COMPARATOR, "&9&lSettings", List.of("&7Manage your friend settings:", "", "&8■ &7Accept requests", "&8■ &7Online notifications", "&8■ &7Blocked players", "", "&eClick to open!"), "open_settings"));
      inv.setItem(51, this.createButton(Material.BOOK, "&eℹ &lInfo", List.of("&7Friend System Help", "", "&8■ &f/friend add &a<player>", "&8■ &f/friend accept &a<player>", "&8■ &f/friend remove &a<player>", "&8■ &f/friend requests", "&8■ &f/friend toggle &a<setting>"), "noop"));
      inv.setItem(53, this.createButton(Material.BARRIER, "&c&l✘ &c&lClose", List.of("&7Close this menu"), "close"));
      player.openInventory(inv);
      this.playClick(player);
   }

   private void sortFriends(List<FriendData> friends) {
      String mode = this.plugin.getConfig().getString("gui.sort.mode", "online_first");
      friends.sort((a, b) -> {
         boolean aOnline = Bukkit.getPlayer(a.getFriendUuid()) != null;
         boolean bOnline = Bukkit.getPlayer(b.getFriendUuid()) != null;
         if (mode.equals("favorite_first") && a.isFavorite() != b.isFavorite()) {
            return a.isFavorite() ? -1 : 1;
         } else if ((mode.equals("online_first") || mode.equals("favorite_first")) && aOnline != bOnline) {
            return aOnline ? -1 : 1;
         } else {
            String nameA = this.manager.getCache().getName(a.getFriendUuid());
            String nameB = this.manager.getCache().getName(b.getFriendUuid());
            if (nameA == null) {
               nameA = "zzz";
            }

            if (nameB == null) {
               nameB = "zzz";
            }

            return nameA.compareToIgnoreCase(nameB);
         }
      });
   }

   private ItemStack createFriendItem(FriendData fd) {
      UUID fid = fd.getFriendUuid();
      String realName = this.manager.resolveNamePublic(fid);
      String displayName = fd.getNickname() != null && !fd.getNickname().isEmpty() ? fd.getNickname() : realName;
      Player online = Bukkit.getPlayer(fid);
      boolean isOnline = online != null && online.isOnline();
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (meta == null) {
         return head;
      } else {
         if (online != null) {
            meta.setOwningPlayer(online);
         } else {
            try {
               meta.setOwningPlayer(Bukkit.getOfflinePlayer(fid));
            } catch (Exception var14) {
            }
         }

         String title = (isOnline ? "&a" : "&7") + displayName;
         if (fd.isFavorite()) {
            title = "&e★ " + title;
         }

         meta.setDisplayName(this.color(title));
         List<String> lore = new ArrayList();
         if (isOnline) {
            lore.add(this.color("&a&l● &aOnline"));
            lore.add(this.color("&7World: &f" + online.getWorld().getName()));
            if (this.plugin.getNetworkManager() != null && this.plugin.getNetworkManager().isEnabled()) {
               String server = this.plugin.getNetworkManager().getServerOf(fid);
               if (server != null) {
                  lore.add(this.color("&7Server: &e" + server));
               }
            }
         } else {
            lore.add(this.color("&c&l● &cOffline"));
            if (this.plugin.getNetworkManager() != null && this.plugin.getNetworkManager().isEnabled()) {
               String server = this.plugin.getNetworkManager().getServerOf(fid);
               if (server != null && !server.isEmpty()) {
                  lore.add(this.color("&e● Online on &f" + server));
               } else {
                  long last = this.manager.getDataManager().getLastSeen(fid);
                  if (last > 0L) {
                     String var17 = this.formatTime(System.currentTimeMillis() - last);
                     lore.add(this.color("&7Last Seen: &f" + var17 + " ago"));
                  }
               }
            } else {
               long last = this.manager.getDataManager().getLastSeen(fid);
               if (last > 0L) {
                  String var10002 = this.formatTime(System.currentTimeMillis() - last);
                  lore.add(this.color("&7Last Seen: &f" + var10002 + " ago"));
               }
            }
         }

         lore.add("");
         if (!displayName.equals(realName)) {
            lore.add(this.color("&7Real Name: &f" + realName));
         }

         String var18 = this.formatTime(System.currentTimeMillis() - fd.getCreatedAt());
         lore.add(this.color("&7Friends Since: &f" + var18 + " ago"));
         lore.add("");
         if (isOnline) {
            lore.add(this.color("&e▸ Left-Click &7to Message"));
            lore.add(this.color("&e▸ Right-Click &7to Teleport"));
         }

         lore.add(this.color("&c▸ Shift+Left &7to Remove"));
         lore.add(this.color("&6▸ Drop (Q) &7to " + (fd.isFavorite() ? "Unfavorite" : "Favorite")));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "friend:" + String.valueOf(fid));
         head.setItemMeta(meta);
         return head;
      }
   }

   private ItemStack createPlayerHead(Player player) {
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (meta == null) {
         return head;
      } else {
         meta.setOwningPlayer(player);
         meta.setDisplayName(this.color("&6&l" + player.getName()));
         int friendCount = this.manager.getFriendsCache(player.getUniqueId()).size();
         int pendingCount = this.manager.getPendingCache(player.getUniqueId()).size();
         int max = this.manager.getMaxFriends();
         List<String> lore = new ArrayList();
         lore.add("");
         lore.add(this.color("&eYour Profile:"));
         lore.add(this.color("&7Friends: &a" + friendCount + "&7/&f" + max));
         lore.add(this.color("&7Pending Requests: &e" + pendingCount));
         lore.add("");
         lore.add(this.color("&7Use the buttons below"));
         lore.add(this.color("&7to manage your friends."));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "noop");
         head.setItemMeta(meta);
         return head;
      }
   }

   private ItemStack createButton(Material mat, String name, List<String> lore, String action) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setDisplayName(this.color(name));
         List<String> colored = new ArrayList();

         for(String s : lore) {
            colored.add(this.color(s));
         }

         meta.setLore(colored);
         meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createBorder() {
      Material mat = Material.matchMaterial(this.plugin.getConfig().getString("gui.border.material", "BLACK_STAINED_GLASS_PANE"));
      if (mat == null) {
         mat = Material.BLACK_STAINED_GLASS_PANE;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta m = item.getItemMeta();
      if (m != null) {
         m.setDisplayName(" ");
         item.setItemMeta(m);
      }

      return item;
   }

   private ItemStack createFiller() {
      Material mat = Material.matchMaterial(this.plugin.getConfig().getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
      if (mat == null) {
         mat = Material.GRAY_STAINED_GLASS_PANE;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta m = item.getItemMeta();
      if (m != null) {
         m.setDisplayName(" ");
         item.setItemMeta(m);
      }

      return item;
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   private void playClick(Player p) {
      try {
         p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
      } catch (Exception var3) {
      }

   }

   private String formatTime(long ms) {
      long sec = ms / 1000L;
      if (sec < 60L) {
         return sec + "s";
      } else {
         long min = sec / 60L;
         if (min < 60L) {
            return min + "m";
         } else {
            long hr = min / 60L;
            if (hr < 24L) {
               return hr + "h";
            } else {
               long days = hr / 24L;
               if (days < 7L) {
                  return days + "d";
               } else {
                  long weeks = days / 7L;
                  return weeks < 4L ? weeks + "w" : weeks / 4L + "mo";
               }
            }
         }
      }
   }
}
