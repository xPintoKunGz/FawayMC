package com.antimery.check.world;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FastUseCheck extends Check {

    private final Map<UUID, Long> startConsumeTime = new ConcurrentHashMap<>();

    public FastUseCheck(AntiMery plugin) {
        super(plugin, CheckType.FASTUSE, "fastuse");
    }

    public void handleQuit(Player player) {
        startConsumeTime.remove(player.getUniqueId());
    }

    public void recordStart(Player player) {
        startConsumeTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean checkFinish(Player player) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        Long start = startConsumeTime.remove(player.getUniqueId());
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            // Vanilla eating time is 32 ticks (~1600ms). If finished in less than 750ms: FastEat!
            if (duration < 750) {
                String details = String.format("กินไอเทม/ใช้งานไอเทมไวผิดปกติ (%d ms)", duration);
                flag(player, details);
                return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-action", true);
            }
        }
        return false;
    }
}

