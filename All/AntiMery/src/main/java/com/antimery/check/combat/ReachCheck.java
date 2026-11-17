package com.antimery.check.combat;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/**
 * Watchdog Reach Engine
 * คำนวณระยะการโจมตีจริงแบบ Raytrace สู่ Hitbox (BoundingBox) ของเป้าหมาย
 * พร้อมระบบชดเชยค่าความหน่วงของเครือข่าย (Ping/Latency Compensation)
 */
public class ReachCheck extends Check {

    public ReachCheck(AntiMery plugin) {
        super(plugin, CheckType.REACH, "reach");
    }

    public boolean check(Player player, Entity target) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        Location eyeLoc = player.getEyeLocation();
        BoundingBox box = target.getBoundingBox();

        // คำนวณจุดที่ใกล้สายตาผู้เล่นที่สุดบน Hitbox (Closest point on AABB)
        double closestX = Math.max(box.getMinX(), Math.min(eyeLoc.getX(), box.getMaxX()));
        double closestY = Math.max(box.getMinY(), Math.min(eyeLoc.getY(), box.getMaxY()));
        double closestZ = Math.max(box.getMinZ(), Math.min(eyeLoc.getZ(), box.getMaxZ()));

        double distance = eyeLoc.distance(new Location(eyeLoc.getWorld(), closestX, closestY, closestZ));

        // บันทึกลง Atlas
        if (plugin.getAtlasManager() != null) {
            plugin.getAtlasManager().getProfile(player).setLastReachDistance(distance);
        }

        double baseMaxReach = plugin.getConfigManager().getCheckDouble(configPath, "max-distance", 3.1);

        // ระบบชดเชยค่าความหน่วง (Ping Compensation) สูงสุดไม่เกิน 0.45 block
        int ping = player.getPing();
        double pingAllowance = Math.min(0.45, (ping / 1000.0) * 1.5);

        // โบนัสการวิ่ง (Sprinting Momentum) เล็กน้อย
        double sprintAllowance = player.isSprinting() ? 0.15 : 0.0;

        double maxAllowed = baseMaxReach + pingAllowance + sprintAllowance;

        if (distance > maxAllowed) {
            String details = String.format("Watchdog Reach: โจมตีไกลเกินระยะ Hitbox (Distance: %.2f > %.2f blocks, Ping: %dms)",
                    distance, maxAllowed, ping);
            flag(player, details);
            return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
        }

        return false;
    }
}
