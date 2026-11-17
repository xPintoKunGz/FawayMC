package com.mery.npc;

import com.mery.npc.command.MeryNPCCommand;
import com.mery.npc.config.ConfigManager;
import com.mery.npc.config.StorageManager;
import com.mery.npc.listener.*;
import com.mery.npc.manager.NPCManager;
import com.mery.npc.manager.SkinManager;
import com.mery.npc.nms.NMSPlayerSpawner;
import com.mery.npc.task.NPCAuraTask;
import com.mery.npc.task.NPCLookTask;
import com.mery.npc.util.ColorUtil;
import com.mery.npc.util.PlaceholderUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class MeryNPC extends JavaPlugin {

    private static MeryNPC instance;

    private ConfigManager configManager;
    private StorageManager storageManager;
    private SkinManager skinManager;
    private NPCManager npcManager;
    private NMSPlayerSpawner nmsPlayerSpawner;
    private PlayerConnectionListener playerConnectionListener;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialize MeryNPC Studio Core Managers
        this.configManager = new ConfigManager(this);
        this.skinManager = new SkinManager(this);
        this.nmsPlayerSpawner = new NMSPlayerSpawner(this);
        this.npcManager = new NPCManager(this);
        this.storageManager = new StorageManager(this);

        // 2. Setup Vault Economy (Optional)
        setupEconomy();

        // 3. Register BungeeCord outgoing & incoming channels for cross-server transfer and real-time PlayerCount
        PlaceholderUtil placeholderChannelListener = new PlaceholderUtil();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", placeholderChannelListener);

        // 4. Register MeryNPC Studio Listeners
        this.playerConnectionListener = new PlayerConnectionListener(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new NPCInteractListener(this), this);
        pm.registerEvents(new NPCProtectionListener(this), this);
        pm.registerEvents(new WorldListener(this), this);
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(new ChatInputListener(this), this);
        pm.registerEvents(playerConnectionListener, this);

        // 5. Register Commands
        MeryNPCCommand cmdHandler = new MeryNPCCommand(this);
        if (getCommand("merynpc") != null) {
            getCommand("merynpc").setExecutor(cmdHandler);
            getCommand("merynpc").setTabCompleter(cmdHandler);
        }

        // 6. Load Saved Personas
        storageManager.loadNPCs();

        // Retry spawning personas in case multi-world plugins (Multiverse-Core, etc.) load worlds after onEnable
        Bukkit.getScheduler().runTaskLater(this, () -> npcManager.checkAndSpawnPendingWorlds(), 20L);
        Bukkit.getScheduler().runTaskLater(this, () -> npcManager.checkAndSpawnPendingWorlds(), 60L);
        Bukkit.getScheduler().runTaskLater(this, () -> npcManager.checkAndSpawnPendingWorlds(), 120L);

        // 7. Start MeryNPC Studio Background Engines (Auto-Gaze, Particle Aura Ring & Real-Time PlaceholderAPI Hologram Refresh)
        new NPCLookTask(this).runTaskTimer(this, 10L, 2L);
        new NPCAuraTask(this).runTaskTimer(this, 15L, 4L);
        Bukkit.getScheduler().runTaskTimer(this, () -> npcManager.refreshAllHolograms(), 5L, 5L);
        Bukkit.getScheduler().runTaskTimer(this, () -> PlaceholderUtil.pollBungeeCounts(this), 20L, 20L);

        // Viewer range check task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                playerConnectionListener.updateViewersForPlayer(p);
            }
        }, 20L, 10L);

        // Auto-save task
        long intervalTicks = Math.max(1, configManager.getAutoSaveMinutes()) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            storageManager.saveNPCs();
        }, intervalTicks, intervalTicks);

        // Startup Banner
        sendStartupBanner();
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.saveNPCs();
        }

        if (npcManager != null) {
            npcManager.despawnAll();
        }

        getLogger().info("MeryNPC Studio disabled successfully.");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                getLogger().info("Vault economy integration hooked successfully.");
            }
        } catch (Exception ignored) {}
    }

    private void sendStartupBanner() {
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize(""));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#FF6CAB  ╔══════════════════════════════════════════════════════════╗"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#FF6CAB  ║  ✦ &#FFD166M E R Y N P C   S T U D I O   E N G I N E &#FF6CAB✦          ║"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#7366FF  ║  &fNext-Gen 3D Persona, Aura, Scale & Action System       &#7366FF║"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#7366FF  ║  &7Release: &#FFD166v26.3 Native &8| &7Active Personas: &#43E97B" + npcManager.getAllNPCs().size() + "            &#7366FF║"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize("&#FF6CAB  ╚══════════════════════════════════════════════════════════╝"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.colorize(""));
    }

    public static MeryNPC getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public SkinManager getSkinManager() {
        return skinManager;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public NMSPlayerSpawner getNmsPlayerSpawner() {
        return nmsPlayerSpawner;
    }

    public PlayerConnectionListener getPlayerConnectionListener() {
        return playerConnectionListener;
    }

    public Economy getEconomy() {
        return economy;
    }
}
