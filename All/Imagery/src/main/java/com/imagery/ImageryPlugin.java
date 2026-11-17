package com.imagery;

import com.imagery.command.ImageryCommand;
import com.imagery.command.ImageryTabCompleter;
import com.imagery.config.ConfigManager;
import com.imagery.gui.GuiListener;
import com.imagery.manager.ImageFileManager;
import com.imagery.manager.ParticleHighlightManager;
import com.imagery.manager.PlacedImageManager;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public class ImageryPlugin extends JavaPlugin {

    private static ImageryPlugin instance;

    private ConfigManager configManager;
    private ImageFileManager imageFileManager;
    private com.imagery.manager.AlbumManager albumManager;
    private PlacedImageManager placedImageManager;
    private ParticleHighlightManager particleHighlightManager;

    @Override
    public void onLoad() {
        instance = this;
        ConfigurationSerialization.registerClass(PlacedImage.class);
    }

    @Override
    public void onEnable() {
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        this.imageFileManager = new ImageFileManager(this);
        this.imageFileManager.init();

        this.albumManager = new com.imagery.manager.AlbumManager(this);
        this.albumManager.init();

        this.placedImageManager = new PlacedImageManager(this);
        this.placedImageManager.loadPlacedImages();

        this.particleHighlightManager = new ParticleHighlightManager(this);

        // Register commands & completer
        if (getCommand("imagery") != null) {
            ImageryCommand cmd = new ImageryCommand(this);
            getCommand("imagery").setExecutor(cmd);
            getCommand("imagery").setTabCompleter(new ImageryTabCompleter(this));
        }

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.imagery.gui.WorldInteractionListener(this), this);

        // Console banner
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#00f2fe============================================="));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#00f2fe  ❖ &#4facfe&lImagery System 26.3 &aENABLED"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#a18cd1  - Loaded Images: &f" + imageFileManager.getAllMetadata().size() + " file(s)"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#a18cd1  - Loaded Albums: &f" + albumManager.getAllAlbums().size() + " album(s)"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#a18cd1  - Active Placed: &f" + placedImageManager.getAllPlacedImages().size() + " location(s)"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#00f5a0  - Interactive Multi-Page & Click Commands Active!"));
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#00f2fe============================================="));
    }

    @Override
    public void onDisable() {
        if (particleHighlightManager != null) {
            particleHighlightManager.cancelAll();
        }

        if (placedImageManager != null) {
            placedImageManager.stopAutoSlideshowTask();
            placedImageManager.savePlacedImages();
        }

        if (albumManager != null) {
            albumManager.saveAlbums();
        }

        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&#ff0844[Imagery] Plugin disabled safely."));
    }

    public static ImageryPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ImageFileManager getImageFileManager() {
        return imageFileManager;
    }

    public com.imagery.manager.AlbumManager getAlbumManager() {
        return albumManager;
    }

    public PlacedImageManager getPlacedImageManager() {
        return placedImageManager;
    }

    public ParticleHighlightManager getParticleHighlightManager() {
        return particleHighlightManager;
    }
}

