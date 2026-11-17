package com.bedwars.arena;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class Team {

    private final TeamColor color;
    private Location spawnLocation;
    private Location bedLocation;
    private Location baseSpawnerLocation;
    private Location shopNpcLocation;
    private Location upgradeNpcLocation;
    private final Set<UUID> players = new HashSet<>();
    private boolean bedAlive = true;

    // Team Upgrades
    private int sharpnessLevel = 0;
    private int protectionLevel = 0;
    private int hasteLevel = 0;
    private int forgeLevel = 0;
    private boolean healPool = false;
    private boolean ironSkin = false;
    private boolean mineralMagnet = false;
    private boolean dragonBuff = false;
    private final List<String> activeTraps = new ArrayList<>();

    public Team(TeamColor color) {
        this.color = color;
    }

    public TeamColor getColor() {
        return color;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getBedLocation() {
        return bedLocation;
    }

    public void setBedLocation(Location bedLocation) {
        this.bedLocation = bedLocation;
    }

    public Location getBaseSpawnerLocation() {
        return baseSpawnerLocation;
    }

    public void setBaseSpawnerLocation(Location baseSpawnerLocation) {
        this.baseSpawnerLocation = baseSpawnerLocation;
    }

    public Location getShopNpcLocation() {
        return shopNpcLocation;
    }

    public void setShopNpcLocation(Location shopNpcLocation) {
        this.shopNpcLocation = shopNpcLocation;
    }

    public Location getUpgradeNpcLocation() {
        return upgradeNpcLocation;
    }

    public void setUpgradeNpcLocation(Location upgradeNpcLocation) {
        this.upgradeNpcLocation = upgradeNpcLocation;
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public boolean isBedAlive() {
        return bedAlive;
    }

    public void setBedAlive(boolean bedAlive) {
        this.bedAlive = bedAlive;
    }

    public boolean isEliminated() {
        return players.isEmpty();
    }

    public int getAlivePlayerCount() {
        return players.size();
    }

    public int getAlivePlayerCount(Arena arena) {
        if (arena == null) return players.size();
        int count = 0;
        for (UUID u : players) {
            if (arena.getPlayers().contains(u) || (arena.getActiveBots().containsKey(u) && arena.getActiveBots().get(u).isActive())) {
                count++;
            }
        }
        return count;
    }

    // Upgrades Getters & Setters
    public int getSharpnessLevel() {
        return sharpnessLevel;
    }

    public void setSharpnessLevel(int sharpnessLevel) {
        this.sharpnessLevel = sharpnessLevel;
    }

    public int getProtectionLevel() {
        return protectionLevel;
    }

    public void setProtectionLevel(int protectionLevel) {
        this.protectionLevel = protectionLevel;
    }

    public int getHasteLevel() {
        return hasteLevel;
    }

    public void setHasteLevel(int hasteLevel) {
        this.hasteLevel = hasteLevel;
    }

    public int getForgeLevel() {
        return forgeLevel;
    }

    public void setForgeLevel(int forgeLevel) {
        this.forgeLevel = forgeLevel;
    }

    public boolean hasHealPool() {
        return healPool;
    }

    public void setHealPool(boolean healPool) {
        this.healPool = healPool;
    }

    public boolean hasIronSkin() {
        return ironSkin;
    }

    public void setIronSkin(boolean ironSkin) {
        this.ironSkin = ironSkin;
    }

    public boolean hasMineralMagnet() {
        return mineralMagnet;
    }

    public void setMineralMagnet(boolean mineralMagnet) {
        this.mineralMagnet = mineralMagnet;
    }

    public boolean hasDragonBuff() {
        return dragonBuff;
    }

    public void setDragonBuff(boolean dragonBuff) {
        this.dragonBuff = dragonBuff;
    }

    public List<String> getActiveTraps() {
        return activeTraps;
    }

    public void addTrap(String trapType) {
        if (activeTraps.size() < 3) {
            activeTraps.add(trapType);
        }
    }

    public void reset() {
        players.clear();
        bedAlive = true;
        sharpnessLevel = 0;
        protectionLevel = 0;
        hasteLevel = 0;
        forgeLevel = 0;
        healPool = false;
        ironSkin = false;
        mineralMagnet = false;
        dragonBuff = false;
        activeTraps.clear();
    }
}

