package com.merycraft.meryitem.item;

import org.bukkit.Material;

/**
 * Item Category / Type definition.
 */
public enum ItemType {

    SWORD("Sword", "⚔", Material.NETHERITE_SWORD, true, false),
    DAGGER("Dagger", "🗡", Material.IRON_SWORD, true, false),
    BOW("Bow", "🏹", Material.BOW, true, false),
    CROSSBOW("Crossbow", "🎯", Material.CROSSBOW, true, false),
    STAFF("Staff", "🪄", Material.BLAZE_ROD, true, false),
    SHIELD("Shield", "🛡", Material.SHIELD, false, true),
    HELMET("Helmet", "🪖", Material.NETHERITE_HELMET, false, true),
    CHESTPLATE("Chestplate", "🦺", Material.NETHERITE_CHESTPLATE, false, true),
    LEGGINGS("Leggings", "👖", Material.NETHERITE_LEGGINGS, false, true),
    BOOTS("Boots", "🥾", Material.NETHERITE_BOOTS, false, true),
    ACCESSORY("Accessory", "💍", Material.AMETHYST_SHARD, false, true),
    CONSUMABLE("Consumable", "🧪", Material.POTION, false, false),
    CATALYST("Catalyst", "💠", Material.ECHO_SHARD, false, false),
    MATERIAL("Material", "📦", Material.DIAMOND, false, false),
    MISC("Miscellaneous", "✦", Material.NETHER_STAR, false, false);

    private final String displayName;
    private final String icon;
    private final Material defaultMaterial;
    private final boolean weapon;
    private final boolean armorOrDefense;

    ItemType(String displayName, String icon, Material defaultMaterial, boolean weapon, boolean armorOrDefense) {
        this.displayName = displayName;
        this.icon = icon;
        this.defaultMaterial = defaultMaterial;
        this.weapon = weapon;
        this.armorOrDefense = armorOrDefense;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public Material getDefaultMaterial() {
        return defaultMaterial;
    }

    public boolean isWeapon() {
        return weapon;
    }

    public boolean isArmorOrDefense() {
        return armorOrDefense;
    }

    public static ItemType fromString(String name) {
        if (name == null) return MISC;
        String clean = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (ItemType type : values()) {
            if (type.name().equals(clean)) {
                return type;
            }
        }
        return MISC;
    }
}

