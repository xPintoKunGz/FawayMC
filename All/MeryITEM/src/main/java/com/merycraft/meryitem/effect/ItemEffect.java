package com.merycraft.meryitem.effect;

import org.bukkit.Particle;

/**
 * All Particle styles matching MMOItems, completely usable in Minecraft 1.20 - 26.3+.
 */
public enum ItemEffect {

    SAKURA("Sakura Blossom", "&#F472B6", Particle.CHERRY_LEAVES, 3, 0.25),
    FLAME("Blazing Flame", "&#FB923C", Particle.FLAME, 3, 0.2),
    SOUL_FLAME("Soul Fire", "&#38BDF8", Particle.SOUL_FIRE_FLAME, 3, 0.2),
    STARLIGHT("Starlight Sparkle", "&#FDE047", Particle.END_ROD, 2, 0.15),
    VOID("Void Shadow", "&#C084FC", Particle.PORTAL, 5, 0.3),
    DIVINE("Divine Radiance", "&#FBBF24", Particle.TOTEM_OF_UNDYING, 4, 0.25),
    CRIT("Thunder Sparks", "&#F43F5E", Particle.CRIT, 4, 0.2),
    HEART("Charm Hearts", "&#FDA4AF", Particle.HEART, 2, 0.25),
    DRAGON_BREATH("Dragon Breath", "&#E879F9", Particle.DRAGON_BREATH, 4, 0.2),
    ENCHANT("Enchanting Runes", "&#818CF8", Particle.ENCHANT, 6, 0.4),
    SMOKE("Shadow Smoke", "&#64748B", Particle.LARGE_SMOKE, 3, 0.2),
    ELECTRIC("Electric Spark", "&#38BDF8", Particle.ELECTRIC_SPARK, 4, 0.2),
    GLOW("Golden Glow", "&#FACC15", Particle.GLOW, 3, 0.2),
    NAUTILUS("Ocean Nautilus", "&#2DD4BF", Particle.NAUTILUS, 3, 0.25),
    SCULK_SOUL("Sculk Soul", "&#06B6D4", Particle.SCULK_SOUL, 3, 0.2),
    SONIC_BOOM("Sonic Resonance", "&#0284C7", Particle.SONIC_BOOM, 1, 0.0),
    FROST("Glacial Frost", "&#A5F3FC", Particle.SNOWFLAKE, 4, 0.25),
    FIREWORK("Prismatic Sparks", "&#F43F5E", Particle.FIREWORK, 3, 0.2),
    NONE("None", "&#94A3B8", null, 0, 0);

    private final String displayName;
    private final String hexColor;
    private final Particle particle;
    private final int count;
    private final double offset;

    ItemEffect(String displayName, String hexColor, Particle particle, int count, double offset) {
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.particle = particle;
        this.count = count;
        this.offset = offset;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColor() {
        return hexColor;
    }

    public Particle getParticle() {
        return particle;
    }

    public int getCount() {
        return count;
    }

    public double getOffset() {
        return offset;
    }

    public static ItemEffect fromString(String name) {
        if (name == null) return NONE;
        String clean = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (ItemEffect effect : values()) {
            if (effect.name().equals(clean)) {
                return effect;
            }
        }
        return NONE;
    }
}
