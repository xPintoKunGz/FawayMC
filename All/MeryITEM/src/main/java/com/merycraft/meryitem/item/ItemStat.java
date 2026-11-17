package com.merycraft.meryitem.item;

/**
 * 100+ RPG Custom Stats for MeryITEM with custom icons, pastel hex colors, and formatting.
 */
public enum ItemStat {

    // === 1. Core Physical Offensive Stats ===
    ATTACK_DAMAGE("Attack Damage", "⚔", "&#F87171", "+%s", true),
    PHYSICAL_DAMAGE("Physical Damage", "🗡", "&#EF4444", "+%s", true),
    TRUE_DAMAGE("True Damage", "✦", "&#F43F5E", "+%s", true),
    CRIT_CHANCE("Critical Rate", "✦", "&#FB7185", "+%s%%", true),
    CRIT_DAMAGE("Crit Damage", "💥", "&#FB923C", "+%s%%", true),
    ATTACK_SPEED("Attack Speed", "⚡", "&#FDE047", "+%s", true),
    ARMOR_PENETRATION("Armor Penetration", "🪓", "&#F97316", "+%s%%", true),
    MAGIC_PENETRATION("Magic Penetration", "🔮", "&#C084FC", "+%s%%", true),
    FEROCITY("Ferocity", "🐾", "&#DC2626", "+%s", true),
    COMBO_STRIKE("Combo Multiplier", "🔥", "&#EA580C", "+%s%%", true),
    BACKSTAB_DAMAGE("Backstab Damage", "🗡", "&#B91C1C", "+%s%%", true),
    EXECUTE_DAMAGE("Execute Threshold", "☠", "&#991B1B", "+%s%%", true),
    SWEEPING_POWER("Sweeping Power", "🌀", "&#F472B6", "+%s%%", true),
    KNOCKBACK_FORCE("Knockback Force", "💨", "&#38BDF8", "+%s", true),

    // === 2. Magical & Elemental Damage ===
    MAGIC_DAMAGE("Arcane Power", "✨", "&#A855F7", "+%s", true),
    FIRE_DAMAGE("Fire Damage", "🔥", "&#F97316", "+%s", true),
    ICE_DAMAGE("Frost Damage", "❄", "&#38BDF8", "+%s", true),
    LIGHTNING_DAMAGE("Lightning Damage", "⚡", "&#FACC15", "+%s", true),
    HOLY_DAMAGE("Holy Radiance", "⭐", "&#FEF08A", "+%s", true),
    DARK_DAMAGE("Shadow Damage", "🌑", "&#6B21A8", "+%s", true),
    POISON_DAMAGE("Poison Damage", "🧪", "&#22C55E", "+%s", true),
    WITHER_DAMAGE("Wither Damage", "☠", "&#374151", "+%s", true),
    VOID_DAMAGE("Void Damage", "🌌", "&#581C87", "+%s", true),
    CHAOS_DAMAGE("Chaos Damage", "🌀", "&#D946EF", "+%s", true),
    SOUL_DAMAGE("Soul Harvest", "👻", "&#06B6D4", "+%s", true),
    EARTH_DAMAGE("Earth Shatter", "🪨", "&#78350F", "+%s", true),
    WIND_DAMAGE("Gale Damage", "🌪", "&#A7F3D0", "+%s", true),
    BLOOD_DAMAGE("Hemo Damage", "🩸", "&#991B1B", "+%s", true),

    // === 3. Status Effect Chances ===
    BLEED_CHANCE("Bleed Chance", "🩸", "&#E11D48", "+%s%%", true),
    BURN_CHANCE("Ignite Chance", "🔥", "&#EA580C", "+%s%%", true),
    FREEZE_CHANCE("Freeze Chance", "🧊", "&#0EA5E9", "+%s%%", true),
    STUN_CHANCE("Stun Chance", "💫", "&#F59E0B", "+%s%%", true),
    SHOCK_CHANCE("Shock Chance", "⚡", "&#EAB308", "+%s%%", true),
    BLIND_CHANCE("Blind Chance", "👁", "&#475569", "+%s%%", true),

