package com.bedwars.replay;

import java.io.Serializable;
import java.util.*;

public class ReplayData implements Serializable {

    private String replayId;
    private String arenaName;
    private String worldName;
    private long timestamp;
    private int durationSeconds;
    private int sampleRate = 5; // Samples per second (default 5 = every 0.2s)
    private String winnerTeam;

    private Map<String, String> playerNames = new HashMap<>(); // UUID -> Name
    private Map<String, String> playerTeams = new HashMap<>(); // UUID -> TeamColor name
    private Map<String, String[]> playerSkins = new HashMap<>(); // UUID -> [textureValue, signature]
    private Map<String, Integer> playerKills = new HashMap<>();
    private Map<String, Integer> playerFinalKills = new HashMap<>();
    private Map<String, Integer> playerBedsBroken = new HashMap<>();

    // Timeline: second -> list of player snapshots
    private Map<Integer, List<PlayerSnapshot>> timeline = new TreeMap<>();

    // Block changes: chronological list
    private List<BlockChange> blockChanges = new ArrayList<>();

    // Key match events
    private List<ReplayEvent> events = new ArrayList<>();

    public ReplayData() {}

    public ReplayData(String replayId, String arenaName, String worldName, long timestamp) {
        this.replayId = replayId;
        this.arenaName = arenaName;
        this.worldName = worldName;
        this.timestamp = timestamp;
    }

    public static class PlayerSnapshot implements Serializable {
        public String uuid;
        public String name;
        public String teamColor;
        public double x, y, z;
        public float yaw, pitch;
        public boolean sneaking;
        public boolean sprinting;
        public boolean swinging; // arm swinging / attacking
        public String heldItem;
        public String offHandItem;
        public String helmet;
        public String chestplate;
        public String leggings;
        public String boots;
        public double health;
    }

    public static class BlockChange implements Serializable {
        public int second;
        public int x, y, z;
        public String world;
        public String material;
        public String blockData;
        public boolean isPlaced; // true = placed, false = broken
    }

    public static class ReplayEvent implements Serializable {
        public int second;
        public String type; // "KILL", "FINAL_KILL", "BED_BREAK", "ALERT"
        public String description;
        public String teamColor;
    }

    // Getters and Setters
    public String getReplayId() { return replayId; }
    public void setReplayId(String replayId) { this.replayId = replayId; }

    public String getArenaName() { return arenaName; }
    public void setArenaName(String arenaName) { this.arenaName = arenaName; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getSampleRate() { return sampleRate > 0 ? sampleRate : 1; }
    public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }

    public String getWinnerTeam() { return winnerTeam; }
    public void setWinnerTeam(String winnerTeam) { this.winnerTeam = winnerTeam; }

    public Map<String, String> getPlayerNames() { return playerNames; }
    public Map<String, String> getPlayerTeams() { return playerTeams; }
    public Map<String, String[]> getPlayerSkins() { return playerSkins; }
    public void setPlayerSkins(Map<String, String[]> playerSkins) { this.playerSkins = playerSkins; }
    public Map<String, Integer> getPlayerKills() { return playerKills; }
    public Map<String, Integer> getPlayerFinalKills() { return playerFinalKills; }
    public Map<String, Integer> getPlayerBedsBroken() { return playerBedsBroken; }

    public Map<Integer, List<PlayerSnapshot>> getTimeline() { return timeline; }
    public List<BlockChange> getBlockChanges() { return blockChanges; }
    public List<ReplayEvent> getEvents() { return events; }
}

