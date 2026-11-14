package org.kcm.bloomery.fawaymc.friend.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import me.clip.placeholderapi.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.BlockData;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.data.FriendRequest;
import org.kcm.bloomery.fawaymc.friend.data.PlayerSettings;
import org.kcm.bloomery.fawaymc.friend.gui.FriendMainGui;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendCommand implements CommandExecutor, TabCompleter {
   private final Global plugin;
   private final FriendManager manager;

   public FriendCommand(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "Only players!");
         return true;
      } else if (args.length == 0) {
         (new FriendMainGui(this.plugin)).open(player);
         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "list":
               this.listFriends(player);
               return true;
            case "add":
               if (args.length < 2) {
                  player.sendMessage(this.msg("usage_add"));
                  return true;
               }

               this.manager.sendRequest(player, args[1], (r) -> player.sendMessage(r.getMessage()));
               return true;
            case "accept":
               if (args.length < 2) {
                  player.sendMessage(this.msg("usage_accept"));
                  return true;
               }

               this.manager.acceptRequest(player, args[1], (r) -> player.sendMessage(r.getMessage()));
               return true;
            case "deny":
               if (args.length < 2) {
                  player.sendMessage(this.msg("usage_deny"));
                  return true;
               }

               this.manager.denyRequest(player, args[1], (r) -> player.sendMessage(r.getMessage()));
               return true;
            case "remove":
               if (args.length < 2) {
                  player.sendMessage(this.msg("usage_remove"));
                  return true;
               }

               this.manager.removeFriend(player, args[1], (r) -> player.sendMessage(r.getMessage()));
               return true;
            case "requests":
            case "pending":
               this.listPending(player);
               return true;
            case "toggle":
               if (args.length < 2) {
                  player.sendMessage(this.msg("usage_toggle"));
                  return true;
               }

               this.toggleSetting(player, args[1].toLowerCase());
               return true;
            case "jump":
            case "tp":
               if (args.length < 2) {
                  player.sendMessage(Msg.color("&e/friend jump <player>"));
                  return true;
               }

               this.handleJump(player, args[1]);
               return true;
            case "block":
               if (args.length < 2) {
                  player.sendMessage(this.msg("usage_block"));
                  return true;
               }

               this.manager.blockPlayer(player, args[1], (r) -> player.sendMessage(r.getMessage()));
               return true;
            case "unblock":
               if (args.length < 2) {
                  player.sendMessage(this.msg("usage_unblock"));
                  return true;
               }

               this.manager.unblockPlayer(player, args[1], (r) -> player.sendMessage(r.getMessage()));
               return true;
            case "blocklist":
               this.listBlocked(player);
               return true;
            case "nickname":
            case "nick":
               if (args.length < 3) {
                  player.sendMessage(this.msg("usage_nickname"));
                  return true;
               }

               String nickname = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 2, args.length));
               this.manager.setNickname(player, args[1], nickname, (r) -> player.sendMessage(r.getMessage()));
               return true;
            case "msg":
            case "tell":
            case "dm":
               if (args.length < 3) {
                  player.sendMessage(this.msg("usage_msg"));
                  return true;
               }

               String dm = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 2, args.length));
               this.handleDirectMessage(player, args[1], dm);
               return true;
            default:
               player.sendMessage(this.msg("usage_main"));
               return true;
         }
      }
   }

   private void listFriends(Player player) {
      List<FriendData> friends = this.manager.getFriendsCache(player.getUniqueId());
      if (friends.isEmpty()) {
         player.sendMessage(this.msg("friend_list_empty"));
      } else {
         player.sendMessage(this.msg("friend_list_header").replace("{count}", String.valueOf(friends.size())).replace("{max}", String.valueOf(this.manager.getMaxFriends())));

         for(FriendData fd : friends) {
            UUID fid = fd.getFriendUuid();
            String name = this.manager.resolveNamePublic(fid);
            Player online = this.plugin.getServer().getPlayer(fid);
            boolean isOnline = online != null && online.isOnline();
            String status = isOnline ? "&a● Online" : "&c● Offline";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', " &8▪ &f" + name + " &7— " + status));
         }

      }
   }

   private void listPending(Player player) {
      List<FriendRequest> pending = this.manager.getPendingCache(player.getUniqueId());
      if (pending.isEmpty()) {
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7You have no pending requests."));
      } else {
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&l✦ &fPending Requests &7(" + pending.size() + ") &b&l✦"));

         for(FriendRequest req : pending) {
            String name = this.manager.getCache().getName(req.getSenderUuid());
            if (name == null) {
               name = "Unknown";
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', " &8▪ &f" + name + " &7— &a/friend accept " + name));
         }

      }
   }

   private void toggleSetting(Player player, String type) {
      PlayerSettings s = this.manager.getSettingsCache(player.getUniqueId());
      switch (type) {
         case "requests":
         case "request":
            s.setAcceptRequests(!s.isAcceptRequests());
            this.manager.getDataManager().saveSettings(s);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aFriend requests: " + (s.isAcceptRequests() ? "&aON" : "&cOFF")));
            break;
         case "notify":
         case "online":
            s.setNotifyOnline(!s.isNotifyOnline());
            this.manager.getDataManager().saveSettings(s);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aOnline notify: " + (s.isNotifyOnline() ? "&aON" : "&cOFF")));
            break;
         default:
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUnknown setting. Use: requests / notify"));
      }

   }

   private void handleJump(Player player, String targetName) {
      if (this.plugin.getNetworkManager() != null && this.plugin.getNetworkManager().isEnabled()) {
         UUID targetUuid = null;
         Player online = Bukkit.getPlayerExact(targetName);
         if (online != null) {
            targetUuid = online.getUniqueId();
         }

         if (targetUuid == null) {
            targetUuid = this.manager.getCache().uuidFromName(targetName);
         }

         if (targetUuid == null) {
            targetUuid = this.manager.getDataManager().findPlayerUuid(targetName);
         }

         if (targetUuid == null) {
            player.sendMessage(Msg.color("&c&lFRIENDS &8» &cPlayer not found!"));
         } else if (!this.manager.getDataManager().isFriend(player.getUniqueId(), targetUuid)) {
            player.sendMessage(Msg.color("&c&lFRIENDS &8» &cYou are not friends with that player!"));
         } else {
            this.plugin.getNetworkManager().jumpToPlayer(player, targetUuid);
         }
      } else {
         player.sendMessage(Msg.color("&c&lFRIENDS &8» &cCross-server is not available!"));
      }
   }

   private void listBlocked(Player player) {
      List<BlockData> blocks = this.manager.getBlockedList(player.getUniqueId());
      if (blocks.isEmpty()) {
         player.sendMessage(this.msg("blocklist_empty"));
      } else {
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l✦ &fBlocked Players &7(" + blocks.size() + ") &c&l✦"));

         for(BlockData b : blocks) {
            String name = this.manager.getDataManager().findPlayerName(b.getBlockedUuid());
            if (name == null) {
               name = b.getBlockedUuid().toString();
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', " &8▪ &c" + name + " &7(/friend unblock " + name + ")"));
         }

      }
   }

   private void handleDirectMessage(Player player, String targetName, String message) {
      UUID targetUuid = null;
      Player online = Bukkit.getPlayerExact(targetName);
      if (online != null) {
         targetUuid = online.getUniqueId();
      }

      if (targetUuid == null) {
         targetUuid = this.manager.getCache().uuidFromName(targetName);
      }

      if (targetUuid == null) {
         targetUuid = this.manager.getDataManager().findPlayerUuid(targetName);
      }

      if (targetUuid == null) {
         player.sendMessage(this.msg("player_not_found").replace("{player}", targetName));
      } else if (!this.manager.getDataManager().isFriend(player.getUniqueId(), targetUuid)) {
         player.sendMessage(this.msg("not_friends").replace("{player}", targetName));
      } else {
         String targetDisplay = this.manager.resolveNamePublic(targetUuid);
         if (targetDisplay.equals("Unknown")) {
            targetDisplay = targetName;
         }

         player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d&lDM &8» &fYou &7→ &f" + targetDisplay + "&7: &f" + message));
         Player targetLocal = Bukkit.getPlayer(targetUuid);
         if (targetLocal != null && targetLocal.isOnline()) {
            String var10002 = player.getName();
            targetLocal.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d&lDM &8» &f" + var10002 + " &7→ &fYou&7: &f" + message));

            try {
               targetLocal.playSound(targetLocal.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
            } catch (Throwable var9) {
            }

         } else if (this.plugin.getNetworkManager() != null && this.plugin.getNetworkManager().isEnabled() && this.plugin.getNetworkManager().isOnlineOnNetwork(targetUuid)) {
            this.plugin.getNetworkManager().publishDirectMessage(player.getUniqueId(), targetUuid, player.getName(), targetDisplay, message);
         } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lFRIENDS &8» &c" + targetDisplay + " is offline"));
         }
      }
   }

   private String msg(String key) {
      String raw = this.plugin.getConfig().getString("messages." + key, "&cMissing: " + key);
      String prefix = this.plugin.getConfig().getString("messages.prefix", "");
      return ChatColor.translateAlternateColorCodes('&', prefix + raw);
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (!(sender instanceof Player player)) {
         return out;
      } else {
         if (args.length == 1) {
            String input = args[0].toLowerCase();

            for(String s : new String[]{"list", "add", "accept", "deny", "remove", "requests", "toggle", "block", "unblock", "blocklist", "nickname", "msg"}) {
               if (s.startsWith(input)) {
                  out.add(s);
               }
            }
         } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            if (!sub.equals("add") && !sub.equals("remove")) {
               if (sub.equals("accept") || sub.equals("deny")) {
                  for(FriendRequest req : this.manager.getPendingCache(player.getUniqueId())) {
                     String name = this.manager.getCache().getName(req.getSenderUuid());
                     if (name != null && name.toLowerCase().startsWith(input)) {
                        out.add(name);
                     }
                  }
               } else if (sub.equals("toggle")) {
                  if ("requests".startsWith(input)) {
                     out.add("requests");
                  }

                  if ("notify".startsWith(input)) {
                     out.add("notify");
                  }
               }
            } else {
               for(Player p : this.plugin.getServer().getOnlinePlayers()) {
                  if (p != player && p.getName().toLowerCase().startsWith(input)) {
                     out.add(p.getName());
                  }
               }
            }
         }

         return out;
      }
   }
}
