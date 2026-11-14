package org.kcm.bloomery.fawaymc.leaderboard.data;

import java.util.UUID;

public class LeaderboardEntry {
   private final UUID uuid;
   private final String playerName;
   private final long value;

   public LeaderboardEntry(UUID uuid, String playerName, long value) {
      this.uuid = uuid;
      this.playerName = playerName;
      this.value = value;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public long getValue() {
      return this.value;
   }
}
