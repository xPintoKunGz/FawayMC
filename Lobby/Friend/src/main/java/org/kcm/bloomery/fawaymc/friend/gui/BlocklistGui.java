package org.kcm.bloomery.fawaymc.friend.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.BlockData;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class BlocklistGui {
   private final Global plugin;
   private final FriendManager manager;

   public BlocklistGui(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public void open(Player player) {
      String title = this.color("&8&l✦ &c&lBLOCKED PLAYERS &8&l✦");
      FriendGuiHolder holder = new FriendGuiHolder(FriendGuiHolder.Type.SETTINGS);
      Inventory inv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(inv);
      ItemStack border = this.createStatic(Material.BLACK_STAINED_GLASS_PANE, " ");
      ItemStack filler = this.createStatic(Material.GRAY_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 54; ++i) {
         inv.setItem(i, filler);
      }

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      List<BlockData> blocks = this.manager.getBlockedList(player.getUniqueId());
      if (blocks.isEmpty()) {
         inv.setItem(22, this.createStatic(Material.BARRIER, this.color("&c&lNo Blocked Players"), List.of(this.color("&7You haven't blocked anyone."))));
      } else {
         int idx = 0;

         for(BlockData b : blocks) {
            if (idx >= 36) {
               break;
            }

            inv.setItem(9 + idx, this.createBlockItem(player, b));
            ++idx;
         }
      }

      inv.setItem(45, this.createStatic(Material.ARROW, this.color("&e← Back"), List.of(this.color("&7Return to Settings")), "back_settings"));
      player.openInventory(inv);
   }

   private ItemStack createBlockItem(Player viewer, BlockData b) {
      String name = this.manager.getDataManager().findPlayerName(b.getBlockedUuid());
      if (name == null) {
         name = b.getBlockedUuid().toString();
      }

      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (meta == null) {
         return head;
      } else {
         try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(b.getBlockedUuid()));
         } catch (Exception var7) {
         }

         meta.setDisplayName(this.color("&c" + name));
         List<String> lore = new ArrayList();
         lore.add(this.color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         String var10002 = this.formatTime(System.currentTimeMillis() - b.getCreatedAt());
         lore.add(this.color("&7Blocked &f" + var10002 + " &7ago"));
         lore.add(this.color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         lore.add("");
         lore.add(this.color("&c&lRight-Click &7— &cUnblock"));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(FriendMainGui.ACTION_KEY, PersistentDataType.STRING, "unblock:" + String.valueOf(b.getBlockedUuid()));
         head.setItemMeta(meta);
         return head;
      }
   }

   private ItemStack createStatic(Material mat, String name, List<String> lore, String action) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
         }

         if (action != null) {
            meta.getPersistentDataContainer().set(FriendMainGui.ACTION_KEY, PersistentDataType.STRING, action);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createStatic(Material mat, String name) {
      return this.createStatic(mat, name, (List)null, (String)null);
   }

   private ItemStack createStatic(Material mat, String name, List<String> lore) {
      return this.createStatic(mat, name, lore, (String)null);
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
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
                  return weeks + "w";
               }
            }
         }
      }
   }
}
