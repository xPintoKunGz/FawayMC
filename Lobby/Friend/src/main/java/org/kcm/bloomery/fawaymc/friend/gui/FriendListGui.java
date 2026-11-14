package org.kcm.bloomery.fawaymc.friend.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendListGui {
   private final Global plugin;
   private final FriendManager manager;

   public FriendListGui(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public void open(Player player, int page) {
      (new FriendMainGui(this.plugin)).open(player);
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}