    // === 4. Mob & Target Specific Damage ===
    PVP_DAMAGE("PvP Damage", "🤺", "&#EC4899", "+%s%%", true),
    PVE_DAMAGE("PvE Damage", "🏹", "&#84CC16", "+%s%%", true),
    BOSS_DAMAGE("Boss Damage", "👑", "&#F59E0B", "+%s%%", true),
    UNDEAD_DAMAGE("Smite Bonus", "💀", "&#FDE047", "+%s%%", true),
    NETHER_DAMAGE("Nether Slayer", "🌋", "&#DC2626", "+%s%%", true),
    END_DAMAGE("Ender Slayer", "🔮", "&#9333EA", "+%s%%", true),
    ELITE_DAMAGE("Elite Killer", "🎖", "&#E11D48", "+%s%%", true),

    // === 5. Core Defensive Stats ===
    DEFENSE("Defense", "🛡", "&#60A5FA", "+%s", true),
    PHYSICAL_DEFENSE("Physical Armor", "🦺", "&#3B82F6", "+%s", true),
    MAGIC_RESISTANCE("Magic Resistance", "🔮", "&#818CF8", "+%s", true),
    TRUE_DEFENSE("True Defense", "💎", "&#67E8F9", "+%s", true),
    ARMOR_TOUGHNESS("Armor Toughness", "🧱", "&#94A3B8", "+%s", true),
    MAX_HEALTH("Max Health", "❤", "&#34D399", "+%s HP", true),
    HEALTH_REGEN("Health Regen", "💖", "&#10B981", "+%s/s", true),
    DAMAGE_REDUCTION("Damage Mitigation", "🔰", "&#2563EB", "+%s%%", true),
    DODGE_CHANCE("Dodge Chance", "💨", "&#06B6D4", "+%s%%", true),
    BLOCK_CHANCE("Shield Block", "🛡", "&#3B82F6", "+%s%%", true),
    PARRY_CHANCE("Parry Chance", "⚔", "&#FBBF24", "+%s%%", true),
    THORNS_DAMAGE("Thorns Reflect", "🌵", "&#16A34A", "+%s%%", true),
    KNOCKBACK_RESISTANCE("Knockback Resist", "⚓", "&#64748B", "+%s%%", true),

    // === 6. Elemental Resistances ===
    FIRE_RESISTANCE("Fire Resistance", "🔥", "&#F97316", "+%s%%", true),
    BLAST_RESISTANCE("Blast Resistance", "💣", "&#EF4444", "+%s%%", true),
    PROJECTILE_RESISTANCE("Projectile Resist", "🏹", "&#10B981", "+%s%%", true),
    FALL_RESISTANCE("Feather Falling", "🪶", "&#A7F3D0", "+%s%%", true),
    POISON_RESISTANCE("Poison Resist", "🧪", "&#4ADE80", "+%s%%", true),
    WITHER_RESISTANCE("Wither Resist", "☠", "&#475569", "+%s%%", true),
    DEBUFF_RESISTANCE("Tenacity", "🎗", "&#A855F7", "+%s%%", true),

    // === 7. Sustain & Leech ===
    LIFE_STEAL("Life Steal", "🩸", "&#E11D48", "+%s%%", true),
    MANA_STEAL("Mana Steal", "💧", "&#0284C7", "+%s%%", true),
    ABSORPTION_HEART("Absorption Shield", "💛", "&#FDE047", "+%s HP", true),
    VAMPIRISM("Vampirism", "🦇", "&#BE123C", "+%s%%", true),
    KILL_HEAL("Kill Recovery", "🌸", "&#F472B6", "+%s HP", true),

    // === 8. Movement & Mobility ===
    MOVEMENT_SPEED("Swiftness", "💨", "&#38BDF8", "+%s%%", true),
    SPRINT_SPEED("Sprint Boost", "👟", "&#0EA5E9", "+%s%%", true),
    JUMP_HEIGHT("Jump Power", "🦘", "&#4ADE80", "+%s%%", true),
    SWIM_SPEED("Dolphin Grace", "🐬", "&#06B6D4", "+%s%%", true),
    FLYING_SPEED("Sky Glide", "🦅", "&#7DD3FC", "+%s%%", true),
    STEP_HEIGHT("Step Assist", "🪜", "&#CBD5E1", "+%s", true),
    SNEAK_SPEED("Sneak Swiftness", "🥷", "&#64748B", "+%s%%", true),
    GRAVITY_REDUCTION("Low Gravity", "🎈", "&#F472B6", "-%s%%", true),

