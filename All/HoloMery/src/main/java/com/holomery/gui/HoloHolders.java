package com.holomery.gui;

import com.holomery.model.Hologram;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HoloHolders {

    public static class ListHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        public ListHolder(int page) {
            this.page = page;
        }

        public int getPage() {
            return page;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static class EditHolder implements InventoryHolder {
        private final Hologram hologram;
        private Inventory inventory;

        public EditHolder(Hologram hologram) {
            this.hologram = hologram;
        }

        public Hologram getHologram() {
            return hologram;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static class ThemeHolder implements InventoryHolder {
        private final Hologram hologram;
        private Inventory inventory;

        public ThemeHolder(Hologram hologram) {
            this.hologram = hologram;
        }

        public Hologram getHologram() {
            return hologram;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static class AddLineHolder implements InventoryHolder {
        private final Hologram hologram;
        private Inventory inventory;

        public AddLineHolder(Hologram hologram) {
            this.hologram = hologram;
        }

        public Hologram getHologram() {
            return hologram;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static class ActionHolder implements InventoryHolder {
        private final Hologram hologram;
        private Inventory inventory;

        public ActionHolder(Hologram hologram) {
            this.hologram = hologram;
        }

        public Hologram getHologram() {
            return hologram;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

