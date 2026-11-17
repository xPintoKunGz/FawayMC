package com.antigravity.reversecrouch.listener;

import com.antigravity.reversecrouch.ReverseCrouch;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BridgingListener implements Listener {

    private final ReverseCrouch plugin;
    private final Set<Material> allowedMaterials = new HashSet<>();

    public BridgingListener(ReverseCrouch plugin) {
        this.plugin = plugin;
        loadAllowedBlocks();
    }

    public void loadAllowedBlocks() {
        allowedMaterials.clear();
        List<String> list = plugin.getConfig().getStringList("bridging.allowed-blocks");
        if (list != null && !list.isEmpty()) {
            for (String name : list) {
                Material mat = Material.matchMaterial(name);
                if (mat != null) {
                    allowedMaterials.add(mat);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("enabled", true)) return;
        if (!plugin.getConfig().getBoolean("bridging.prevent-edge-fall", true)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("reversecrouch.bypass")) return;

        // Only active when player is on ground, not flying, not in vehicle
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle() || player.isDead()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        // If no horizontal movement, skip
        if (dx == 0 && dz == 0) return;

        // Check if player is holding a placeable block
        if (plugin.getConfig().getBoolean("bridging.require-holding-placeable-block", true)) {
            if (!isHoldingPlaceableBlock(player)) {
                return;
            }
        }

        // Check pitch angle (bridging requires looking downward towards the edge)
        if (plugin.getConfig().getBoolean("bridging.require-looking-down", true)) {
            double minPitch = plugin.getConfig().getDouble("bridging.min-pitch-angle", 30.0);
            if (from.getPitch() < minPitch) {
                return;
            }
        }

        // Check if movement is backward or sideways-backward (moving opposite to look direction)
        Vector lookDir = from.getDirection().setY(0);
        if (lookDir.lengthSquared() > 0.0001) {
            lookDir.normalize();
            Vector moveDir = new Vector(dx, 0, dz).normalize();
            double dot = lookDir.dot(moveDir);
            // dot < 0 means moving backwards relative to look direction (bridging motion)
            if (dot > 0.2) {
                // Moving forward: do not clamp, allow normal running/falling
                return;
            }
        }

        // Verify the block below "from" was solid
        Block groundBlock = getBlockDirectlyUnder(from);
        if (groundBlock == null || !groundBlock.getType().isSolid()) {
            // Player is already airborne or falling, do not clamp in mid-air
            return;
        }

        // Check if next X position would be over air/non-solid
        Location testX = from.clone();
        testX.setX(to.getX());
        boolean xSafe = isPositionSupported(testX);

        // Check if next Z position would be over air/non-solid
        Location testZ = from.clone();
        testZ.setZ(to.getZ());
        boolean zSafe = isPositionSupported(testZ);

        // Check combined destination
        boolean bothSafe = isPositionSupported(to);

        if (!bothSafe) {
            double newX = xSafe ? to.getX() : from.getX();
            double newZ = zSafe ? to.getZ() : from.getZ();

            // If neither is individually safe, clamp both
            if (!xSafe && !zSafe) {
                newX = from.getX();
                newZ = from.getZ();
            }

            Location clamped = new Location(
                    to.getWorld(),
                    newX,
                    to.getY(),
                    newZ,
                    to.getYaw(),
                    to.getPitch()
            );

            event.setTo(clamped);

            // Optional subtle audio feedback
            if (plugin.getConfig().getBoolean("feedback.play-edge-sound", false)) {
                String soundName = plugin.getConfig().getString("feedback.sound-name", "BLOCK_NOTE_BLOCK_HAT");
                try {
                    @SuppressWarnings("removal")
                    Sound sound = Sound.valueOf(soundName);
                    float volume = (float) plugin.getConfig().getDouble("feedback.volume", 0.3);
                    float pitch = (float) plugin.getConfig().getDouble("feedback.pitch", 1.8);
                    player.playSound(clamped, sound, volume, pitch);
                } catch (IllegalArgumentException | NoSuchFieldError ignored) {}
            }
        }
    }

    private boolean isHoldingPlaceableBlock(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        return isPlaceableBlock(main) || isPlaceableBlock(off);
    }

    private boolean isPlaceableBlock(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        Material mat = item.getType();
        if (!mat.isBlock() || !mat.isSolid()) return false;

        if (!allowedMaterials.isEmpty()) {
            return allowedMaterials.contains(mat);
        }
        return true;
    }

    private Block getBlockDirectlyUnder(Location loc) {
        // Player feet are at loc.getY(), ground block is at loc.getY() - 0.5
        Location under = loc.clone().subtract(0, 0.5, 0);
        return under.getBlock();
    }

    private boolean isPositionSupported(Location loc) {
        // Check if there is any solid block directly supporting the player's bounding box
        // Player hitbox radius is approximately 0.3 blocks
        double r = 0.28;
        double[][] offsets = {
                {0, 0},
                {r, r},
                {r, -r},
                {-r, r},
                {-r, -r}
        };

        for (double[] offset : offsets) {
            Location check = loc.clone().add(offset[0], -0.5, offset[1]);
            Block b = check.getBlock();
            if (b.getType().isSolid()) {
                return true;
            }
        }
        return false;
    }
}