    // === 9. Magic & Mana Pool ===
    MANA_CAPACITY("Max Mana", "💧", "&#38BDF8", "+%s MP", true),
    MANA_REGEN("Mana Regen", "🌊", "&#0284C7", "+%s MP/s", true),
    COOLDOWN_REDUCTION("Cooldown Speed", "⌛", "&#FBBF24", "+%s%%", true),
    SPELL_CRIT_CHANCE("Spell Crit Rate", "✨", "&#C084FC", "+%s%%", true),
    SPELL_CRIT_DAMAGE("Spell Crit Damage", "💥", "&#D946EF", "+%s%%", true),
    CAST_SPEED("Cast Speed", "⚡", "&#FDE047", "+%s%%", true),

    // === 10. Gathering & Profession Stats ===
    MINING_SPEED("Mining Speed", "⛏", "&#F59E0B", "+%s%%", true),
    MINING_FORTUNE("Mining Fortune", "💎", "&#06B6D4", "+%s", true),
    WOODCUTTING_SPEED("Chopping Speed", "🪓", "&#D97706", "+%s%%", true),
    FORAGING_FORTUNE("Foraging Fortune", "🌲", "&#15803D", "+%s", true),
    FARMING_FORTUNE("Farming Fortune", "🌾", "&#84CC16", "+%s", true),
    FISHING_LUCK("Sea Luck", "🎣", "&#0284C7", "+%s", true),
    DOUBLE_DROP_CHANCE("Double Drop", "📦", "&#F59E0B", "+%s%%", true),
    AUTO_SMELT("Auto Smelt Chance", "🔥", "&#EA580C", "+%s%%", true),
    EXP_BONUS("EXP Multiplier", "🔮", "&#A855F7", "+%s%%", true),

    // === 11. Attributes & RPG Attributes ===
    STRENGTH("Strength Attribute", "💪", "&#EF4444", "+%s", true),
    DEXTERITY("Dexterity Attribute", "🏹", "&#F97316", "+%s", true),
    INTELLIGENCE("Intelligence", "🧠", "&#A855F7", "+%s", true),
    AGILITY("Agility Attribute", "🏃", "&#38BDF8", "+%s", true),
    VITALITY("Vitality Attribute", "💖", "&#10B981", "+%s", true),
    WISDOM("Wisdom Attribute", "📖", "&#6366F1", "+%s", true),
    LUCK("Luck Attribute", "🍀", "&#22C55E", "+%s", true),
    CHARISMA("Charisma Attribute", "👑", "&#FBBF24", "+%s", true),
    SOUL_RESONANCE("Soul Resonance", "🌀", "&#8B5CF6", "+%s%%", true),
    DIVINE_FAVOR("Divine Favor", "🌟", "&#FDE047", "+%s%%", true);

    private final String displayName;
    private final String icon;
    private final String hexColor;
    private final String format;
    private final boolean positiveGood;

    ItemStat(String displayName, String icon, String hexColor, String format, boolean positiveGood) {
        this.displayName = displayName;
        this.icon = icon;
        this.hexColor = hexColor;
        this.format = format;
        this.positiveGood = positiveGood;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getHexColor() {
        return hexColor;
    }

    public String getFormat() {
        return format;
    }

    public boolean isPositiveGood() {
        return positiveGood;
    }

    public String formatValue(double value) {
        String valStr = (value == (long) value) ? String.format("%d", (long) value) : String.format("%.1f", value);
        return String.format(format, valStr);
    }

    public static ItemStat fromString(String name) {
        if (name == null) return null;
        String clean = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (ItemStat stat : values()) {
            if (stat.name().equals(clean)) {
                return stat;
            }
        }
        return null;
    }
}
