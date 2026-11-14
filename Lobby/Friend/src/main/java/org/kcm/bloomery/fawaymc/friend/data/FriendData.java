package org.kcm.bloomery.fawaymc.friend.data;

import java.util.UUID;

public class FriendData {
   private final UUID playerUuid;
   private final UUID friendUuid;
   private final long createdAt;
   private boolean favorite;
   private String nickname;

   public FriendData(UUID playerUuid, UUID friendUuid, long createdAt, boolean favorite, String nickname) {
      this.playerUuid = playerUuid;
      this.friendUuid = friendUuid;
      this.createdAt = createdAt;
      this.favorite = favorite;
      this.nickname = nickname;
   }

   public FriendData(UUID playerUuid, UUID friendUuid, long createdAt, boolean favorite) {
      this(playerUuid, friendUuid, createdAt, favorite, (String)null);
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public UUID getFriendUuid() {
      return this.friendUuid;
   }

   public long getCreatedAt() {
      return this.createdAt;
   }

   public boolean isFavorite() {
      return this.favorite;
   }

   public void setFavorite(boolean favorite) {
      this.favorite = favorite;
   }

   public String getNickname() {
      return this.nickname;
   }

   public void setNickname(String nickname) {
      this.nickname = nickname;
   }
}
