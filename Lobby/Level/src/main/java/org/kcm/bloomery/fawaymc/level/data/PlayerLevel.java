package org.kcm.bloomery.fawaymc.level.data;

import java.util.UUID;

public class PlayerLevel {
   private final UUID uuid;
   private int level;
   private long exp;
   private long totalExp;
   private long lastSeen;

   public PlayerLevel(UUID uuid) {
      this.uuid = uuid;
      this.level = 1;
      this.exp = 0L;
      this.totalExp = 0L;
      this.lastSeen = System.currentTimeMillis();
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public int getLevel() {
      return this.level;
   }

   public void setLevel(int level) {
      this.level = level;
   }

   public long getExp() {
      return this.exp;
   }

   public void setExp(long exp) {
      this.exp = exp;
   }

   public long getTotalExp() {
      return this.totalExp;
   }

   public void setTotalExp(long v) {
      this.totalExp = v;
   }

   public void addTotalExp(long v) {
      this.totalExp += v;
   }

   public long getLastSeen() {
      return this.lastSeen;
   }

   public void setLastSeen(long v) {
      this.lastSeen = v;
   }
}
