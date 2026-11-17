package com.merycraft.meryitem.item;

/**
 * Built-in MMOItems-grade RPG item abilities with active triggers and passives.
 */
public enum ItemAbility {

    CELESTIAL_SLASH("Celestial Slash", "Right-Click", "&#F472B6", "Dash forward and unleash a radiant pink slash dealing area damage.", 8),
    LIGHTNING_STRIKE("Thunder Strike", "Right-Click", "&#FDE047", "Call down divine lightning upon target crosshair, shocking foes.", 10),
    HEAL_BURST("Divine Restoration", "Right-Click", "&#34D399", "Emit an aura of healing, restoring 6 HP and granting Regeneration.", 15),
    SHADOW_STEP("Shadow Step", "Right-Click", "&#C084FC", "Blink forward into shadows, disorienting and blinding nearby enemies.", 6),
    TELEPORT("Warp Blink", "Right-Click", "&#38BDF8", "Instant warp teleport to targeted block or crosshair up to 50 blocks.", 3),
    PULL("Grapple Hook", "Right-Click", "&#FBBF24", "Hook and pull targeted entities to you, or launch yourself to target block.", 3),
    FIRE_BURST("Ignite Nova", "Right-Click", "&#FB923C", "Erupt in flames, knocking back and setting surrounding enemies ablaze.", 10),
    FROST_NOVA("Frost Nova", "Right-Click", "&#67E8F9", "Freeze nearby enemies in glacial ice, dealing damage and inflicting slowness.", 12),
    METEOR_STRIKE("Meteor Strike", "Right-Click", "&#EF4444", "Hurl an explosive blazing meteor projectile detonating on impact.", 14),
    WIND_PULSE("Sonic Shockwave", "Right-Click", "&#38BDF8", "Unleash a concussive shockwave blasting all enemies backward.", 8),
    RUN_COMMAND("Execute Command", "Right-Click", "&#C084FC", "Executes configured server commands on use (Player or Console).", 0),
    VAMPIRIC_TOUCH("Vampiric Touch", "Passive", "&#E11D48", "Drain life force from targets, healing you on every strike.", 0),
    BERSERK("Berserk Fury", "Passive", "&#F87171", "Increases damage by +30% when your health drops below 50%.", 0),
    THORNS_AURA("Aegis Reflect", "Passive", "&#60A5FA", "Reflects 20% of incoming damage back to attackers.", 0);

    private final String displayName;
    private final String trigger;
    private final String hexColor;
    private final String description;
    private final int cooldownSeconds;

    ItemAbility(String displayName, String trigger, String hexColor, String description, int cooldownSeconds) {
        this.displayName = displayName;
        this.trigger = trigger;
        this.hexColor = hexColor;
        this.description = description;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTrigger() {
        return trigger;
    }

    public String getHexColor() {
        return hexColor;
    }

    public String getDescription() {
        return description;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public static ItemAbility fromString(String name) {
        if (name == null) return null;
        String clean = name.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (ItemAbility ability : values()) {
            if (ability.name().equals(clean)) {
                return ability;
            }
        }
        return null;
    }
}
