package com.antimery.check.movement;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedCheck extends Check {

    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastVelocityTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();

    public SpeedCheck(AntiMery plugin) {
        super(plugin, CheckType.SPEED, "speed");
    }

    public void recordVelocity(Player player, Vector velocity) {
        lastVelocityTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void recordTeleport(Player player) {
        lastTeleportTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        lastLocations.remove(uuid);
        lastVelocityTime.remove(uuid);
        lastTeleportTime.remove(uuid);
    }

    public boolean check(Player player, Location from, Location to) {
        if (!isEnabled()) {
            return false;
        }

        // Creative or Spectator
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        // Bypass permissions
        if (player.isOp() || player.hasPermission("antimery.bypass") || player.hasPermission("antimery.bypass.movement")) {
            return false;
        }

        if (player.isGliding() || player.isInsideVehicle() || player.isRiptiding()) {
            return false;
        }

        // Ignore if player is flying by permission/command
        if (player.getAllowFlight()) {
            return false;
        }

        // Command or plugin teleport immunity (within last 3 seconds)
        Long lastTp = lastTeleportTime.get(player.getUniqueId());
        if (lastTp != null && System.currentTimeMillis() - lastTp < 3000) {
            return false;
        }

        // Velocity from other plugins / Jump Pads / Explosion immunity (within last 2.5 seconds)
        Long lastVel = lastVelocityTime.get(player.getUniqueId());
        if (lastVel != null && System.currentTimeMillis() - lastVel < 2500) {
            return false;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Base max speed
        double maxSpeed = plugin.getConfigManager().getCheckDouble(configPath, "max-horizontal-speed", 0.65);

        // Account for custom speed attributes set by other plugins or armor
        try {
            AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                double baseVal = speedAttr.getBaseValue();
                double currentVal = speedAttr.getValue();
                if (currentVal > baseVal) {
                    maxSpeed += (currentVal - baseVal) * 3.5;
                }
            }
        } catch (Throwable ignored) {
        }

        // Account for speed potion effect
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                maxSpeed += (effect.getAmplifier() + 1) * 0.15;
            } else if (effect.getType().equals(PotionEffectType.DOLPHINS_GRACE)) {
                maxSpeed += 0.8;
            }
        }

        // Account for ice underneath
        Block blockBelow = to.clone().subtract(0, 1, 0).getBlock();
        Material mat = blockBelow.getType();
        if (mat == Material.ICE || mat == Material.PACKED_ICE || mat == Material.BLUE_ICE) {
            maxSpeed += 0.35;
        }

        if (horizontalDistance > maxSpeed) {
            String details = String.format("ความเร็วเกินกำหนดโดยไม่มีต้นเหตุ (%.3f > %.3f blocks/tick)", horizontalDistance, maxSpeed);
            flag(player, details);

            if (plugin.getConfigManager().getCheckBoolean(configPath, "setback", true)) {
                player.teleport(from);
            }
            return true;
        }

        lastLocations.put(player.getUniqueId(), to);
        return false;
    }
}
