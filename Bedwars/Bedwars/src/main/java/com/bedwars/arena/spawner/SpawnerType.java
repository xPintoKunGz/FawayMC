package com.bedwars.arena.spawner;

import org.bukkit.Material;

public enum SpawnerType {
    IRON("Iron", Material.IRON_INGOT, "§f§lIRON", 1),
    GOLD("Gold", Material.GOLD_INGOT, "§6§lGOLD", 60),
    DIAMOND("Diamond", Material.DIAMOND, "§b§lDIAMOND", 30),
    EMERALD("Emerald", Material.EMERALD, "§a§lEMERALD", 60);

    private final String displayName;
    private final Material material;
    private final String title;
    private final int defaultInterval;

    SpawnerType(String displayName, Material material, String title, int defaultInterval) {
        this.displayName = displayName;
        this.material = material;
        this.title = title;
        this.defaultInterval = defaultInterval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getTitle() {
        return title;
    }

    public int getDefaultInterval() {
        return defaultInterval;
    }
}

