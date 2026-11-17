package com.merycraft.meryitem;

import com.merycraft.meryitem.command.MeryItemCommand;
import com.merycraft.meryitem.command.MeryItemTabCompleter;
import com.merycraft.meryitem.effect.ParticleEffectTask;
import com.merycraft.meryitem.gui.ChatInputListener;
import com.merycraft.meryitem.gui.MenuListener;
import com.merycraft.meryitem.hook.HookManager;
import com.merycraft.meryitem.item.ItemManager;
import com.merycraft.meryitem.listener.CombatListener;
import com.merycraft.meryitem.listener.EquipmentListener;
import com.merycraft.meryitem.listener.ItemAbilityListener;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * MeryITEM - Modern, intuitive RPG Item Framework for Minecraft 1.20, 1.21 & 26.x.
 */
public class MeryItemPlugin extends JavaPlugin {

    private static MeryItemPlugin instance;
    private ItemManager itemManager;
    private HookManager hookManager;
    private EquipmentListener equipmentListener;
    private com.merycraft.meryitem.upgrade.UpgradeManager upgradeManager;

    public static MeryItemPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Print modern banner
        sendStartupBanner();

        // Save default configs
        saveDefaultConfig();

        // Initialize managers
        this.itemManager = new ItemManager(this);
        this.itemManager.loadAll();

        this.upgradeManager = new com.merycraft.meryitem.upgrade.UpgradeManager(this);
        this.upgradeManager.loadRecipes();

        this.hookManager = new HookManager(this);
        this.hookManager.init();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new ChatInputListener(), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ItemAbilityListener(this), this);

        this.equipmentListener = new EquipmentListener(this);
        Bukkit.getPluginManager().registerEvents(equipmentListener, this);
        this.equipmentListener.startTask();

        // Start particle aura effect task for held weapons
        new ParticleEffectTask(this).start();

        // Register commands
        registerCommands();

        getLogger().info("MeryITEM has been successfully enabled! Ready for action.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MeryITEM has been disabled. Goodbye!");
        instance = null;
    }

    public void reloadAll() {
        reloadConfig();
        itemManager.loadAll();
        if (upgradeManager != null) {
            upgradeManager.loadRecipes();
        }
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public com.merycraft.meryitem.upgrade.UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    private void registerCommands() {
        MeryItemCommand cmd = new MeryItemCommand(this);
        MeryItemTabCompleter tab = new MeryItemTabCompleter(this);

        PluginCommand mitem = getCommand("mitem");
        if (mitem != null) {
            mitem.setExecutor(cmd);
            mitem.setTabCompleter(tab);
        }

        PluginCommand meryitem = getCommand("meryitem");
        if (meryitem != null) {
            meryitem.setExecutor(cmd);
            meryitem.setTabCompleter(tab);
        }
    }

    public void saveDefaultItemFiles() {
        File itemsFolder = new File(getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
        }

        saveResourceIfNotExists("items/weapons.yml");
        saveResourceIfNotExists("items/armor.yml");
        saveResourceIfNotExists("items/accessories.yml");
        saveResourceIfNotExists("items/consumables.yml");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File outFile = new File(getDataFolder(), resourcePath);
        if (!outFile.exists()) {
            try (InputStream in = getResource(resourcePath)) {
                if (in != null) {
                    Files.copy(in, outFile.toPath());
                }
            } catch (Exception e) {
                getLogger().warning("Could not save default resource " + resourcePath + ": " + e.getMessage());
            }
        }
    }

    private void sendStartupBanner() {
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#334155§m                                                  "));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#F472B6  __  __                  _____ _______ ______ __  __ "));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#F472B6 |  \\/  |                |_   _|__   __|  ____|  \\/  |"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#FB7185 | \\  / | ___ _ __ _   _  | |    | |  | |__  | \\  / |"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#FDA4AF | |\\/| |/ _ \\ '__| | | | | |    | |  |  __| | |\\/| |"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#C084FC | |  | |  __/ |  | |_| |_| |_   | |  | |____| |  | |"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#38BDF8 |_|  |_|\\___|_|   \\__, |_____|  |_|  |______|_|  |_|"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#38BDF8                    __/ |                               "));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color(" &#38BDF8                   |___/   &#CBD5E1v" + getDescription().getVersion() + " &#334155• &#34D399Online"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#334155§m                                                  "));
    }
}

