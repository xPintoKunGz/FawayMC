package org.kcm.bloomery.fawaymc.friend.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class FriendGuiHolder implements InventoryHolder {
   private final Type type;
   private Inventory inventory;

   public FriendGuiHolder(Type type) {
      this.type = type;
   }

   public Type getType() {
      return this.type;
   }

   public void setInventory(Inventory inv) {
      this.inventory = inv;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public static enum Type {
      MAIN,
      LIST,
      REQUESTS,
      SETTINGS;

      // $FF: synthetic method
      private static Type[] $values() {
         return new Type[]{MAIN, LIST, REQUESTS, SETTINGS};
      }
   }
}
