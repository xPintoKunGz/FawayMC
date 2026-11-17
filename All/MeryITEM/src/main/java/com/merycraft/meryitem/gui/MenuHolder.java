package com.merycraft.meryitem.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface MenuHolder extends InventoryHolder {
    void handleClick(org.bukkit.event.inventory.InventoryClickEvent event);
}

