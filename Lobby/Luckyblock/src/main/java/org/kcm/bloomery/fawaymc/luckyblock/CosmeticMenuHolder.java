package org.kcm.bloomery.fawaymc.luckyblock;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CosmeticMenuHolder implements InventoryHolder {
   private Inventory inventory;

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
