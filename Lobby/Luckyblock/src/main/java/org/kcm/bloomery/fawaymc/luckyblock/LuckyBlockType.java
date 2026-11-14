package org.kcm.bloomery.fawaymc.luckyblock;

import org.bukkit.Material;

public class LuckyBlockType {
   public final String id;
   public final String displayName;
   public final Material material;
   public final Rarity rarity;
   public final int menuSlot;
   public final boolean glow;
   public final String lootTable;

   public LuckyBlockType(String id, String displayName, Material material, Rarity rarity, int menuSlot, boolean glow, String lootTable) {
      this.id = id;
      this.displayName = displayName;
      this.material = material;
      this.rarity = rarity;
      this.menuSlot = menuSlot;
      this.glow = glow;
      this.lootTable = lootTable;
   }
}
