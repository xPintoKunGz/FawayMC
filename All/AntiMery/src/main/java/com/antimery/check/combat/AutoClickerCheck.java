package com.antimery.check.combat;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watchdog AutoClicker Engine
 * วิเคราะห์สถิติการคลิก (Statistical & Heuristic Click Analysis):
 * 1. ขีดจำกัดความเร็วสูงสุด (CPS Limit)
 * 2. ค่าเบี่ยงเบนมาตรฐาน (Standard Deviation / Jitter): มนุษย์คลิกจะมีจังหวะหน่วงต่างกัน (StdDev > 12ms)
 *    หากเป็น Macro/AutoClicker ค่าความแปรปรวนจะสม่ำเสมอผิดธรรมชาติ (StdDev ต่ำมาก)
 * 3. ตรวจจับการหน่วงเวลาซ้ำเป๊ะ (Duplicate Delays)
 */
public class AutoClickerCheck extends Check {

    private final Map<UUID, List<Long>> clickTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> clickDelays = new ConcurrentHashMap<>();

    public AutoClickerCheck(AntiMery plugin) {
        super(plugin, CheckType.AUTOCLICKER, "autoclicker");
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        clickTimestamps.remove(uuid);
        clickDelays.remove(uuid);
    }

    public boolean check(Player player) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        List<Long> timestamps = clickTimestamps.computeIfAbsent(uuid, k -> new ArrayList<>());
        List<Long> delays = clickDelays.computeIfAbsent(uuid, k -> new ArrayList<>());

        synchronized (timestamps) {
            if (!timestamps.isEmpty()) {
                long lastClick = timestamps.get(timestamps.size() - 1);
                long delay = now - lastClick;
                if (delay > 0 && delay < 1000) {
                    delays.add(delay);
                    if (delays.size() > 20) {
                        delays.remove(0);
                    }
                }
            }

            timestamps.add(now);
            timestamps.removeIf(time -> now - time > 1000); // 1-second window

            int cps = timestamps.size();
            int maxCps = plugin.getConfigManager().getCheckInt(configPath, "max-cps", 20);

            // 1. Raw CPS Cap Check
            if (cps > maxCps) {
                String details = String.format("คลิกเร็วเกินไป (%d CPS > %d CPS)", cps, maxCps);
                flag(player, details);
                return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
            }

            // 2. Watchdog Statistical Analysis (เมื่อมีตัวอย่างคลิกพอสมควร >= 15 ครั้ง)
            if (delays.size() >= 15 && cps >= 12) {
                double stdDev = calculateStandardDeviation(delays);

                if (plugin.getAtlasManager() != null) {
                    plugin.getAtlasManager().getProfile(player).setLastCpsStandardDeviation(stdDev);
                }

                // หากกดรัวเกิน 12 CPS แต่ค่าเบี่ยงเบนเวลาคลิกแทบไม่กระดิก (< 5ms) = Macro/AutoClicker แน่นอน
                if (stdDev < 5.0) {
                    String details = String.format("Watchdog Heuristics: คลิกสม่ำเสมอผิดธรรมชาติ (StdDev: %.2f ms, CPS: %d)", stdDev, cps);
                    flag(player, details);
                    return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
                }
            }
        }

        return false;
    }

    private double calculateStandardDeviation(List<Long> values) {
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (long val : values) {
            sum += val;
        }
        double mean = sum / values.size();

        double sumSquares = 0.0;
        for (long val : values) {
            sumSquares += Math.pow(val - mean, 2);
        }
        return Math.sqrt(sumSquares / values.size());
    }
}
