package com.antimery.check.movement;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class NoFallCheck extends Check {

    public NoFallCheck(AntiMery plugin) {
        super(plugin, CheckType.NOFALL, "nofall");
    }

    public boolean check(Player player, Location from, Location to) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        if (player.isGliding() || player.isInsideVehicle() || player.isRiptiding()) {
            return false;
        }

        // If player packet claims they are on ground, but the block beneath is air
        if (player.isOnGround()) {
            Block blockBelow = to.clone().subtract(0, 0.5, 0).getBlock();
            Block blockAtFeet = to.getBlock();

            if (blockBelow.isEmpty() && blockAtFeet.isEmpty() && player.getFallDistance() == 0) {
                // If they were moving downward
                if (from.getY() - to.getY() > 0.5) {
                    flag(player, "หลอกสถานะบนพื้นขณะตก (Ground Packet Spoof)");
                    return true;
                }
            }
        }

        return false;
    }
}

