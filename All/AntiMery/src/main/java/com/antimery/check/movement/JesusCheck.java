package com.antimery.check.movement;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class JesusCheck extends Check {

    public JesusCheck(AntiMery plugin) {
        super(plugin, CheckType.JESUS, "jesus");
    }

    public boolean check(Player player, Location from, Location to) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        if (player.isGliding() || player.isInsideVehicle() || player.isSwimming() || player.isRiptiding()) {
            return false;
        }

        // Check if player has frost walker boots
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.containsEnchantment(Enchantment.FROST_WALKER)) {
            return false;
        }

        Block blockFeet = to.getBlock();
        Block blockBelow = to.clone().subtract(0, 0.3, 0).getBlock();

        boolean inLiquid = (blockFeet.getType() == Material.WATER || blockFeet.getType() == Material.LAVA);
        boolean onLiquid = (blockBelow.getType() == Material.WATER || blockBelow.getType() == Material.LAVA);

        if (onLiquid && !inLiquid) {
            // Player is standing directly on top of liquid without sinking
            if (Math.abs(to.getY() - from.getY()) < 0.01) {
                // Check if any nearby solid block exists
                boolean hasSolid = false;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (to.clone().add(x, -0.3, z).getBlock().getType().isSolid()) {
                            hasSolid = true;
                            break;
                        }
                    }
                }

                if (!hasSolid) {
                    flag(player, "เดินบนผิวน้ำ/ลาวา (Jesus / WaterWalk)");
                    if (plugin.getConfigManager().getCheckBoolean(configPath, "setback", true)) {
                        player.teleport(from);
                    }
                    return true;
                }
            }
        }

        return false;
    }
}

