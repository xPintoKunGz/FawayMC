package com.antimery.check.movement;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import com.antimery.punishment.PunishmentType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlyCheck extends Check {

    private final Map<UUID, Integer> airTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastGroundLocation = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastVelocityTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDoubleJumpTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAllowFlightTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastJumpPadTime = new ConcurrentHashMap<>();

    public FlyCheck(AntiMery plugin) {
        super(plugin, CheckType.FLY, "fly");
    }

    public void recordDoubleJump(Player player) {
        lastDoubleJumpTime.put(player.getUniqueId(), System.currentTimeMillis());
        airTicks.put(player.getUniqueId(), 0);
    }

    public void recordAllowFlight(Player player) {
        lastAllowFlightTime.put(player.getUniqueId(), System.currentTimeMillis());
        airTicks.put(player.getUniqueId(), 0);
    }

    public void recordJumpPad(Player player) {
        lastJumpPadTime.put(player.getUniqueId(), System.currentTimeMillis());
        airTicks.put(player.getUniqueId(), 0);
    }

    public void recordVelocity(Player player, Vector velocity) {
        lastVelocityTime.put(player.getUniqueId(), System.currentTimeMillis());
        airTicks.put(player.getUniqueId(), 0);
    }

    public void recordTeleport(Player player) {
        lastTeleportTime.put(player.getUniqueId(), System.currentTimeMillis());
        airTicks.put(player.getUniqueId(), 0);
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        airTicks.remove(uuid);
        lastGroundLocation.remove(uuid);
        lastVelocityTime.remove(uuid);
        lastTeleportTime.remove(uuid);
        lastDoubleJumpTime.remove(uuid);
        lastAllowFlightTime.remove(uuid);
        lastJumpPadTime.remove(uuid);
    }

    public boolean check(Player player, Location from, Location to) {
        if (!isEnabled()) {
            return false;
        }

        // 1. Creative or Spectator mode
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            airTicks.remove(player.getUniqueId());
            return false;
        }

        // 2. Flight allowed by server / command / plugin (e.g. /fly, DoubleJump plugin enabling allowFlight)
        boolean allowCommandFlight = plugin.getConfigManager().getCheckBoolean(configPath, "allow-command-flight", true);
        if (allowCommandFlight && player.getAllowFlight()) {
            recordAllowFlight(player);
            airTicks.remove(player.getUniqueId());
            return false;
        }

        // 3. Permissions / OP check
        if (player.isOp() || player.hasPermission("antimery.bypass") || player.hasPermission("antimery.bypass.fly")
                || player.hasPermission("essentials.fly") || player.hasPermission("cmi.command.fly")) {
            airTicks.remove(player.getUniqueId());
            return false;
        }

        // 4. Gliding with Elytra
        if (player.isGliding()) {
            airTicks.remove(player.getUniqueId());
            return false;
        }

        // 5. Inside vehicle
        if (player.isInsideVehicle()) {
            airTicks.remove(player.getUniqueId());
            return false;
        }

        // 6. Swimming in water, lava, or bubble column, or riptiding
        if (player.isSwimming() || player.isRiptiding() || isNearLiquid(to)) {
            airTicks.remove(player.getUniqueId());
            return false;
        }

        // 7. Potion Effects (Levitation, Slow Falling)
        if (player.hasPotionEffect(PotionEffectType.LEVITATION) || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            airTicks.remove(player.getUniqueId());
            return false;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 8. DOUBLE JUMP plugin activation (within last 4 seconds)
        Long lastDJ = lastDoubleJumpTime.get(uuid);
        if (lastDJ != null && now - lastDJ < 4000) {
            airTicks.remove(uuid);
            return false;
        }

        // 9. Recently had allowFlight enabled by double jump plugin (within last 4 seconds)
        Long lastAF = lastAllowFlightTime.get(uuid);
        if (lastAF != null && now - lastAF < 4000) {
            airTicks.remove(uuid);
            return false;
        }

        // 10. JUMP PAD activation (pressure plates, sponge, bounce pads)
        Long lastJP = lastJumpPadTime.get(uuid);
        if (lastJP != null && now - lastJP < 4000) {
            airTicks.remove(uuid);
            return false;
        }

        // 11. Command or plugin teleport immunity (within last 3.5 seconds)
        Long lastTp = lastTeleportTime.get(uuid);
        if (lastTp != null && now - lastTp < 3500) {
            airTicks.remove(uuid);
            return false;
        }

        // 12. Velocity from other plugins / Jump Pads / Knockback (within last 3.5 seconds)
        Long lastVel = lastVelocityTime.get(uuid);
        if (lastVel != null && now - lastVel < 3500) {
            airTicks.remove(uuid);
            return false;
        }

        // 13. Near Slime block, Honey block, Bubble column, Piston, Trampoline, Pressure plate
        if (isNearBouncingBlock(from) || isNearBouncingBlock(to)) {
            airTicks.remove(uuid);
            return false;
        }

        // 14. Near ladder, vines, scaffolding, cobweb
        if (isNearClimbable(to)) {
            airTicks.remove(uuid);
            return false;
        }

        // 15. Ground detection
        boolean onGround = isGroundNearby(to);
        if (onGround) {
            airTicks.put(uuid, 0);
            lastGroundLocation.put(uuid, to.clone());
            return false;
        }

        // Player is completely in midair with NO ground and NO legitimate server/plugin reason!
        int ticks = airTicks.getOrDefault(uuid, 0) + 1;
        airTicks.put(uuid, ticks);

        double deltaY = to.getY() - from.getY();
        int maxAirTicks = plugin.getConfigManager().getCheckInt(configPath, "max-air-ticks", 15);

        // If player is hovering (deltaY >= -0.05) or ascending (deltaY > 0) in midair with no reason:
        if (ticks > maxAirTicks && deltaY >= -0.05) {
            String details = String.format("ลอยตัว/บินในอากาศโดยไม่มีเหตุผล (Ticks: %d, dY: %.3f)", ticks, deltaY);

            PunishmentType punishment = getPunishmentType();
            plugin.getPunishmentManager().alertStaff(player, type, ticks, maxAirTicks, details);
            plugin.getPunishmentManager().executePunishment(player, type, punishment, "บิน/ลอยตัวโดยไม่มีเหตุผล (ไม่มีคำสั่ง/ไม่มียา/ไม่มีต้นเหตุ)");
            return true;
        }

        return false;
    }

    private void teleportBack(Player player) {
        Location ground = lastGroundLocation.get(player.getUniqueId());
        if (ground != null && ground.getWorld().equals(player.getWorld())) {
            player.teleport(ground);
        }
    }

    public boolean isGroundNearby(Location loc) {
        Location checkLoc = loc.clone().subtract(0, 0.2, 0);
        double[] offsets = {-0.3, 0.0, 0.3};
        for (double ox : offsets) {
            for (double oz : offsets) {
                Block b = checkLoc.clone().add(ox, 0, oz).getBlock();
                if (b.getType().isSolid()) {
                    return true;
                }
                if (isClimbable(b.getType())) {
                    return true;
                }
            }
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

    private boolean isNearClimbable(Location loc) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Material mat = loc.clone().add(x, y, z).getBlock().getType();
                    if (isClimbable(mat)) return true;
                }
            }
        }
        return false;
    }

    private boolean isNearLiquid(Location loc) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Material mat = loc.clone().add(x, y, z).getBlock().getType();
                    if (mat == Material.WATER || mat == Material.LAVA || mat == Material.BUBBLE_COLUMN) return true;
                }
            }
        }
        return false;
    }

    private boolean isClimbable(Material mat) {
        return mat == Material.LADDER || mat == Material.VINE ||
                mat == Material.SCAFFOLDING || mat == Material.WEEPING_VINES ||
                mat == Material.TWISTING_VINES || mat == Material.WEEPING_VINES_PLANT ||
                mat == Material.TWISTING_VINES_PLANT || mat == Material.COBWEB;
    }
}
