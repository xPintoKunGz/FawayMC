package org.kcm.bloomery.fawaymc.daily.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class DailyGuiHolder implements InventoryHolder {
   private Inventory inventory;

   public void setInventory(Inventory inv) {
      this.inventory = inv;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
