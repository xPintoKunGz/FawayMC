package com.holomery.model;

/**
 * Supported line types for HoloMery holograms.
 */
public enum LineType {
    TEXT,
    ITEM,
    BLOCK,
    MOB;

    public static LineType detect(String raw) {
        if (raw == null) return TEXT;
        String upper = raw.trim().toUpperCase();
        if (upper.startsWith("ITEM:") || upper.startsWith("ICON:")) {
            return ITEM;
        } else if (upper.startsWith("BLOCK:")) {
            return BLOCK;
        } else if (upper.startsWith("MOB:") || upper.startsWith("ENTITY:")) {
            return MOB;
        }
        return TEXT;
    }

    public static String extractValue(String raw) {
        if (raw == null) return "";
        int colon = raw.indexOf(':');
        if (colon != -1) {
            return raw.substring(colon + 1).trim();
        }
        return raw.trim();
    }
}

