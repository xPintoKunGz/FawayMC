package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final BloomeryX plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public static class PlayerData {
        private String nickname;
        private boolean godMode;
        private boolean vanished;
        private boolean socialSpy;
        private boolean musicMuted;
        private int musicVolume = 80;
        private String musicLoop = "OFF"; // OFF, ONE, ALL
        private boolean musicShuffle = false;
        private boolean musicParticles = true;
        private boolean radioListening = false;
        private final Set<String> musicFavorites = new HashSet<>();
        private Location backLocation;
        private final Map<String, Location> homes = new HashMap<>();

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public boolean isGodMode() { return godMode; }
        public void setGodMode(boolean godMode) { this.godMode = godMode; }

        public boolean isVanished() { return vanished; }
        public void setVanished(boolean vanished) { this.vanished = vanished; }

        public boolean isSocialSpy() { return socialSpy; }
        public void setSocialSpy(boolean socialSpy) { this.socialSpy = socialSpy; }

        public boolean isMusicMuted() { return musicMuted; }
        public void setMusicMuted(boolean musicMuted) { this.musicMuted = musicMuted; }

        public int getMusicVolume() { return musicVolume; }
        public void setMusicVolume(int musicVolume) { this.musicVolume = Math.max(10, Math.min(100, musicVolume)); }

        public String getMusicLoop() { return musicLoop != null ? musicLoop : "OFF"; }
        public void setMusicLoop(String musicLoop) { this.musicLoop = musicLoop; }

        public boolean isMusicShuffle() { return musicShuffle; }
        public void setMusicShuffle(boolean musicShuffle) { this.musicShuffle = musicShuffle; }

        public boolean isMusicParticles() { return musicParticles; }
        public void setMusicParticles(boolean musicParticles) { this.musicParticles = musicParticles; }

        public boolean isRadioListening() { return radioListening; }
        public void setRadioListening(boolean radioListening) { this.radioListening = radioListening; }

        public Set<String> getMusicFavorites() { return musicFavorites; }

        public Location getBackLocation() { return backLocation; }
        public void setBackLocation(Location backLocation) { this.backLocation = backLocation; }

        public Map<String, Location> getHomes() { return homes; }
    }

    public PlayerDataManager(BloomeryX plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public PlayerData getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadData);
    }

    private PlayerData loadData(UUID uuid) {
        PlayerData data = new PlayerData();
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return data;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.setNickname(config.getString("nickname", null));
        data.setGodMode(config.getBoolean("godmode", false));
        data.setVanished(config.getBoolean("vanished", false));
        data.setSocialSpy(config.getBoolean("socialspy", false));
        data.setMusicMuted(config.getBoolean("music-muted", false));
        data.setMusicVolume(config.getInt("music-volume", 80));
        data.setMusicLoop(config.getString("music-loop", "OFF"));
        data.setMusicShuffle(config.getBoolean("music-shuffle", false));
        data.setMusicParticles(config.getBoolean("music-particles", true));
        data.setRadioListening(config.getBoolean("radio-listening", false));

        List<String> favList = config.getStringList("music-favorites");
        if (favList != null) {
            data.getMusicFavorites().addAll(favList);
        }

        if (config.contains("back")) {
            data.setBackLocation(loadLocation(config.getConfigurationSection("back")));
        }

        ConfigurationSection homesSection = config.getConfigurationSection("homes");
        if (homesSection != null) {
            for (String key : homesSection.getKeys(false)) {
                Location loc = loadLocation(homesSection.getConfigurationSection(key));
                if (loc != null) {
                    data.getHomes().put(key.toLowerCase(), loc);
                }
            }
        }

        return data;
    }

    public void saveData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("nickname", data.getNickname());
        config.set("godmode", data.isGodMode());
        config.set("vanished", data.isVanished());
        config.set("socialspy", data.isSocialSpy());
        config.set("music-muted", data.isMusicMuted());
        config.set("music-volume", data.getMusicVolume());
        config.set("music-loop", data.getMusicLoop());
        config.set("music-shuffle", data.isMusicShuffle());
        config.set("music-particles", data.isMusicParticles());
        config.set("radio-listening", data.isRadioListening());
        config.set("music-favorites", new ArrayList<>(data.getMusicFavorites()));

        if (data.getBackLocation() != null) {
            saveLocation(config.createSection("back"), data.getBackLocation());
        }

        ConfigurationSection homesSection = config.createSection("homes");
        for (Map.Entry<String, Location> entry : data.getHomes().entrySet()) {
            saveLocation(homesSection.createSection(entry.getKey()), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for: " + uuid);
        }
    }

    public void unloadData(UUID uuid) {
        saveData(uuid);
        cache.remove(uuid);
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            saveData(uuid);
        }
    }

    public String getNickname(UUID uuid) {
        return getData(uuid).getNickname();
    }

    public void setNickname(UUID uuid, String nick) {
        getData(uuid).setNickname(nick);
        saveData(uuid);
    }

    public boolean isGod(UUID uuid) {
        return getData(uuid).isGodMode();
    }

    public void setGod(UUID uuid, boolean god) {
        getData(uuid).setGodMode(god);
        saveData(uuid);
    }

    public boolean isVanished(UUID uuid) {
        return getData(uuid).isVanished();
    }

    public void setVanished(UUID uuid, boolean vanished) {
        getData(uuid).setVanished(vanished);
        saveData(uuid);
    }

    public boolean isSocialSpy(UUID uuid) {
        return getData(uuid).isSocialSpy();
    }

    public void setSocialSpy(UUID uuid, boolean spy) {
        getData(uuid).setSocialSpy(spy);
        saveData(uuid);
    }

    public boolean isMusicMuted(UUID uuid) {
        return getData(uuid).isMusicMuted();
    }

    public void setMusicMuted(UUID uuid, boolean muted) {
        getData(uuid).setMusicMuted(muted);
        saveData(uuid);
    }

    public int getMusicVolume(UUID uuid) {
        return getData(uuid).getMusicVolume();
    }

    public void setMusicVolume(UUID uuid, int volume) {
        getData(uuid).setMusicVolume(volume);
        saveData(uuid);
    }

    public String getMusicLoop(UUID uuid) {
        return getData(uuid).getMusicLoop();
    }

    public void setMusicLoop(UUID uuid, String loop) {
        getData(uuid).setMusicLoop(loop);
        saveData(uuid);
    }

    public boolean isMusicShuffle(UUID uuid) {
        return getData(uuid).isMusicShuffle();
    }

    public void setMusicShuffle(UUID uuid, boolean shuffle) {
        getData(uuid).setMusicShuffle(shuffle);
        saveData(uuid);
    }

    public boolean isMusicParticles(UUID uuid) {
        return getData(uuid).isMusicParticles();
    }

    public void setMusicParticles(UUID uuid, boolean particles) {
        getData(uuid).setMusicParticles(particles);
        saveData(uuid);
    }

    public boolean isRadioListening(UUID uuid) {
        return getData(uuid).isRadioListening();
    }

    public void setRadioListening(UUID uuid, boolean listening) {
        getData(uuid).setRadioListening(listening);
        saveData(uuid);
    }

    public Set<String> getMusicFavorites(UUID uuid) {
        return getData(uuid).getMusicFavorites();
    }

    public boolean isFavorite(UUID uuid, String songId) {
        if (songId == null) return false;
        return getData(uuid).getMusicFavorites().contains(songId.toLowerCase());
    }

    public boolean toggleFavorite(UUID uuid, String songId) {
        if (songId == null) return false;
        Set<String> favs = getData(uuid).getMusicFavorites();
        boolean added;
        if (favs.contains(songId.toLowerCase())) {
            favs.remove(songId.toLowerCase());
            added = false;
        } else {
            favs.add(songId.toLowerCase());
            added = true;
        }
        saveData(uuid);
        return added;
    }

    public Location getBackLocation(UUID uuid) {
        return getData(uuid).getBackLocation();
    }

    public void setBackLocation(UUID uuid, Location loc) {
        getData(uuid).setBackLocation(loc);
    }

    public Map<String, Location> getHomes(UUID uuid) {
        return getData(uuid).getHomes();
    }

    public Location getHome(UUID uuid, String name) {
        return getData(uuid).getHomes().get(name.toLowerCase());
    }

    public void setHome(UUID uuid, String name, Location loc) {
        getData(uuid).getHomes().put(name.toLowerCase(), loc);
        saveData(uuid);
    }

    public boolean deleteHome(UUID uuid, String name) {
        Location removed = getData(uuid).getHomes().remove(name.toLowerCase());
        if (removed != null) {
            saveData(uuid);
            return true;
        }
        return false;
    }

    public static Location loadLocation(ConfigurationSection sec) {
        if (sec == null) return null;
        String worldName = sec.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = sec.getDouble("x");
        double y = sec.getDouble("y");
        double z = sec.getDouble("z");
        float yaw = (float) sec.getDouble("yaw", 0.0);
        float pitch = (float) sec.getDouble("pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static void saveLocation(ConfigurationSection sec, Location loc) {
        if (sec == null || loc == null || loc.getWorld() == null) return;
        sec.set("world", loc.getWorld().getName());
        sec.set("x", loc.getX());
        sec.set("y", loc.getY());
        sec.set("z", loc.getZ());
        sec.set("yaw", loc.getYaw());
        sec.set("pitch", loc.getPitch());
    }
}

