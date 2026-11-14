package org.kcm.bloomery.fawaymc.daily.data;

import java.util.UUID;

public class PlayerStreak {
   private final UUID uuid;
   private int streakDays;
   private String lastClaimDate;
   private long lastClaimTime;
   private int totalClaims;

   public PlayerStreak(UUID uuid) {
      this.uuid = uuid;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public int getStreakDays() {
      return this.streakDays;
   }

   public void setStreakDays(int v) {
      this.streakDays = v;
   }

   public String getLastClaimDate() {
      return this.lastClaimDate;
   }

   public void setLastClaimDate(String v) {
      this.lastClaimDate = v;
   }

   public long getLastClaimTime() {
      return this.lastClaimTime;
   }

   public void setLastClaimTime(long v) {
      this.lastClaimTime = v;
   }

   public int getTotalClaims() {
      return this.totalClaims;
   }

   public void setTotalClaims(int v) {
      this.totalClaims = v;
   }
}
