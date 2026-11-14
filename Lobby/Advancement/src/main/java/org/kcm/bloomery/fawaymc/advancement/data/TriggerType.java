package org.kcm.bloomery.fawaymc.advancement.data;

public enum TriggerType {
   JOIN,
   PLAY_TIME,
   BLOCK_BREAK,
   BLOCK_PLACE,
   MOB_KILL,
   PLAYER_KILL,
   LEVEL,
   COMMAND,
   API;

   public static TriggerType fromString(String s) {
      if (s == null) {
         return API;
      } else {
         try {
            return valueOf(s.toUpperCase());
         } catch (IllegalArgumentException var2) {
            return API;
         }
      }
   }

   // $FF: synthetic method
   private static TriggerType[] $values() {
      return new TriggerType[]{JOIN, PLAY_TIME, BLOCK_BREAK, BLOCK_PLACE, MOB_KILL, PLAYER_KILL, LEVEL, COMMAND, API};
   }
}
