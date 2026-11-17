package com.imagery.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Custom InventoryHolder to cleanly distinguish Imagery GUIs without relying on title strings.
 */
public class ImageryHolder implements InventoryHolder {

    public enum GuiType {
        MAIN_HUB,
        GALLERY,
        IMAGE_CONFIG,
        PLACED_MANAGER,
        RESIZE_EDITOR,
        ALBUM_MANAGER,
        ALBUM_VIEW,
        COMMAND_MANAGER
    }

    private final GuiType type;
    private final Object context;
    private int page = 0;
    private Inventory inventory;

    public ImageryHolder(GuiType type, Object context) {
        this.type = type;
        this.context = context;
    }

    public ImageryHolder(GuiType type) {
        this(type, null);
    }

    public GuiType getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext() {
        return (T) context;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

