package org.kcm.bloomery.fawaymc.friend.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class AddFriendGui {
   private final Global plugin;
   private final FriendManager manager;

   public AddFriendGui(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public void open(Player player) {
      String title = this.color("&8&l✦ &e&lADD FRIEND &8&l✦");
      FriendGuiHolder holder = new FriendGuiHolder(FriendGuiHolder.Type.MAIN);
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

      ItemStack searchItem = new ItemStack(Material.COMPASS);
      ItemMeta searchMeta = searchItem.getItemMeta();
      if (searchMeta != null) {
         searchMeta.setDisplayName(this.color("&e&l\ud83d\udd0d &e&lSearch Player"));
         List<String> lore = new ArrayList();
         lore.add(this.color("&7Type &f/friend add <player>"));
         lore.add(this.color("&7to send a friend request."));
         lore.add("");
         lore.add(this.color("&7Or click on a player"));
         lore.add(this.color("&7head below to add them."));
         searchMeta.setLore(lore);
         searchItem.setItemMeta(searchMeta);
      }

      inv.setItem(13, searchItem);
      int idx = 0;

      for(Player online : Bukkit.getOnlinePlayers()) {
         if (!online.equals(player)) {
            if (idx >= 28) {
               break;
            }

            int slot = 18 + idx;
            inv.setItem(slot, this.createOnlinePlayerHead(player, online));
            ++idx;
         }
      }

      inv.setItem(45, this.createStatic(Material.ARROW, this.color("&e← Back"), List.of(this.color("&7Return to Friends menu")), "back"));
      player.openInventory(inv);
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
   }

   private ItemStack createOnlinePlayerHead(Player viewer, Player target) {
      FriendManager manager = this.plugin.getFriendManager();
      boolean isFriend = manager.getDataManager().isFriend(viewer.getUniqueId(), target.getUniqueId());
      boolean hasPending = manager.getDataManager().hasRequest(viewer.getUniqueId(), target.getUniqueId());
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)head.getItemMeta();
      if (meta == null) {
         return head;
      } else {
         meta.setOwningPlayer(target);
         meta.setDisplayName(this.color("&a" + target.getName()));
         List<String> lore = new ArrayList();
         lore.add(this.color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         if (isFriend) {
            lore.add(this.color("&a✔ Already Friends"));
         } else if (hasPending) {
            lore.add(this.color("&e⏳ Request Pending"));
         } else {
            lore.add(this.color("&a● Online"));
            lore.add(this.color(" &8▪ &7World: &f" + target.getWorld().getName()));
            lore.add("");
            lore.add(this.color("&eLeft-Click &7— Send request"));
         }

         lore.add(this.color("&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(FriendMainGui.ACTION_KEY, PersistentDataType.STRING, "add:" + String.valueOf(target.getUniqueId()));
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

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}
