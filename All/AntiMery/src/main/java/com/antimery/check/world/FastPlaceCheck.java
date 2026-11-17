package com.antimery.check.world;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FastPlaceCheck extends Check {

    private final Map<UUID, List<Long>> placeTimes = new ConcurrentHashMap<>();

    public FastPlaceCheck(AntiMery plugin) {
        super(plugin, CheckType.FASTPLACE, "fastplace");
    }

    public void handleQuit(Player player) {
        placeTimes.remove(player.getUniqueId());
    }

    public boolean check(Player player) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        List<Long> places = placeTimes.computeIfAbsent(uuid, k -> new ArrayList<>());
        synchronized (places) {
            places.add(now);
            places.removeIf(time -> now - time > 1000); // 1-second window

            int count = places.size();
            int maxPlaces = plugin.getConfigManager().getCheckInt(configPath, "max-places-per-second", 15);

            if (count > maxPlaces) {
                String details = String.format("วางบล็อกรัวเกินกำหนด (%d บล็อก/วินาที > %d)", count, maxPlaces);
                flag(player, details);
                return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-place", true);
            }
        }

        return false;
    }
}

