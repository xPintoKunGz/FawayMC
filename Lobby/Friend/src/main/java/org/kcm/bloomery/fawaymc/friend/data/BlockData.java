package org.kcm.bloomery.fawaymc.friend.data;

import java.util.UUID;

public class BlockData {
   private final UUID playerUuid;
   private final UUID blockedUuid;
   private final long createdAt;

   public BlockData(UUID playerUuid, UUID blockedUuid, long createdAt) {
      this.playerUuid = playerUuid;
      this.blockedUuid = blockedUuid;
      this.createdAt = createdAt;
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public UUID getBlockedUuid() {
      return this.blockedUuid;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }
}
