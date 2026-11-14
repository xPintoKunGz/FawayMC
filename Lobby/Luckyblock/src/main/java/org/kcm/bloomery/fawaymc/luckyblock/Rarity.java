package org.kcm.bloomery.fawaymc.luckyblock;

public enum Rarity {
   COMMON("&7", "Common", 60),
   RARE("&9", "Rare", 25),
   EPIC("&5", "Epic", 10),
   LEGENDARY("&6", "Legendary", 4),
   MYTHIC("&c", "Mythic", 1);

   public final String color;
   public final String displayName;
   public final int defaultWeight;

   private Rarity(String color, String displayName, int defaultWeight) {
      this.color = color;
      this.displayName = displayName;
      this.defaultWeight = defaultWeight;
   }

   public static Rarity fromString(String s) {
      if (s == null) {
         return COMMON;
      } else {
         try {
            return valueOf(s.toUpperCase());
         } catch (IllegalArgumentException var2) {
            return COMMON;
         }
      }
   }

   // $FF: synthetic method
   private static Rarity[] $values() {
      return new Rarity[]{COMMON, RARE, EPIC, LEGENDARY, MYTHIC};
   }
}
