package com.holomery.manager;

import com.holomery.HoloMeryPlugin;
import com.holomery.model.BillboardMode;
import com.holomery.model.HoloLine;
import com.holomery.model.HoloPage;
import com.holomery.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages holograms, multi-page persistence, auto-refresh, and auto-page cycling.
 */
public class HologramManager {

    private static HologramManager instance;
    private final HoloMeryPlugin plugin;
    private final Map<String, Hologram> holograms = new LinkedHashMap<>();
    private File storageFile;
    private FileConfiguration storageConfig;
    private BukkitTask refreshTask;
    private BukkitTask autoPageTask;
    private BukkitTask spinTask;
    private long tickCounter = 0;

    public HologramManager(HoloMeryPlugin plugin) {
        instance = this;
        this.plugin = plugin;
    }

    public static HologramManager getInstance() {
        return instance;
    }

    public void initialize() {
        storageFile = new File(plugin.getDataFolder(), "holograms.yml");
        if (!storageFile.exists()) {
            plugin.saveResource("holograms.yml", false);
        }
        loadHolograms();
        startTasks();
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        if (autoPageTask != null) {
            autoPageTask.cancel();
            autoPageTask = null;
        }
        if (spinTask != null) {
            spinTask.cancel();
            spinTask = null;
        }
        saveHolograms();
        for (Hologram holo : holograms.values()) {
            holo.despawn();
        }
        holograms.clear();
    }

    public Hologram createHologram(String id, Location location) {
        String cleanId = id.toLowerCase().trim();
        if (holograms.containsKey(cleanId)) {
            return null;
        }

        Hologram holo = new Hologram(cleanId, location);

        // Apply defaults from config
        boolean defaultTransparent = plugin.getConfig().getBoolean("settings.default-transparent-background", true);
        holo.setTransparentBackground(defaultTransparent);

        double defaultSpacing = plugin.getConfig().getDouble("settings.default-line-spacing", 0.30);
        holo.setLineSpacing(defaultSpacing);

        float defaultScale = (float) plugin.getConfig().getDouble("settings.default-scale", 1.0);
        holo.setScale(defaultScale);

        boolean defaultShadow = plugin.getConfig().getBoolean("settings.default-shadow", true);
        holo.setShadow(defaultShadow);

        boolean defaultSeeThrough = plugin.getConfig().getBoolean("settings.default-see-through", false);
        holo.setSeeThrough(defaultSeeThrough);

        String billboardStr = plugin.getConfig().getString("settings.default-billboard", "CENTER");
        holo.setBillboard(BillboardMode.fromString(billboardStr));

        holograms.put(cleanId, holo);
        holo.spawn();
        saveHolograms();
        return holo;
    }

    public boolean deleteHologram(String id) {
        String cleanId = id.toLowerCase().trim();
        Hologram holo = holograms.remove(cleanId);
        if (holo != null) {
            holo.despawn();
            saveHolograms();
            return true;
        }
        return false;
    }

    public Hologram getHologram(String id) {
        if (id == null) return null;
        return holograms.get(id.toLowerCase().trim());
    }

    public Collection<Hologram> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    public boolean exists(String id) {
        if (id == null) return false;
        return holograms.containsKey(id.toLowerCase().trim());
    }

    public void loadHolograms() {
        for (Hologram h : holograms.values()) {
            h.despawn();
        }
        holograms.clear();

        storageConfig = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection root = storageConfig.getConfigurationSection("holograms");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;

            String worldName = sec.getString("world");
            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not loaded for hologram '" + id + "'. Skipping for now.");
                continue;
            }

            double x = sec.getDouble("x");
            double y = sec.getDouble("y");
            double z = sec.getDouble("z");
            float yaw = (float) sec.getDouble("yaw", 0.0);
            float pitch = (float) sec.getDouble("pitch", 0.0);
            Location loc = new Location(world, x, y, z, yaw, pitch);

            Hologram holo = new Hologram(id.toLowerCase(), loc);
            holo.setLineSpacing(sec.getDouble("spacing", 0.30));
            holo.setBillboard(BillboardMode.fromString(sec.getString("billboard", "CENTER")));
            holo.setShadow(sec.getBoolean("shadow", true));
            holo.setSeeThrough(sec.getBoolean("see-through", false));
            holo.setTransparentBackground(sec.getBoolean("transparent-background", false));
            holo.setScale((float) sec.getDouble("scale", 1.0));
            holo.setViewRange((float) sec.getDouble("view-range", 1.0));
            holo.setClickPageSwitch(sec.getBoolean("click-switch", true));
            holo.setAutoPageIntervalSeconds(sec.getInt("auto-page-interval", 0));
            holo.setSpin(sec.getBoolean("spin", false));

            String alignStr = sec.getString("alignment", "CENTER");
            try {
                holo.setAlignment(TextDisplay.TextAlignment.valueOf(alignStr.toUpperCase()));
            } catch (Exception ignored) {
                holo.setAlignment(TextDisplay.TextAlignment.CENTER);
            }

            if (sec.contains("background-color")) {
                int argb = sec.getInt("background-color");
                holo.setBackgroundColor(Color.fromARGB(argb));
            }

