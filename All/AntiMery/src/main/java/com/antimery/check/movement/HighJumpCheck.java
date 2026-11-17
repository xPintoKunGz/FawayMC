package com.antimery.check.movement;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import com.antimery.punishment.PunishmentType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HighJumpCheck extends Check {

    private final Map<UUID, Long> lastVelocityTime = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastGivenVelocityY = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDoubleJumpTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAllowFlightTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastJumpPadTime = new ConcurrentHashMap<>();

    public HighJumpCheck(AntiMery plugin) {
        super(plugin, CheckType.HIGHJUMP, "highjump");
    }

    public void recordDoubleJump(Player player) {
        lastDoubleJumpTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void recordAllowFlight(Player player) {
        lastAllowFlightTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void recordJumpPad(Player player) {
        lastJumpPadTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void recordVelocity(Player player, Vector velocity) {
        UUID uuid = player.getUniqueId();
        lastVelocityTime.put(uuid, System.currentTimeMillis());
        if (velocity != null) {
            lastGivenVelocityY.put(uuid, velocity.getY());
        }
    }

    public void recordTeleport(Player player) {
        lastTeleportTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        lastVelocityTime.remove(uuid);
        lastGivenVelocityY.remove(uuid);
        lastTeleportTime.remove(uuid);
        lastDoubleJumpTime.remove(uuid);
        lastAllowFlightTime.remove(uuid);
        lastJumpPadTime.remove(uuid);
    }

    public boolean check(Player player, Location from, Location to) {
        if (!isEnabled()) {
            return false;
        }

        // 1. Creative or Spectator
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }

        // 2. Flight allowed by command or plugin (e.g. /fly or DoubleJump plugin enabling allowFlight)
        if (player.getAllowFlight() || player.isFlying()) {
            recordAllowFlight(player);
            return false;
        }

        // 3. Bypass permission or OP
        if (player.isOp() || player.hasPermission("antimery.bypass") || player.hasPermission("antimery.bypass.movement")) {
            return false;
        }

        // 4. Gliding, riding, riptide, swimming
        if (player.isGliding() || player.isInsideVehicle() || player.isRiptiding() || player.isSwimming()) {
            return false;
        }

        double deltaY = to.getY() - from.getY();
        if (deltaY <= 0.42) {
            return false; // Normal step or walking
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 5. Check DOUBLE JUMP plugin activation (within last 4 seconds)
        Long lastDJ = lastDoubleJumpTime.get(uuid);
        if (lastDJ != null && now - lastDJ < 4000) {
            return false; // Legitimate double jump!
        }

        // 6. Check if player recently had allowFlight enabled (within last 4 seconds)
        Long lastAF = lastAllowFlightTime.get(uuid);
        if (lastAF != null && now - lastAF < 4000) {
            return false;
        }

        // 7. Check JUMP PAD activation (pressure plates, sponge, bounce pads)
        Long lastJP = lastJumpPadTime.get(uuid);
        if (lastJP != null && now - lastJP < 4000) {
            return false;
        }

        // 8. Check teleport immunity (within last 3.5 seconds)
        Long lastTp = lastTeleportTime.get(uuid);
        if (lastTp != null && now - lastTp < 3500) {
            return false;
        }

        // 9. Check server/plugin velocity (within last 3.5 seconds)
        Long lastVel = lastVelocityTime.get(uuid);
        if (lastVel != null && now - lastVel < 3500) {
            return false;
        }

        // 10. Check bouncing blocks (Slime, Honey, Bubble Column, Piston, Trampoline)
        if (isNearBouncingBlock(from) || isNearBouncingBlock(to)) {
            return false;
        }

        // 11. Calculate base jump height + Potion Effects
        double maxJump = plugin.getConfigManager().getCheckDouble(configPath, "max-jump-height", 1.35);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.JUMP)) {
                maxJump += (effect.getAmplifier() + 1) * 0.75;
            } else if (effect.getType().equals(PotionEffectType.LEVITATION)) {
                return false;
            }
        }

        // 12. If jump height exceeds allowed without ANY server/plugin cause:
        if (deltaY > maxJump) {
            String details = String.format("กระโดดสูงผิดปกติโดยไม่มีต้นเหตุ (dY: %.3f > %.3f, ไม่มีปลั๊กอิน/ไม่มีคำสั่ง/ไม่มียา)", deltaY, maxJump);
            flag(player, details);

            if (plugin.getConfigManager().getCheckBoolean(configPath, "setback", true)) {
                player.teleport(from);
            }
            return true;
        }

        return false;
    }

    private boolean isNearBouncingBlock(Location loc) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Material mat = loc.clone().add(x, y, z).getBlock().getType();
                    if (mat == Material.SLIME_BLOCK || mat == Material.BUBBLE_COLUMN ||
                            mat == Material.PISTON || mat == Material.MOVING_PISTON ||
                            mat == Material.PISTON_HEAD || mat.name().contains("BED") ||
                            mat.name().contains("PLATE") || mat == Material.SPONGE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
