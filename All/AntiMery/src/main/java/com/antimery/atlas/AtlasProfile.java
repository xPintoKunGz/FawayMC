package com.antimery.atlas;

import com.antimery.check.CheckType;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atlas Profile for individual players.
 * Aggregates suspicion levels across Watchdog, NoCheatPlus, and Prediction checks.
 * Includes Ban Prediction Engine (ทำนายความเสี่ยงและโอกาสการถูกแบน).
 */
public class AtlasProfile {

    private final UUID uuid;
    private final String playerName;

    // Component Suspicion Scores
    private double watchdogSuspicion = 0.0;
    private double ncpSuspicion = 0.0;
    private double predictionSuspicion = 0.0;
    private double chatSuspicion = 0.0;

    // Detailed Stats
    private double lastPredictionOffset = 0.0;
    private double lastCpsStandardDeviation = 0.0;
    private double lastReachDistance = 0.0;
    private long lastFlagTimestamp = 0L;

    // GameMode 0 Flight Tracking & Ban Prediction
    private long airborneStartTimestamp = 0L;
    private double currentAirborneSeconds = 0.0;
    private boolean isCurrentlyAirborne = false;

    private final Map<CheckType, Integer> categoryFlags = new ConcurrentHashMap<>();

    public AtlasProfile(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public synchronized void addWatchdogScore(double points, CheckType type) {
        this.watchdogSuspicion += points;
        this.lastFlagTimestamp = System.currentTimeMillis();
        categoryFlags.merge(type, 1, Integer::sum);
    }

    public synchronized void addNcpScore(double points, CheckType type) {
        this.ncpSuspicion += points;
        this.lastFlagTimestamp = System.currentTimeMillis();
        categoryFlags.merge(type, 1, Integer::sum);
    }

    public synchronized void addPredictionScore(double points, CheckType type, double offset) {
        this.predictionSuspicion += points;
        this.lastPredictionOffset = offset;
        this.lastFlagTimestamp = System.currentTimeMillis();
        categoryFlags.merge(type, 1, Integer::sum);
    }

    public synchronized void addChatScore(double points, CheckType type) {
        this.chatSuspicion += points;
        this.lastFlagTimestamp = System.currentTimeMillis();
        categoryFlags.merge(type, 1, Integer::sum);
    }

    /**
     * Total aggregated suspicion score.
     */
    public synchronized double getTotalSuspicion() {
        return watchdogSuspicion + ncpSuspicion + predictionSuspicion + chatSuspicion;
    }

    /**
     * Calculates Atlas confidence percentage (0% to 100%).
     */
    public synchronized int getConfidence() {
        int categoriesTriggered = 0;
        if (watchdogSuspicion > 5) categoriesTriggered++;
        if (ncpSuspicion > 5) categoriesTriggered++;
        if (predictionSuspicion > 5) categoriesTriggered++;

        double total = getTotalSuspicion();
        if (total <= 0) return 0;

        double base = Math.min(100.0, (total / 50.0) * 100.0);

        if (categoriesTriggered >= 3) {
            base = Math.min(100.0, base * 1.5);
        } else if (categoriesTriggered >= 2) {
            base = Math.min(100.0, base * 1.25);
        }

        return (int) Math.round(base);
    }

    // ========================================== //
    //     Ban Prediction Engine (การทำนายการโดนแบน)  //
    // ========================================== //

    public synchronized void updateAirborneTime(boolean airborne, double seconds) {
        this.isCurrentlyAirborne = airborne;
        this.currentAirborneSeconds = seconds;
    }

    /**
     * ทำนายโอกาสความน่าจะเป็นที่จะถูกลงโทษ / แบน (0% - 100%)
     */
    public synchronized int getPredictedBanProbability(double maxSuspicion) {
        double suspicionRatio = Math.min(1.0, getTotalSuspicion() / Math.max(1.0, maxSuspicion));
        double flightRatio = Math.min(1.0, currentAirborneSeconds / 20.0);

        double combined = Math.max(suspicionRatio, flightRatio) * 100.0;
        return (int) Math.round(Math.min(100.0, combined));
    }

    /**
     * ข้อความทำนายสถานะการโดนแบน
     */
    public synchronized String getPredictedBanStatus(double maxSuspicion) {
        int prob = getPredictedBanProbability(maxSuspicion);
        if (prob >= 90) {
            return ChatColor.DARK_RED + "วิกฤต (Critical - ใกล้ถูกเตะ/แบนทันที 90-100%)";
        } else if (prob >= 60) {
            return ChatColor.RED + "เสี่ยงสูง (High Risk - กำลังเข้าใกล้เกณฑ์ลงโทษ 60-89%)";
        } else if (prob >= 30) {
            return ChatColor.YELLOW + "น่าสงสัย (Moderate - เริ่มมีพฤติกรรมผิดปกติ 30-59%)";
        } else {
            return ChatColor.GREEN + "ปลอดภัย (Safe - พฤติกรรมปกติ 0-29%)";
        }
    }

    /**
     * เวลาที่คาดการณ์ว่าจะโดนแบนหากยังบิน GameMode 0 ต่อเนื่อง (นับถอยหลังจาก 20 วินาที)
     */
    public synchronized double getPredictedTimeUntilBan() {
        if (!isCurrentlyAirborne) return -1.0;
        return Math.max(0.0, 20.0 - currentAirborneSeconds);
    }

    /**
     * Decays suspicion over time if the player has been clean.
     */
    public synchronized void decay(double amount) {
        watchdogSuspicion = Math.max(0.0, watchdogSuspicion - amount);
        ncpSuspicion = Math.max(0.0, ncpSuspicion - amount);
        predictionSuspicion = Math.max(0.0, predictionSuspicion - amount);
        chatSuspicion = Math.max(0.0, chatSuspicion - amount);
    }

    public synchronized void reset() {
        watchdogSuspicion = 0.0;
        ncpSuspicion = 0.0;
        predictionSuspicion = 0.0;
        chatSuspicion = 0.0;
        lastPredictionOffset = 0.0;
        airborneStartTimestamp = 0L;
        currentAirborneSeconds = 0.0;
        isCurrentlyAirborne = false;
        categoryFlags.clear();
    }

    public double getWatchdogSuspicion() {
        return watchdogSuspicion;
    }

    public double getNcpSuspicion() {
        return ncpSuspicion;
    }

    public double getPredictionSuspicion() {
        return predictionSuspicion;
    }

    public double getChatSuspicion() {
        return chatSuspicion;
    }

    public double getLastPredictionOffset() {
        return lastPredictionOffset;
    }

    public void setLastPredictionOffset(double lastPredictionOffset) {
        this.lastPredictionOffset = lastPredictionOffset;
    }

    public double getLastCpsStandardDeviation() {
        return lastCpsStandardDeviation;
    }

    public void setLastCpsStandardDeviation(double lastCpsStandardDeviation) {
        this.lastCpsStandardDeviation = lastCpsStandardDeviation;
    }

    public double getLastReachDistance() {
        return lastReachDistance;
    }

    public void setLastReachDistance(double lastReachDistance) {
        this.lastReachDistance = lastReachDistance;
    }

    public long getLastFlagTimestamp() {
        return lastFlagTimestamp;
    }

    public Map<CheckType, Integer> getCategoryFlags() {
        return categoryFlags;
    }

    public double getCurrentAirborneSeconds() {
        return currentAirborneSeconds;
    }

    public boolean isCurrentlyAirborne() {
        return isCurrentlyAirborne;
    }
}
