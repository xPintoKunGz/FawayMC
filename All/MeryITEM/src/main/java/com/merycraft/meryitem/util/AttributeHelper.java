package com.merycraft.meryitem.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * Version-independent Attribute helper for 1.20, 1.21, and 26.x.
 */
public final class AttributeHelper {

    private static Attribute MAX_HEALTH;
    private static Attribute MOVEMENT_SPEED;

    static {
        // Resolve MAX_HEALTH
        try {
            MAX_HEALTH = Attribute.valueOf("MAX_HEALTH");
        } catch (Throwable t) {
            try {
                MAX_HEALTH = Attribute.valueOf("GENERIC_MAX_HEALTH");
            } catch (Throwable ignored) {}
        }

        // Resolve MOVEMENT_SPEED
        try {
            MOVEMENT_SPEED = Attribute.valueOf("MOVEMENT_SPEED");
        } catch (Throwable t) {
            try {
                MOVEMENT_SPEED = Attribute.valueOf("GENERIC_MOVEMENT_SPEED");
            } catch (Throwable ignored) {}
        }
    }

    private AttributeHelper() {}

    public static void setBonusHealth(Player player, double bonusHealth) {
        if (MAX_HEALTH == null) return;
        AttributeInstance instance = player.getAttribute(MAX_HEALTH);
        if (instance != null) {
            double base = 20.0;
            instance.setBaseValue(base + Math.max(0, bonusHealth));
        }
    }

    public static void setBonusSpeed(Player player, double speedPercent) {
        if (MOVEMENT_SPEED == null) return;
        AttributeInstance instance = player.getAttribute(MOVEMENT_SPEED);
        if (instance != null) {
            double base = 0.1; // Default Minecraft walking speed
            double multiplier = 1.0 + (speedPercent / 100.0);
            instance.setBaseValue(base * multiplier);
        }
    }

    public static void resetAttributes(Player player) {
        if (MAX_HEALTH != null) {
            AttributeInstance hp = player.getAttribute(MAX_HEALTH);
            if (hp != null) hp.setBaseValue(20.0);
        }
        if (MOVEMENT_SPEED != null) {
            AttributeInstance sp = player.getAttribute(MOVEMENT_SPEED);
            if (sp != null) sp.setBaseValue(0.1);
        }
    }
}

