package me.editmery.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class EditMeryMenuHolder implements InventoryHolder {

    private final MenuType menuType;
    private Inventory inventory;
    private final Map<Integer, MenuItem> items = new HashMap<>();

    public EditMeryMenuHolder(MenuType menuType) {
        this.menuType = menuType;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public void setMenuItem(int slot, MenuItem item) {
        items.put(slot, item);
        if (inventory != null && item != null && item.getItemStack() != null) {
            inventory.setItem(slot, item.getItemStack());
        }
    }

    public MenuItem getMenuItem(int slot) {
        return items.get(slot);
    }
}
