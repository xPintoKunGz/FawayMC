package com.antimery.check.world;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FastBreakCheck extends Check {

    private final Map<UUID, Long> lastBreakTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> breaksThisTick = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTickBreak = new ConcurrentHashMap<>();

    public FastBreakCheck(AntiMery plugin) {
        super(plugin, CheckType.FASTBREAK, "fastbreak");
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        lastBreakTime.remove(uuid);
        breaksThisTick.remove(uuid);
        lastTickBreak.remove(uuid);
    }

    public boolean check(Player player, Block block) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        // Check unbreakable block break in survival
        Material type = block.getType();
        if (type == Material.BEDROCK || type == Material.BARRIER || type == Material.END_PORTAL_FRAME) {
            flag(player, "พยายามทำลายบล็อกที่ไม่สามารถขุดได้ (" + type.name() + ")");
            return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-break", true);
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTime = lastBreakTime.getOrDefault(uuid, 0L);
        long diff = now - lastTime;

        Long lastTick = lastTickBreak.get(uuid);
        if (lastTick != null && now - lastTick < 50) {
            int count = breaksThisTick.getOrDefault(uuid, 0) + 1;
            breaksThisTick.put(uuid, count);
            if (count > 4) {
                flag(player, "ขุดบล็อกหลายบล็อกพร้อมกันใน 1 tick (Nuker)");
                return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-break", true);
            }
        } else {
            breaksThisTick.put(uuid, 1);
            lastTickBreak.put(uuid, now);
        }

        lastBreakTime.put(uuid, now);
        return false;
    }
}

