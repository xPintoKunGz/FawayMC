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
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.BlockData;
import org.kcm.bloomery.fawaymc.friend.data.PlayerSettings;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendSettingsGui {
   private final Global plugin;
   private final FriendManager manager;

   public FriendSettingsGui(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public void open(Player player) {
      String title = this.color("&8&l✦ &6&lFRIEND SETTINGS &8&l✦");
      FriendGuiHolder holder = new FriendGuiHolder(FriendGuiHolder.Type.SETTINGS);
      Inventory inv = Bukkit.createInventory(holder, 36, title);
      holder.setInventory(inv);
      ItemStack border = this.createStatic(Material.BLACK_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 36; ++i) {
         inv.setItem(i, border);
      }

      PlayerSettings s = this.manager.getSettingsCache(player.getUniqueId());
      Material reqMat = s.isAcceptRequests() ? Material.LIME_DYE : Material.GRAY_DYE;
      String reqStatus = s.isAcceptRequests() ? "&a&lENABLED" : "&c&lDISABLED";
      List<String> reqLore = new ArrayList();
      reqLore.add(this.color("&7Allow other players to"));
      reqLore.add(this.color("&7send you friend requests."));
      reqLore.add("");
      reqLore.add(this.color("&8■ &fStatus: " + reqStatus));
      reqLore.add("");
      reqLore.add(this.color("&eClick to toggle!"));
      inv.setItem(11, this.createButton(reqMat, "&e\ud83d\udce9 &e&lFriend Requests", reqLore, "toggle_requests"));
      List<BlockData> blocks = this.manager.getBlockedList(player.getUniqueId());
      Material var10003 = Material.BARRIER;
      String var10005 = this.color("&7Manage your blocked players.");
      int var10008 = blocks.size();
      inv.setItem(13, this.createButton(var10003, "&c&l\ud83d\udeab &c&lBlocked Players", List.of(var10005, "", this.color("&8■ &fBlocked: &c" + var10008), "", this.color("&eClick to open!")), "open_blocklist"));
      Material notifyMat = s.isNotifyOnline() ? Material.LIME_DYE : Material.GRAY_DYE;
      String notifyStatus = s.isNotifyOnline() ? "&a&lENABLED" : "&c&lDISABLED";
      List<String> notifyLore = new ArrayList();
      notifyLore.add(this.color("&7Receive notifications when"));
      notifyLore.add(this.color("&7your friends come online."));
      notifyLore.add("");
      notifyLore.add(this.color("&8■ &fStatus: " + notifyStatus));
      notifyLore.add("");
      notifyLore.add(this.color("&eClick to toggle!"));
      inv.setItem(15, this.createButton(notifyMat, "&b\ud83d\udd14 &b&lOnline Notify", notifyLore, "toggle_notify"));
      inv.setItem(22, this.createButton(Material.ARROW, "&e← &e&lBack", List.of(this.color("&7Return to Friends menu")), "back"));
      player.openInventory(inv);
   }

   private ItemStack createButton(Material mat, String name, List<String> lore, String action) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(this.color(name));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(FriendMainGui.ACTION_KEY, PersistentDataType.STRING, action);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createStatic(Material mat, String name) {
      ItemStack item = new ItemStack(mat);
      ItemMeta m = item.getItemMeta();
      if (m != null) {
         m.setDisplayName(name);
         item.setItemMeta(m);
      }

      return item;
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}
