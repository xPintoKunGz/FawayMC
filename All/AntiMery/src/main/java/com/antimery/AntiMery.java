package com.antimery;

import com.antimery.atlas.AtlasManager;
import com.antimery.check.CheckManager;
import com.antimery.client.ClientDetector;
import com.antimery.command.AntiMeryCommand;
import com.antimery.config.ConfigManager;
import com.antimery.listener.AntiHijackListener;
import com.antimery.listener.BlockListener;
import com.antimery.listener.CombatListener;
import com.antimery.listener.ChatListener;
import com.antimery.listener.PlayerListener;
import com.antimery.listener.PortBypassListener;
import com.antimery.punishment.PunishmentManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiMery extends JavaPlugin {

    private static AntiMery instance;
    private ConfigManager configManager;
    private AtlasManager atlasManager;
    private PunishmentManager punishmentManager;
    private CheckManager checkManager;
    private ClientDetector clientDetector;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialize configuration
        this.configManager = new ConfigManager(this);

        // 2. Initialize punishment & atlas managers
        this.atlasManager = new AtlasManager(this);
        this.punishmentManager = new PunishmentManager(this);

        // 3. Initialize checks
        this.checkManager = new CheckManager(this);

        // 4. Initialize client detector
        this.clientDetector = new ClientDetector(this);
        this.clientDetector.registerChannels();

        // 5. Register event listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this.clientDetector, this);
        pm.registerEvents(new AntiHijackListener(this), this);
        pm.registerEvents(new PortBypassListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new ChatListener(this), this);

        // 5. Register commands
        AntiMeryCommand cmd = new AntiMeryCommand(this);
        PluginCommand pluginCommand = getCommand("antimery");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(cmd);
            pluginCommand.setTabCompleter(cmd);
        }

        getLogger().info("==========================================");
        getLogger().info(" AntiMery v" + getDescription().getVersion() + " ได้ถูกเปิดใช้งานแล้ว!");
        getLogger().info(" ระบบป้องกันการบิน, กันแฮคทุกประเภท และกรองคำหยาบ TH/EN");
        getLogger().info(" ค่าเริ่มต้นการลงโทษ: " + configManager.getDefaultPunishment().name());
        getLogger().info("==========================================");
    }

    @Override
    public void onDisable() {
        if (clientDetector != null) {
            clientDetector.unregisterChannels();
        }
        getLogger().info("AntiMery v" + getDescription().getVersion() + " ปิดการทำงานเรียบร้อยแล้ว");
    }

    public static AntiMery getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AtlasManager getAtlasManager() {
        return atlasManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public ClientDetector getClientDetector() {
        return clientDetector;
    }
}

