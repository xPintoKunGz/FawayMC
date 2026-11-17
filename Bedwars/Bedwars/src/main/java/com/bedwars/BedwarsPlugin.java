package com.bedwars;

import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaManager;
import com.bedwars.commands.BedwarsCommand;
import com.bedwars.listeners.BlockListener;
import com.bedwars.listeners.PlayerListener;
import com.bedwars.listeners.SetupListener;
import com.bedwars.listeners.ShopListener;
import com.bedwars.replay.ReplayManager;
import com.bedwars.scoreboard.ScoreboardManager;
import com.bedwars.util.LocationUtil;
import com.bedwars.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class BedwarsPlugin extends JavaPlugin {

    private static BedwarsPlugin instance;
    private ArenaManager arenaManager;
    private ScoreboardManager scoreboardManager;
    private ReplayManager replayManager;

    private File messagesFile;
    private FileConfiguration messagesConfig;
    private Location mainLobby;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadCustomConfigs();

        // Load Main Spawn/Lobby Location
        String spawnStr = getConfig().getString("server.spawn-location");
        if (spawnStr == null || spawnStr.isEmpty()) {
            spawnStr = getConfig().getString("server.lobby-location");
        }
        if (spawnStr != null && !spawnStr.isEmpty()) {
            this.mainLobby = LocationUtil.deserialize(spawnStr);
        } else if (!getServer().getWorlds().isEmpty()) {
            this.mainLobby = getServer().getWorlds().get(0).getSpawnLocation();
        }

        // Initialize Managers
        this.arenaManager = new ArenaManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.replayManager = new ReplayManager(this);

        // Register Command
        BedwarsCommand cmd = new BedwarsCommand(this);
        if (getCommand("bedwars") != null) {
            getCommand("bedwars").setExecutor(cmd);
            getCommand("bedwars").setTabCompleter(cmd);
        }

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new SetupListener(this), this);

        // Register PlaceholderAPI Expansion
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.bedwars.integration.PlaceholderAPIHook(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        if (com.bedwars.integration.HoloMeryHook.isAvailable()) {
            getLogger().info("Successfully hooked into HoloMery for Holograms!");
        }

        if (com.bedwars.integration.MeryNPCHook.isAvailable()) {
            getLogger().info("Successfully hooked into MeryNPC for Interactive NPCs!");
        }

        getLogger().info("==========================================");
        getLogger().info(" Bedwars Next-Gen Plugin Enabled!");
        getLogger().info(" Compatible with Minecraft 26.3 / 1.20+");
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        if (replayManager != null) {
            replayManager.stopAll();
        }
        if (arenaManager != null) {
            for (Arena arena : arenaManager.getArenas()) {
                arena.reset();
            }
            arenaManager.saveArenas();
        }
        getLogger().info("Bedwars plugin disabled.");
    }

    public void loadCustomConfigs() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static BedwarsPlugin getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public ReplayManager getReplayManager() {
        return replayManager;
    }

    public Location getMainLobby() {
        if (mainLobby != null) return mainLobby;
        if (!getServer().getWorlds().isEmpty()) {
            return getServer().getWorlds().get(0).getSpawnLocation();
        }
        return null;
    }

    public void setMainLobby(Location mainLobby) {
        this.mainLobby = mainLobby;
    }

    public String getMessage(String path) {
        String prefix = messagesConfig != null ? messagesConfig.getString("prefix", "§6§l✦ BEDWARS §8» §r") : "§6§l✦ BEDWARS §8» §r";
        String msg = messagesConfig != null ? messagesConfig.getString(path, path) : path;
        return MessageUtil.color(prefix + msg);
    }
}

