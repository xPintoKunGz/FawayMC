package org.kcm.bloomery.fawaymc.friend.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.data.FriendRequest;
import org.kcm.bloomery.fawaymc.friend.data.PlayerSettings;

public class FriendCache {
   private final Map<UUID, List<FriendData>> friends = new ConcurrentHashMap();
   private final Map<UUID, List<FriendRequest>> pending = new ConcurrentHashMap();
   private final Map<UUID, PlayerSettings> settings = new ConcurrentHashMap();
   private final Map<UUID, String> names = new ConcurrentHashMap();

   public List<FriendData> getFriends(UUID uuid) {
      return (List)this.friends.computeIfAbsent(uuid, (k) -> new ArrayList());
   }

   public void setFriends(UUID uuid, List<FriendData> list) {
      this.friends.put(uuid, new ArrayList(list));
   }

   public void addFriend(UUID uuid, FriendData data) {
      ((List)this.friends.computeIfAbsent(uuid, (k) -> new ArrayList())).add(data);
   }

   public void removeFriend(UUID player, UUID friend) {
      List<FriendData> list = (List)this.friends.get(player);
      if (list != null) {
         list.removeIf((fd) -> fd.getFriendUuid().equals(friend));
      }

   }

   public List<FriendRequest> getPending(UUID uuid) {
      return (List)this.pending.computeIfAbsent(uuid, (k) -> new ArrayList());
   }

   public void setPending(UUID uuid, List<FriendRequest> list) {
      this.pending.put(uuid, new ArrayList(list));
   }

   public void addPending(UUID uuid, FriendRequest req) {
      ((List)this.pending.computeIfAbsent(uuid, (k) -> new ArrayList())).add(req);
   }

   public void removePending(UUID target, UUID sender) {
      List<FriendRequest> list = (List)this.pending.get(target);
      if (list != null) {
         list.removeIf((r) -> r.getSenderUuid().equals(sender));
      }

   }

   public PlayerSettings getSettings(UUID uuid) {
      return (PlayerSettings)this.settings.computeIfAbsent(uuid, (k) -> new PlayerSettings(k, true, true));
   }

   public void setSettings(UUID uuid, PlayerSettings s) {
      this.settings.put(uuid, s);
   }

   public String getName(UUID uuid) {
      return (String)this.names.get(uuid);
   }

   public void setName(UUID uuid, String name) {
      this.names.put(uuid, name);
   }

   public UUID uuidFromName(String name) {
      for(Map.Entry<UUID, String> e : this.names.entrySet()) {
         if (((String)e.getValue()).equalsIgnoreCase(name)) {
            return (UUID)e.getKey();
         }
      }

      return null;
   }

   public void clear(UUID uuid) {
      this.friends.remove(uuid);
      this.pending.remove(uuid);
      this.settings.remove(uuid);
   }
}
