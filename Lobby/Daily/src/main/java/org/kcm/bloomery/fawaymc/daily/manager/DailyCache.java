package org.kcm.bloomery.fawaymc.daily.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.kcm.bloomery.fawaymc.daily.data.PlayerStreak;

public class DailyCache {
   private final Map<UUID, PlayerStreak> streaks = new ConcurrentHashMap();
   private final Map<UUID, String> names = new ConcurrentHashMap();

   public PlayerStreak get(UUID uuid) {
      return (PlayerStreak)this.streaks.computeIfAbsent(uuid, PlayerStreak::new);
   }

   public void set(UUID uuid, PlayerStreak streak) {
      this.streaks.put(uuid, streak);
   }

   public void remove(UUID uuid) {
      this.streaks.remove(uuid);
      this.names.remove(uuid);
   }

   public String getName(UUID uuid) {
      return (String)this.names.get(uuid);
   }

   public void setName(UUID uuid, String name) {
      this.names.put(uuid, name);
   }
}
