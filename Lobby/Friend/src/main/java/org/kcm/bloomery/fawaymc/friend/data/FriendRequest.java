package org.kcm.bloomery.fawaymc.friend.data;

import java.util.UUID;

public class FriendRequest {
   private final UUID senderUuid;
   private final UUID targetUuid;
   private final long createdAt;

   public FriendRequest(UUID senderUuid, UUID targetUuid, long createdAt) {
      this.senderUuid = senderUuid;
      this.targetUuid = targetUuid;
      this.createdAt = createdAt;
   }

   public UUID getSenderUuid() {
      return this.senderUuid;
   }

   public UUID getTargetUuid() {
      return this.targetUuid;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }
}
