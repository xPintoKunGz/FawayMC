package com.antimery.check.movement;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class FastClimbCheck extends Check {

    public FastClimbCheck(AntiMery plugin) {
        super(plugin, CheckType.FASTCLIMB, "fastclimb");
    }

    public boolean check(Player player, Location from, Location to) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        Block block = to.getBlock();
        Material mat = block.getType();

        if (mat == Material.LADDER || mat == Material.VINE ||
            mat == Material.SCAFFOLDING || mat == Material.WEEPING_VINES ||
            mat == Material.TWISTING_VINES) {

            double deltaY = to.getY() - from.getY();
            // Normal ladder climbing speed is ~0.11 - 0.15 blocks/tick
            if (deltaY > 0.35) {
                String details = String.format("ปีนบันได/เถาวัลย์ไวเกินกำหนด (dY: %.3f)", deltaY);
                flag(player, details);

                if (plugin.getConfigManager().getCheckBoolean(configPath, "setback", true)) {
                    player.teleport(from);
                }
                return true;
            }
        }

        return false;
    }
}