            // Multi-page loading
            List<?> pagesList = sec.getList("pages");
            if (pagesList != null && !pagesList.isEmpty()) {
                holo.getPages(); // Ensure list is ready
                // Clear default page
                while (holo.getPageCount() > 0) {
                    holo.removePage(0);
                }

                for (Object pageObj : pagesList) {
                    if (pageObj instanceof Map<?, ?> pageMap) {
                        Object linesObj = pageMap.get("lines");
                        if (linesObj instanceof List<?> rawLines) {
                            HoloPage page = new HoloPage();
                            for (Object l : rawLines) {
                                page.addLine(String.valueOf(l));
                            }
                            Object actionsObj = pageMap.get("actions");
                            if (actionsObj instanceof List<?> rawActions) {
                                for (Object a : rawActions) {
                                    page.addClickAction(String.valueOf(a));
                                }
                            }
                            holo.addPage(page);
                        }
                    } else if (pageObj instanceof List<?> rawLines) {
                        HoloPage page = new HoloPage();
                        for (Object l : rawLines) {
                            page.addLine(String.valueOf(l));
                        }
                        holo.addPage(page);
                    }
                }
            } else if (sec.contains("lines")) {
                // Legacy single-page format fallback
                List<String> legacyLines = sec.getStringList("lines");
                for (String l : legacyLines) {
                    holo.addLine(l);
                }
            }

            if (holo.getPageCount() == 0) {
                holo.addPage(new HoloPage());
            }

            holograms.put(id.toLowerCase(), holo);
            holo.spawn();
        }

        plugin.getLogger().info("Successfully loaded " + holograms.size() + " holograms.");
    }

    public void saveHolograms() {
        if (storageConfig == null) {
            storageConfig = new YamlConfiguration();
        }

        storageConfig.set("holograms", null);

        for (Hologram holo : holograms.values()) {
            String path = "holograms." + holo.getId();
            Location loc = holo.getLocation();

            storageConfig.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
            storageConfig.set(path + ".x", loc.getX());
            storageConfig.set(path + ".y", loc.getY());
            storageConfig.set(path + ".z", loc.getZ());
            storageConfig.set(path + ".yaw", loc.getYaw());
            storageConfig.set(path + ".pitch", loc.getPitch());
            storageConfig.set(path + ".spacing", holo.getLineSpacing());
            storageConfig.set(path + ".billboard", holo.getBillboard().name());
            storageConfig.set(path + ".shadow", holo.isShadow());
            storageConfig.set(path + ".see-through", holo.isSeeThrough());
            storageConfig.set(path + ".transparent-background", holo.isTransparentBackground());
            storageConfig.set(path + ".scale", holo.getScale());
            storageConfig.set(path + ".view-range", holo.getViewRange());
            storageConfig.set(path + ".alignment", holo.getAlignment().name());
            storageConfig.set(path + ".click-switch", holo.isClickPageSwitch());
            storageConfig.set(path + ".auto-page-interval", holo.getAutoPageIntervalSeconds());
            storageConfig.set(path + ".spin", holo.isSpin());

            if (holo.getBackgroundColor() != null) {
                storageConfig.set(path + ".background-color", holo.getBackgroundColor().asARGB());
            }

            List<Map<String, Object>> serializedPages = new ArrayList<>();
            for (HoloPage page : holo.getPages()) {
                Map<String, Object> map = new LinkedHashMap<>();
                List<String> lines = new ArrayList<>();
                for (HoloLine line : page.getLines()) {
                    lines.add(line.getRawText());
                }
                map.put("lines", lines);
                if (!page.getClickActions().isEmpty()) {
                    map.put("actions", new ArrayList<>(page.getClickActions()));
                }
                serializedPages.add(map);
            }
            storageConfig.set(path + ".pages", serializedPages);
        }

        try {
            storageConfig.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save holograms to " + storageFile.getName(), e);
        }
    }

    public void onChunkLoad(World world, int chunkX, int chunkZ) {
        for (Hologram holo : holograms.values()) {
            Location loc = holo.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                if ((loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ) {
                    if (!holo.isSpawned()) {
                        holo.spawn();
                    }
                }
            }
        }
    }

    public void onChunkUnload(World world, int chunkX, int chunkZ) {
        for (Hologram holo : holograms.values()) {
            Location loc = holo.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                if ((loc.getBlockX() >> 4) == chunkX && (loc.getBlockZ() >> 4) == chunkZ) {
                    holo.despawn();
                }
            }
        }
    }

    public void cleanOrphanEntities(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.hasMetadata("HoloMery-Id") || entity.hasMetadata("HoloMery-Interact")) {
                entity.remove();
            }
        }
    }

    private void startTasks() {
        int intervalTicks = plugin.getConfig().getInt("settings.refresh-interval-ticks", 20);
        if (intervalTicks <= 0) intervalTicks = 20;

        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Hologram holo : holograms.values()) {
                if (holo.isSpawned()) {
                    holo.update(null);
                }
            }
        }, intervalTicks, intervalTicks);

        // Auto-page cycling task (every 1 second = 20 ticks)
        autoPageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCounter++;
            for (Hologram holo : holograms.values()) {
                if (holo.isSpawned() && holo.getPageCount() > 1 && holo.getAutoPageIntervalSeconds() > 0) {
                    if (tickCounter % holo.getAutoPageIntervalSeconds() == 0) {
                        holo.nextPage(null);
                    }
                }
            }
        }, 20L, 20L);

        // Smooth 360-degree spin animation task (runs every 1 tick for smooth 60fps rotation)
        spinTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Hologram holo : holograms.values()) {
                if (holo.isSpawned() && holo.isSpin()) {
                    holo.tickSpin();
                }
            }
        }, 1L, 1L);
    }
}
