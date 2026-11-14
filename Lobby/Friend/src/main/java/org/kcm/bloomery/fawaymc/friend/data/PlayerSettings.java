package org.kcm.bloomery.fawaymc.friend.data;

import java.util.UUID;

public class PlayerSettings {
   private final UUID playerUuid;
   private boolean acceptRequests;
   private boolean notifyOnline;

   public PlayerSettings(UUID playerUuid, boolean acceptRequests, boolean notifyOnline) {
      this.playerUuid = playerUuid;
      this.acceptRequests = acceptRequests;
      this.notifyOnline = notifyOnline;
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public boolean isAcceptRequests() {
      return this.acceptRequests;
   }

   public void setAcceptRequests(boolean v) {
      this.acceptRequests = v;
   }

   public boolean isNotifyOnline() {
      return this.notifyOnline;
   }

   public void setNotifyOnline(boolean v) {
      this.notifyOnline = v;
   }
}
