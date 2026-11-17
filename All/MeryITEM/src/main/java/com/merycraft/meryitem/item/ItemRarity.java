package com.merycraft.meryitem.item;

/**
 * Modern aesthetic Rarities with refined pastel/gradient colors.
 */
public enum ItemRarity {

    COMMON("Common", "&#94A3B8", "&#CBD5E1", 1),
    UNCOMMON("Uncommon", "&#4ADE80", "&#86EFAC", 2),
    RARE("Rare", "&#38BDF8", "&#7DD3FC", 3),
    EPIC("Epic", "&#C084FC", "&#D8B4FE", 4),
    LEGENDARY("Legendary", "&#FBBF24", "&#FDE68A", 5),
    MYTHIC("Mythic", "&#FB7185", "&#FDA4AF", 6),
    DIVINE("Divine", "&#F472B6", "&#FBCFE8", 7);

    private final String displayName;
    private final String primaryColor;
    private final String lightColor;
    private final int weight;

    ItemRarity(String displayName, String primaryColor, String lightColor, int weight) {
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.lightColor = lightColor;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getLightColor() {
        return lightColor;
    }

    public int getWeight() {
        return weight;
    }

    public String getFormattedName() {
        return primaryColor + displayName.toUpperCase();
    }

    public static ItemRarity fromString(String name) {
        if (name == null) return COMMON;
        String clean = name.trim().toUpperCase();
        for (ItemRarity r : values()) {
            if (r.name().equals(clean)) {
                return r;
            }
        }
        return COMMON;
    }
}

