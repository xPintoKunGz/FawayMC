package org.kcm.bloomery.fawaymc.friend.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendAdminCommand implements CommandExecutor, TabCompleter {
   private final Global plugin;
   private final FriendManager manager;

   public FriendAdminCommand(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!sender.hasPermission("friend.admin")) {
         sender.sendMessage(this.color("&cYou don't have permission!"));
         return true;
      } else if (args.length == 0) {
         this.sendUsage(sender);
         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "list":
               if (args.length < 2) {
                  sender.sendMessage(this.color("&eUsage: /friendadmin list <player>"));
                  return true;
               }

               this.listFriends(sender, args[1]);
               return true;
            case "clear":
               if (args.length < 2) {
                  sender.sendMessage(this.color("&eUsage: /friendadmin clear <player>"));
                  return true;
               }

               this.clearFriends(sender, args[1]);
               return true;
            case "cache":
               if (args.length < 2) {
                  sender.sendMessage(this.color("&eUsage: /friendadmin cache <player>"));
                  return true;
               }

               this.clearCache(sender, args[1]);
               return true;
            case "clearcache":
               int size = Bukkit.getOnlinePlayers().size();

               for(Player p : Bukkit.getOnlinePlayers()) {
                  this.plugin.getCache().clear(p.getUniqueId());
               }

               sender.sendMessage(this.color("&a✓ Cleared cache for " + size + " online player(s)"));
               return true;
            case "reload":
               this.plugin.reloadConfig();
               sender.sendMessage(this.color("&a✓ Config reloaded!"));
               return true;
            case "stats":
               sender.sendMessage(this.color("&b&l✦ Friend System Stats ✦"));
               sender.sendMessage(this.color(" &8▪ &fLoaded players: &e" + Bukkit.getOnlinePlayers().size()));
               sender.sendMessage(this.color(" &8▪ &fConfig max_friends: &e" + this.manager.getMaxFriends()));
               return true;
            default:
               this.sendUsage(sender);
               return true;
         }
      }
   }

   private void listFriends(CommandSender sender, String playerName) {
      UUID uuid = this.resolveUuid(playerName);
      if (uuid == null) {
         sender.sendMessage(this.color("&cPlayer not found: " + playerName));
      } else {
         List<FriendData> friends = this.manager.getDataManager().getFriends(uuid);
         String name = this.manager.getDataManager().findPlayerName(uuid);
         if (name == null) {
            name = playerName;
         }

         sender.sendMessage(this.color("&b&l✦ " + name + "'s Friends (" + friends.size() + ") ✦"));
         if (friends.isEmpty()) {
            sender.sendMessage(this.color(" &7No friends."));
         } else {
            for(FriendData fd : friends) {
               String fname = this.manager.getDataManager().findPlayerName(fd.getFriendUuid());
               if (fname == null) {
                  fname = fd.getFriendUuid().toString();
               }

               Player online = Bukkit.getPlayer(fd.getFriendUuid());
               boolean isOnline = online != null && online.isOnline();
               String status = isOnline ? "&a● Online" : "&c● Offline";
               String fav = fd.isFavorite() ? "&e★ " : "";
               sender.sendMessage(this.color(" &8▪ &f" + fav + fname + " &7— " + status));
            }

         }
      }
   }

   private void clearFriends(CommandSender sender, String playerName) {
      UUID uuid = this.resolveUuid(playerName);
      if (uuid == null) {
         sender.sendMessage(this.color("&cPlayer not found: " + playerName));
      } else {
         List<FriendData> friends = this.manager.getDataManager().getFriends(uuid);
         if (friends.isEmpty()) {
            sender.sendMessage(this.color("&e" + playerName + " has no friends to clear."));
         } else {
            for(FriendData fd : friends) {
               this.manager.getDataManager().removeFriendPair(uuid, fd.getFriendUuid());
               Player fp = Bukkit.getPlayer(fd.getFriendUuid());
               if (fp != null && fp.isOnline()) {
                  this.plugin.getCache().removeFriend(fd.getFriendUuid(), uuid);
               }
            }

            this.plugin.getCache().setFriends(uuid, new ArrayList());
            int var10002 = friends.size();
            sender.sendMessage(this.color("&a✓ Cleared " + var10002 + " friend(s) from " + playerName));
            Logger var10000 = this.plugin.getLogger();
            String var10001 = sender.getName();
            var10000.info("[Friends] " + var10001 + " cleared " + friends.size() + " friends of " + playerName);
         }
      }
   }

   private void clearCache(CommandSender sender, String playerName) {
      UUID uuid = this.resolveUuid(playerName);
      if (uuid == null) {
         sender.sendMessage(this.color("&cPlayer not found: " + playerName));
      } else {
         this.plugin.getCache().clear(uuid);
         sender.sendMessage(this.color("&a✓ Cleared cache for " + playerName));
         Player p = Bukkit.getPlayer(uuid);
         if (p != null && p.isOnline()) {
            this.manager.loadPlayerAsync(uuid, p.getName(), () -> sender.sendMessage(this.color("&a✓ Reloaded cache for " + playerName)));
         }

      }
   }

   private UUID resolveUuid(String name) {
      Player p = Bukkit.getPlayerExact(name);
      if (p != null) {
         return p.getUniqueId();
      } else {
         UUID cached = this.plugin.getCache().uuidFromName(name);
         return cached != null ? cached : this.manager.getDataManager().findPlayerUuid(name);
      }
   }

   private void sendUsage(CommandSender sender) {
      sender.sendMessage(this.color("&b&l✦ Friend Admin Commands ✦"));
      sender.sendMessage(this.color(" &8▪ &f/friendadmin list <player>"));
      sender.sendMessage(this.color(" &8▪ &f/friendadmin clear <player>"));
      sender.sendMessage(this.color(" &8▪ &f/friendadmin cache <player>"));
      sender.sendMessage(this.color(" &8▪ &f/friendadmin clearcache"));
      sender.sendMessage(this.color(" &8▪ &f/friendadmin reload"));
      sender.sendMessage(this.color(" &8▪ &f/friendadmin stats"));
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (!sender.hasPermission("friend.admin")) {
         return out;
      } else {
         if (args.length == 1) {
            String input = args[0].toLowerCase();

            for(String s : new String[]{"list", "clear", "cache", "clearcache", "reload", "stats"}) {
               if (s.startsWith(input)) {
                  out.add(s);
               }
            }
         } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("list") || sub.equals("clear") || sub.equals("cache")) {
               String input = args[1].toLowerCase();

               for(Player p : Bukkit.getOnlinePlayers()) {
                  if (p.getName().toLowerCase().startsWith(input)) {
                     out.add(p.getName());
                  }
               }
            }
         }

         return out;
      }
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }
}
