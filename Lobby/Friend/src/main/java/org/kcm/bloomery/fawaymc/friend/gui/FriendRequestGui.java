package org.kcm.bloomery.fawaymc.friend.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.kcm.bloomery.fawaymc.friend.data.FriendRequest;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendRequestGui {
   private final Global plugin;
   private final FriendManager manager;

   public FriendRequestGui(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public void open(Player player) {
      String title = this.color("&8&l✦ &e&lFRIEND REQUESTS &8&l✦");
      FriendGuiHolder holder = new FriendGuiHolder(FriendGuiHolder.Type.REQUESTS);
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

      List<FriendRequest> pending = this.manager.getPendingCache(player.getUniqueId());
      if (pending.isEmpty()) {
         inv.setItem(22, this.createStatic(Material.BARRIER, this.color("&c&lNo Pending Requests"), List.of(this.color("&7You have no incoming friend requests."))));
      } else {
         int idx = 0;

         for(FriendRequest req : pending) {
            if (idx >= 36) {
               break;
            }

            inv.setItem(9 + idx, this.createRequestItem(req));
            ++idx;
         }
      }

      inv.setItem(45, this.createStatic(Material.ARROW, this.color("&e← Back"), List.of(this.color("&7Return to Friends menu")), "back"));
      player.openInventory(inv);
   }

   private ItemStack createRequestItem(FriendRequest req) {
      UUID sender = req.getSenderUuid();
      String name = this.manager.resolveNamePublic(sender);
      Player online = Bukkit.getPlayer(sender);
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (meta == null) {
         return head;
      } else {
         if (online != null) {
            meta.setOwningPlayer(online);
         }

         meta.setDisplayName(this.color("&e\ud83d\udce9 &f" + name));
         List<String> lore = new ArrayList();
         lore.add(this.color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         lore.add(this.color("&7Sent you a friend request."));
         lore.add(this.color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         lore.add("");
         lore.add(this.color("&a&lLeft-Click &7— &aAccept"));
         lore.add(this.color("&c&lRight-Click &7— &cDeny"));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(FriendMainGui.ACTION_KEY, PersistentDataType.STRING, "req:" + String.valueOf(sender));
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
}
