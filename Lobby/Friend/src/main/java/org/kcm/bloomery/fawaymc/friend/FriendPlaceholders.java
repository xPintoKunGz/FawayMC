package org.kcm.bloomery.fawaymc.friend;

import java.util.List;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.data.FriendRequest;
import org.kcm.bloomery.fawaymc.friend.manager.FriendCache;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class FriendPlaceholders extends PlaceholderExpansion {
   private final Global plugin;

   public FriendPlaceholders(Global plugin) {
      this.plugin = plugin;
   }

   public String getIdentifier() {
      return "friend";
   }

   public String getAuthor() {
      return "FawayMC";
   }

   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer p, String params) {
      if (p != null && p.getUniqueId() != null) {
         UUID uuid = p.getUniqueId();
         String key = params.toLowerCase();
         FriendManager manager = this.plugin.getFriendManager();
         FriendCache cache = this.plugin.getCache();
         if (key.equals("count")) {
            return String.valueOf(manager.getFriendsCache(uuid).size());
         } else if (key.equals("max")) {
            return String.valueOf(manager.getMaxFriends());
         } else if (key.equals("pending")) {
            return String.valueOf(manager.getPendingCache(uuid).size());
         } else if (key.equals("online_count")) {
            int count = 0;

            for(FriendData fd : manager.getFriendsCache(uuid)) {
               Player fp = Bukkit.getPlayer(fd.getFriendUuid());
               if (fp != null && fp.isOnline()) {
                  ++count;
               }
            }

            return String.valueOf(count);
         } else if (key.startsWith("is_friend_")) {
            String targetName = params.substring("is_friend_".length());
            Player tp = Bukkit.getPlayerExact(targetName);
            if (tp == null) {
               return "no";
            } else {
               return manager.getDataManager().isFriend(uuid, tp.getUniqueId()) ? "yes" : "no";
            }
         } else if (key.startsWith("is_requested_by_")) {
            String targetName = params.substring("is_requested_by_".length());
            Player tp = Bukkit.getPlayerExact(targetName);
            if (tp == null) {
               return "no";
            } else {
               for(FriendRequest req : manager.getPendingCache(uuid)) {
                  if (req.getSenderUuid().equals(tp.getUniqueId())) {
                     return "yes";
                  }
               }

               return "no";
            }
         } else if (key.startsWith("friend_name_")) {
            try {
               int idx = Integer.parseInt(params.substring("friend_name_".length()));
               List<FriendData> friends = manager.getFriendsCache(uuid);
               if (idx >= 0 && idx < friends.size()) {
                  String name = cache.getName(((FriendData)friends.get(idx)).getFriendUuid());
                  return name != null ? name : "Unknown";
               }
            } catch (Exception var12) {
            }

            return "";
         } else if (!key.startsWith("friend_online_")) {
            return "";
         } else {
            try {
               int idx = Integer.parseInt(params.substring("friend_online_".length()));
               List<FriendData> friends = manager.getFriendsCache(uuid);
               if (idx >= 0 && idx < friends.size()) {
                  Player fp = Bukkit.getPlayer(((FriendData)friends.get(idx)).getFriendUuid());
                  return fp != null && fp.isOnline() ? "yes" : "no";
               }
            } catch (Exception var13) {
            }

            return "no";
         }
      } else {
         return "";
      }
   }
}
