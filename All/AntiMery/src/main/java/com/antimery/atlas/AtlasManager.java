package com.antimery.atlas;

import com.antimery.AntiMery;
import com.antimery.check.CheckType;
import com.antimery.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atlas (Heuristic & Suspicion Assessment Engine)
 * Inspires by Hypixel's Atlas system. Aggregates flags and calculates confidence.
 * Operates completely silently without chat logs or player warnings.
 * When suspicion exceeds the threshold, automated punishment is executed immediately.
 */
public class AtlasManager {

    private final AntiMery plugin;
    private final Map<UUID, AtlasProfile> profiles = new ConcurrentHashMap<>();

    public AtlasManager(AntiMery plugin) {
        this.plugin = plugin;
        startDecayTask();
    }

    public AtlasProfile getProfile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), k -> new AtlasProfile(player.getUniqueId(), player.getName()));
    }

    public AtlasProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    public void handleQuit(Player player) {
        profiles.remove(player.getUniqueId());
    }

    /**
     * Records a flag event into Atlas and verifies if automated punishment is required.
     */
    public void recordFlag(Player player, CheckType type, double suspicionWeight, String details) {
        if (player == null || !player.isOnline()) return;

        AtlasProfile profile = getProfile(player);

        // Classify which pillar this belongs to
        switch (type) {
            case KILLAURA:
            case AUTOCLICKER:
            case REACH:
            case CRITICALS:
                profile.addWatchdogScore(suspicionWeight, type);
                break;

            case FLY:
            case SPEED:
            case NOFALL:
            case JESUS:
            case HIGHJUMP:
            case FASTCLIMB:
            case FASTBREAK:
            case FASTPLACE:
            case FASTUSE:
                profile.addNcpScore(suspicionWeight, type);
                break;

            case PREDICTION:
                profile.addPredictionScore(suspicionWeight, type, 0.0);
                break;

            case CHAT_FILTER:
                profile.addChatScore(suspicionWeight, type);
                break;

            default:
                profile.addWatchdogScore(suspicionWeight, type);
                break;
        }

        // Check if total suspicion has breached the limit
        double total = profile.getTotalSuspicion();
        double maxSuspicion = plugin.getConfigManager().getConfig().getDouble("atlas.max-suspicion", 100.0);

        if (total >= maxSuspicion) {
            String punishmentStr = plugin.getConfigManager().getConfig().getString("atlas.punishment", "KICK");
            PunishmentType punishment = PunishmentType.fromString(punishmentStr);

            String reason = String.format("ตรวจพบพฤติกรรมผิดปกติโดย Atlas (Suspicion: %.0f, Conf: %d%%)",
                    total, profile.getConfidence());

            // Reset profile before executing punishment so they don't get punished repeatedly
            profile.reset();

            // Execute punishment silently without chat warning/alert
            plugin.getPunishmentManager().executePunishment(player, CheckType.ATLAS, punishment, reason);
        }
    }

    /**
     * Periodic task to decay suspicion points over time for fair players.
     */
    private void startDecayTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            double decayRate = plugin.getConfigManager().getConfig().getDouble("atlas.decay-per-minute", 5.0);
            for (AtlasProfile profile : profiles.values()) {
                profile.decay(decayRate);
            }
        }, 20L * 60, 20L * 60); // every 60 seconds
    }
}

