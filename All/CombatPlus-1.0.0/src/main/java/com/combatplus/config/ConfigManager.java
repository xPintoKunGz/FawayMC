package com.combatplus.config;

import com.combatplus.CombatPlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final CombatPlus plugin;
    private boolean enabled;
    private double damageReductionPercent;
    private double horizontalBlockingAngle;
    private String blockSound;
    private double blockDelaySeconds;
    private boolean deflectProjectiles;
    private boolean playParticles;
    private int durabilityDamage;
    private double knockbackReductionPercent;
    private boolean requirePermission;
    private final Set<String> disabledWorlds = new HashSet<>();

    private String prefix;
    private String reloadSuccess;
    private String noPermission;
    private String toggleOn;
    private String toggleOff;
    private String swordGiven;

    public ConfigManager(CombatPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("enabled", true);
        this.damageReductionPercent = config.getDouble("damage-reduction-percent", 50.0);
        this.horizontalBlockingAngle = config.getDouble("horizontal-blocking-angle", 180.0);
        this.blockSound = config.getString("block-sound", "minecraft:item.shield.block");
        this.blockDelaySeconds = config.getDouble("block-delay-seconds", 0.0);
        this.deflectProjectiles = config.getBoolean("deflect-projectiles", true);
        this.playParticles = config.getBoolean("play-particles", true);
        this.durabilityDamage = config.getInt("durability-damage", 1);
        this.knockbackReductionPercent = config.getDouble("knockback-reduction-percent", 50.0);
        this.requirePermission = config.getBoolean("require-permission", false);

        this.disabledWorlds.clear();
        List<String> worlds = config.getStringList("disabled-worlds");
        if (worlds != null) {
            for (String w : worlds) {
                this.disabledWorlds.add(w.toLowerCase());
            }
        }

        this.prefix = colorize(config.getString("messages.prefix", "&8[&6CombatPlus&8] &r"));
        this.reloadSuccess = colorize(config.getString("messages.reload-success", "&aConfiguration reloaded successfully!"));
        this.noPermission = colorize(config.getString("messages.no-permission", "&cYou do not have permission to execute this command."));
        this.toggleOn = colorize(config.getString("messages.toggle-on", "&aSword blocking has been &eenabled &afor you."));
        this.toggleOff = colorize(config.getString("messages.toggle-off", "&cSword blocking has been &edisabled &cfor you."));
        this.swordGiven = colorize(config.getString("messages.sword-given", "&aGiven upgraded blocking sword (&e%type%&a) to &f%player%&a."));
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getDamageReductionPercent() {
        return damageReductionPercent;
    }

    public double getHorizontalBlockingAngle() {
        return horizontalBlockingAngle;
    }

    public String getBlockSound() {
        return blockSound;
    }

    public double getBlockDelaySeconds() {
        return blockDelaySeconds;
    }

    public boolean isDeflectProjectiles() {
        return deflectProjectiles;
    }

    public boolean isPlayParticles() {
        return playParticles;
    }

    public int getDurabilityDamage() {
        return durabilityDamage;
    }

    public double getKnockbackReductionPercent() {
        return knockbackReductionPercent;
    }

    public boolean isRequirePermission() {
        return requirePermission;
    }

    public boolean isWorldDisabled(String worldName) {
        if (worldName == null) return false;
        return disabledWorlds.contains(worldName.toLowerCase());
    }

    public String getPrefix() {
        return prefix;
    }

    public String getReloadSuccess() {
        return reloadSuccess;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getToggleOn() {
        return toggleOn;
    }

    public String getToggleOff() {
        return toggleOff;
    }

    public String getSwordGiven() {
        return swordGiven;
    }
}

