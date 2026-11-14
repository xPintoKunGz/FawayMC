package org.kcm.bloomery.fawaymc.friend;

public class FriendResult {
   private final Status status;
   private final String message;
   private final String targetName;

   private FriendResult(Status status, String message, String targetName) {
      this.status = status;
      this.message = message;
      this.targetName = targetName;
   }

   public static FriendResult success(String message, String targetName) {
      return new FriendResult(FriendResult.Status.SUCCESS, message, targetName);
   }

   public static FriendResult failure(Status status, String message, String targetName) {
      return new FriendResult(status, message, targetName);
   }

   public Status getStatus() {
      return this.status;
   }

   public String getMessage() {
      return this.message;
   }

   public String getTargetName() {
      return this.targetName;
   }

   public boolean isSuccess() {
      return this.status == FriendResult.Status.SUCCESS;
   }

   public static enum Status {
      SUCCESS,
      PLAYER_NOT_FOUND,
      CANNOT_ADD_SELF,
      ALREADY_FRIENDS,
      ALREADY_REQUESTED,
      REQUESTS_DISABLED,
      TARGET_REQUESTS_DISABLED,
      NO_PENDING_REQUEST,
      MAX_FRIENDS_REACHED,
      MAX_PENDING_REACHED,
      NOT_FRIENDS,
      ERROR;

      // $FF: synthetic method
      private static Status[] $values() {
         return new Status[]{SUCCESS, PLAYER_NOT_FOUND, CANNOT_ADD_SELF, ALREADY_FRIENDS, ALREADY_REQUESTED, REQUESTS_DISABLED, TARGET_REQUESTS_DISABLED, NO_PENDING_REQUEST, MAX_FRIENDS_REACHED, MAX_PENDING_REACHED, NOT_FRIENDS, ERROR};
      }
   }
}
