package com.merycraft.meryitem.effect;

/**
 * MMOItems Particle Motion Types (Helix, Vortex, Double Rings, Galaxy, Fireflies, Aura, Offset).
 */
public enum ParticleMotion {
    AURA("Aura", "Static ambient particle cloud around player & weapon"),
    HELIX("Helix", "Spiraling twin helix rising upwards around the player"),
    VORTEX("Vortex", "Swirling spinning vortex funnel around weapon"),
    DOUBLE_RINGS("Double Rings", "Two intersecting planetary gyroscopic rings"),
    GALAXY("Galaxy", "Rotating multi-armed spiral galaxy ring"),
    FIREFLIES("Fireflies", "Erratic flickering firefly orbs floating around"),
    OFFSET("Offset Weapon Tip", "Directly emits from weapon blade/tip");

    private final String displayName;
    private final String description;

    ParticleMotion(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static ParticleMotion fromString(String name) {
        if (name == null) return AURA;
        String clean = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (ParticleMotion pm : values()) {
            if (pm.name().equals(clean)) return pm;
        }
        return AURA;
    }
}

