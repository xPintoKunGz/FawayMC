package com.bloomerymc.bloomeryx;

import com.bloomerymc.bloomeryx.commands.*;
import com.bloomerymc.bloomeryx.integration.LuckPermsHook;
import com.bloomerymc.bloomeryx.integration.PlaceholderHook;
import com.bloomerymc.bloomeryx.listeners.*;
import com.bloomerymc.bloomeryx.manager.*;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BloomeryX extends JavaPlugin {

    private static BloomeryX instance;

    private PlayerDataManager playerDataManager;
    private WarpManager warpManager;
    private TeleportManager teleportManager;
    private AfkManager afkManager;
    private FreezeManager freezeManager;
    private MusicManager musicManager;
    private NametagManager nametagManager;
    private LuckPermsHook luckPermsHook;
    private PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize Integration Hooks First
        this.luckPermsHook = new LuckPermsHook();
        this.placeholderHook = new PlaceholderHook();

        // Initialize Managers
        this.playerDataManager = new PlayerDataManager(this);
        this.warpManager = new WarpManager(this);
        this.teleportManager = new TeleportManager(this);
        this.afkManager = new AfkManager(this);
        this.freezeManager = new FreezeManager(this);
        this.musicManager = new MusicManager(this);
        this.nametagManager = new NametagManager(this);

        // Register Listeners
        registerListeners();

        // Register Commands
        registerCommands();

        // Initial sync for online players (e.g. if loaded via PlugMan)
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.getData(player.getUniqueId());
            afkManager.updateActivity(player);
            luckPermsHook.updatePlayerNames(player);
        }

        getLogger().info(ColorUtil.colorize("&aBloomeryX v" + getDescription().getVersion() + " has been successfully activated!"));
    }

    @Override
    public void onDisable() {
        if (musicManager != null) {
            musicManager.stop();
        }
        if (nametagManager != null) {
            nametagManager.stop();
        }
        if (afkManager != null) {
            afkManager.stop();
        }
        if (freezeManager != null) {
            freezeManager.stop();
        }
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("BloomeryX has been safely disabled. Data saved.");
    }

    public void reloadPlugin() {
        reloadConfig();
        if (warpManager != null) warpManager.load();
        if (afkManager != null) afkManager.loadAfkPoint();
        if (musicManager != null) musicManager.loadMusic();

        // Resync player names
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (luckPermsHook != null) {
                luckPermsHook.updatePlayerNames(player);
            }
        }
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new PlayerChatListener(this), this);
        pm.registerEvents(new PlayerProtectionListener(this), this);
        pm.registerEvents(new PlayerActivityListener(this), this);
        pm.registerEvents(new FreezeListener(this), this);
        pm.registerEvents(new MusicListener(this), this);
    }

    private void registerCommands() {
        // Main plugin command
        BloomeryXCommand bxCmd = new BloomeryXCommand(this);
        registerCmd("bloomeryx", bxCmd);

        // Gamemode shortcuts
        GamemodeCommand gmCmd = new GamemodeCommand();
        registerCmd("gamemode", gmCmd);
        registerCmd("gms", gmCmd);
        registerCmd("gmc", gmCmd);
        registerCmd("gma", gmCmd);
        registerCmd("gmsp", gmCmd);

        // Time and Weather
        TimeWeatherCommands twCmd = new TimeWeatherCommands();
        registerCmd("day", twCmd);
        registerCmd("night", twCmd);
        registerCmd("noon", twCmd);
        registerCmd("midnight", twCmd);
        registerCmd("time", twCmd);
        registerCmd("sun", twCmd);
        registerCmd("rain", twCmd);
        registerCmd("storm", twCmd);
        registerCmd("weather", twCmd);

        // Player Utilities (includes /kill and /smite)
        PlayerUtilityCommands puCmd = new PlayerUtilityCommands(this);
        registerCmd("fly", puCmd);
        registerCmd("heal", puCmd);
        registerCmd("feed", puCmd);
        registerCmd("god", puCmd);
        registerCmd("speed", puCmd);
        registerCmd("vanish", puCmd);
        registerCmd("hat", puCmd);
        registerCmd("clear", puCmd);
        registerCmd("suicide", puCmd);
        registerCmd("ping", puCmd);
        registerCmd("near", puCmd);
        registerCmd("repair", puCmd);
        registerCmd("kill", puCmd);
        registerCmd("smite", puCmd);

        // Workbenches
        WorkbenchCommands wbCmd = new WorkbenchCommands();
        registerCmd("workbench", wbCmd);
        registerCmd("enderchest", wbCmd);
        registerCmd("anvil", wbCmd);
        registerCmd("smithing", wbCmd);
        registerCmd("cartography", wbCmd);
        registerCmd("grindstone", wbCmd);
        registerCmd("loom", wbCmd);

        // Locations, Homes, Warps, Back, Direct TP
        LocationCommands locCmd = new LocationCommands(this);
        registerCmd("home", locCmd);
        registerCmd("sethome", locCmd);
        registerCmd("delhome", locCmd);
        registerCmd("homes", locCmd);
        registerCmd("warp", locCmd);
        registerCmd("setwarp", locCmd);
        registerCmd("delwarp", locCmd);
        registerCmd("warps", locCmd);
        registerCmd("back", locCmd);
        registerCmd("tp", locCmd);
        registerCmd("tphere", locCmd);
        registerCmd("tpall", locCmd);

        // TPA
        TpaCommands tpaCmd = new TpaCommands(this);
        registerCmd("tpa", tpaCmd);
        registerCmd("tpahere", tpaCmd);
        registerCmd("tpaccept", tpaCmd);
        registerCmd("tpdeny", tpaCmd);
        registerCmd("tpcancel", tpaCmd);

        // AFK System
        AfkCommand afkCmd = new AfkCommand(this);
        registerCmd("afk", afkCmd);
        registerCmd("setafk", afkCmd);
        registerCmd("delafk", afkCmd);

        // Freeze System
        FreezeCommand freezeCmd = new FreezeCommand(this);
        registerCmd("freeze", freezeCmd);
        registerCmd("unfreeze", freezeCmd);
        registerCmd("freezeworld", freezeCmd);

        // Punish System (Ban, Unban, Ban-IP, Unban-IP)
        PunishCommands punishCmd = new PunishCommands(this);
        registerCmd("ban", punishCmd);
        registerCmd("unban", punishCmd);
        registerCmd("ban-ip", punishCmd);
        registerCmd("unban-ip", punishCmd);

        // Custom Jukebox / Music System
        MusicCommand musicCmd = new MusicCommand(this);
        registerCmd("music", musicCmd);

        // Messaging, Broadcast, Spy, Invsee
        MessagingCommands msgCmd = new MessagingCommands(this);
        registerCmd("msg", msgCmd);
        registerCmd("reply", msgCmd);
        registerCmd("socialspy", msgCmd);
        registerCmd("broadcast", msgCmd);
        registerCmd("invsee", msgCmd);

        // Nickname & LuckPerms
        NickCommand nickCmd = new NickCommand(this);
        registerCmd("nick", nickCmd);
    }

    private void registerCmd(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter) {
                cmd.setTabCompleter((org.bukkit.command.TabCompleter) executor);
            }
        }
    }

    public static BloomeryX getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public AfkManager getAfkManager() {
        return afkManager;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public NametagManager getNametagManager() {
        return nametagManager;
    }

    public PlaceholderHook getPlaceholderHook() {
        return placeholderHook;
    }

    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }
}
