package com.holomery.model;

import org.bukkit.entity.Display;

public enum BillboardMode {
    CENTER(Display.Billboard.CENTER),
    FIXED(Display.Billboard.FIXED),
    VERTICAL(Display.Billboard.VERTICAL),
    HORIZONTAL(Display.Billboard.HORIZONTAL);

    private final Display.Billboard bukkitBillboard;

    BillboardMode(Display.Billboard bukkitBillboard) {
        this.bukkitBillboard = bukkitBillboard;
    }

    public Display.Billboard getBukkitBillboard() {
        return bukkitBillboard;
    }

    public static BillboardMode fromString(String name) {
        if (name == null) return CENTER;
        try {
            return BillboardMode.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CENTER;
        }
    }
}

