package org.kcm.bloomery.fawaymc.luckyblock;

public enum CosmeticType {
   TRAIL,
   HAT,
   GADGET;

   public static CosmeticType fromString(String s) {
      if (s == null) {
         return TRAIL;
      } else {
         try {
            return valueOf(s.toUpperCase());
         } catch (IllegalArgumentException var2) {
            return TRAIL;
         }
      }
   }

   // $FF: synthetic method
   private static CosmeticType[] $values() {
      return new CosmeticType[]{TRAIL, HAT, GADGET};
   }
}
