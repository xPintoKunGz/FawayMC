package org.kcm.bloomery.fawaymc.advancement.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerProgress {
   private final UUID uuid;
   private final Map<String, Integer> progress = new HashMap();
   private final Set<String> unlocked = new HashSet();
   private long totalPlaytimeMinutes = 0L;

   public PlayerProgress(UUID uuid) {
      this.uuid = uuid;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public Map<String, Integer> getProgress() {
      return this.progress;
   }

   public Set<String> getUnlocked() {
      return this.unlocked;
   }

   public int getProgress(String advId) {
      return (Integer)this.progress.getOrDefault(advId, 0);
   }

   public void setProgress(String advId, int count) {
      this.progress.put(advId, count);
   }

   public boolean isUnlocked(String advId) {
      return this.unlocked.contains(advId);
   }

   public void markUnlocked(String advId) {
      this.unlocked.add(advId);
   }

   public long getPlaytimeMinutes() {
      return this.totalPlaytimeMinutes;
   }

   public void setPlaytimeMinutes(long m) {
      this.totalPlaytimeMinutes = m;
   }

   public void addPlaytimeMinutes(long m) {
      this.totalPlaytimeMinutes += m;
   }
}
