package com.bedwars.arena;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.spawner.Spawner;
import com.bedwars.arena.spawner.SpawnerType;
import com.bedwars.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final BedwarsPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private File arenasFile;
    private FileConfiguration arenasConfig;

    public ArenaManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }

    public void loadArenas() {
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        arenas.clear();

        ConfigurationSection section = arenasConfig.getConfigurationSection("arenas");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection aSec = section.getConfigurationSection(key);
            if (aSec == null) continue;

            Arena arena = new Arena(key);
            arena.setDisplayName(aSec.getString("display-name", key));
            arena.setMode(aSec.getString("mode", "Solo"));
            arena.setEnabled(aSec.getBoolean("enabled", true));
            arena.setMinPlayers(aSec.getInt("min-players", 2));
            arena.setMaxPlayers(aSec.getInt("max-players", 8));
            arena.setPlayersPerTeam(aSec.getInt("players-per-team", 1));

            if (aSec.contains("lobby")) {
                arena.setLobbyLocation(LocationUtil.deserialize(aSec.getString("lobby")));
            }
            if (aSec.contains("spectator")) {
                arena.setSpectatorLocation(LocationUtil.deserialize(aSec.getString("spectator")));
            }
            if (aSec.contains("pos1")) {
                arena.setPos1(LocationUtil.deserialize(aSec.getString("pos1")));
            }
            if (aSec.contains("pos2")) {
                arena.setPos2(LocationUtil.deserialize(aSec.getString("pos2")));
            }

            // Teams
            ConfigurationSection teamsSec = aSec.getConfigurationSection("teams");
            if (teamsSec != null) {
                for (String tKey : teamsSec.getKeys(false)) {
                    try {
                        TeamColor color = TeamColor.valueOf(tKey.toUpperCase());
                        Team team = new Team(color);
                        if (teamsSec.contains(tKey + ".spawn")) {
                            team.setSpawnLocation(LocationUtil.deserialize(teamsSec.getString(tKey + ".spawn")));
                        }
                        if (teamsSec.contains(tKey + ".bed")) {
                            team.setBedLocation(LocationUtil.deserialize(teamsSec.getString(tKey + ".bed")));
                        }
                        if (teamsSec.contains(tKey + ".spawner")) {
                            team.setBaseSpawnerLocation(LocationUtil.deserialize(teamsSec.getString(tKey + ".spawner")));
                        }
                        if (teamsSec.contains(tKey + ".shop")) {
                            team.setShopNpcLocation(LocationUtil.deserialize(teamsSec.getString(tKey + ".shop")));
                        }
                        if (teamsSec.contains(tKey + ".upgrade")) {
                            team.setUpgradeNpcLocation(LocationUtil.deserialize(teamsSec.getString(tKey + ".upgrade")));
                        }
                        arena.addTeam(team);
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            // Spawners
            List<String> spList = aSec.getStringList("spawners");
            for (String spStr : spList) {
                String[] parts = spStr.split("\\|");
                if (parts.length >= 2) {
                    try {
                        SpawnerType type = SpawnerType.valueOf(parts[0].toUpperCase());
                        Location loc = LocationUtil.deserialize(parts[1]);
                        if (loc != null) {
                            arena.addSpawner(new Spawner(type, loc));
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            // NPCs
            List<String> shopList = aSec.getStringList("shops");
            for (String s : shopList) {
                Location loc = LocationUtil.deserialize(s);
                if (loc != null) arena.getShopNpcLocations().add(loc);
            }

            List<String> upList = aSec.getStringList("upgrades");
            for (String s : upList) {
                Location loc = LocationUtil.deserialize(s);
                if (loc != null) arena.getUpgradeNpcLocations().add(loc);
            }

            arenas.put(key.toLowerCase(), arena);
        }
        plugin.getLogger().info("Loaded " + arenas.size() + " Bedwars arena(s).");
    }

    public void saveArenas() {
        arenasConfig.set("arenas", null);
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.getName();
            arenasConfig.set(path + ".display-name", arena.getDisplayName());
            arenasConfig.set(path + ".enabled", arena.isEnabled());
            arenasConfig.set(path + ".mode", arena.getMode());
            arenasConfig.set(path + ".min-players", arena.getMinPlayers());
            arenasConfig.set(path + ".max-players", arena.getMaxPlayers());
            arenasConfig.set(path + ".players-per-team", arena.getPlayersPerTeam());

            if (arena.getLobbyLocation() != null) {
                arenasConfig.set(path + ".lobby", LocationUtil.serialize(arena.getLobbyLocation()));
            }
            if (arena.getSpectatorLocation() != null) {
                arenasConfig.set(path + ".spectator", LocationUtil.serialize(arena.getSpectatorLocation()));
            }
            if (arena.getPos1() != null) {
                arenasConfig.set(path + ".pos1", LocationUtil.serialize(arena.getPos1()));
            }
            if (arena.getPos2() != null) {
                arenasConfig.set(path + ".pos2", LocationUtil.serialize(arena.getPos2()));
            }

            // Teams
            for (Team team : arena.getTeams().values()) {
                String tPath = path + ".teams." + team.getColor().name();
                if (team.getSpawnLocation() != null) {
                    arenasConfig.set(tPath + ".spawn", LocationUtil.serialize(team.getSpawnLocation()));
                }
                if (team.getBedLocation() != null) {
                    arenasConfig.set(tPath + ".bed", LocationUtil.serialize(team.getBedLocation()));
                }
                if (team.getBaseSpawnerLocation() != null) {
                    arenasConfig.set(tPath + ".spawner", LocationUtil.serialize(team.getBaseSpawnerLocation()));
                }
                if (team.getShopNpcLocation() != null) {
                    arenasConfig.set(tPath + ".shop", LocationUtil.serialize(team.getShopNpcLocation()));
                }
                if (team.getUpgradeNpcLocation() != null) {
                    arenasConfig.set(tPath + ".upgrade", LocationUtil.serialize(team.getUpgradeNpcLocation()));
                }
            }

            // Spawners
            List<String> spList = new ArrayList<>();
            for (Spawner sp : arena.getSpawners()) {
                spList.add(sp.getType().name() + "|" + LocationUtil.serialize(sp.getLocation()));
            }
            arenasConfig.set(path + ".spawners", spList);

            // NPCs
            List<String> shopList = new ArrayList<>();
            for (Location loc : arena.getShopNpcLocations()) {
                shopList.add(LocationUtil.serialize(loc));
            }
            arenasConfig.set(path + ".shops", shopList);

            List<String> upList = new ArrayList<>();
            for (Location loc : arena.getUpgradeNpcLocations()) {
                upList.add(LocationUtil.serialize(loc));
            }
            arenasConfig.set(path + ".upgrades", upList);
        }

        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public Arena getArena(String name) {
        if (name == null) return null;
        return arenas.get(name.toLowerCase());
    }

    public Arena getArena(Player player) {
        if (player == null) return null;
        for (Arena arena : arenas.values()) {
            if (arena.isPlayerInGame(player.getUniqueId())) {
                return arena;
            }
        }
        return null;
    }

    public Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public void addArena(Arena arena) {
        arenas.put(arena.getName().toLowerCase(), arena);
        saveArenas();
    }

    public void removeArena(String name) {
        arenas.remove(name.toLowerCase());
        saveArenas();
    }

    public Arena findBestAvailableArena() {
        Arena best = null;
        int maxPlayers = -1;
        for (Arena a : arenas.values()) {
            if (a.getState() == ArenaState.WAITING || a.getState() == ArenaState.STARTING) {
                int count = a.getPlayers().size();
                if (count < a.getMaxPlayers() && count > maxPlayers) {
                    maxPlayers = count;
                    best = a;
                }
            }
        }
        return best;
    }

    public boolean isArenaReady(Arena arena) {
        if (arena.getLobbyLocation() == null) return false;
        if (arena.getTeams().size() < 2) return false;
        for (Team t : arena.getTeams().values()) {
            if (t.getSpawnLocation() == null || t.getBedLocation() == null) return false;
        }
        return !arena.getSpawners().isEmpty();
    }
}

