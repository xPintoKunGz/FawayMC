package com.antimery.check.combat;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class CriticalsCheck extends Check {

    public CriticalsCheck(AntiMery plugin) {
        super(plugin, CheckType.CRITICALS, "criticals");
    }

    public boolean check(Player player) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        // If player is inside web or ladder
        Block blockAtFeet = player.getLocation().getBlock();
        if (blockAtFeet.getType() == Material.COBWEB || blockAtFeet.getType() == Material.LADDER || blockAtFeet.getType() == Material.VINE) {
            if (player.getFallDistance() > 0.1f) {
                flag(player, "ทำคริติคอลขณะอยู่ในใยแมงมุมหรือบันได (Web/Ladder Criticals)");
                return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
            }
        }

        // Check if player is standing on ground but claims positive fall distance
        if (player.isOnGround() && player.getFallDistance() > 0.05f) {
            flag(player, "ทำคริติคอลขณะยืนบนพื้น (Ground Spoof Criticals)");
            return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
        }

        return false;
    }
}

