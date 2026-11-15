package org.kcm.bloomery.fawaymc.bungee.manager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import org.kcm.bloomery.fawaymc.bungee.Global;

/**
 * บันทึก "เซิร์ฟเวอร์ล่าสุด" ของผู้เล่นแต่ละคนลง sessions.yml
 * ใช้ตอน rejoin เพื่อให้ผู้เล่นกลับไปเซิร์ฟเวอร์เดิม
 */
public class PlayerSessionManager {

    private static final long AUTO_SAVE_SECONDS = 60L;

    private final Global plugin;
    private final File file;
    private final Map<UUID, String> lastServers = new ConcurrentHashMap<>();

    private Configuration config;
    private volatile boolean dirty = false;
    private int saveTaskId = -1;

    public PlayerSessionManager(Global plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sessions.yml");
        this.load();
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            config = new Configuration();
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[Session] ไม่สามารถสร้าง sessions.yml", e);
            }
        } else {
            try {
                config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[Session] โหลด sessions.yml ไม่สำเร็จ", e);
                config = new Configuration();
            }
        }

        lastServers.clear();
        for (String key : config.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                String server = config.getString(key);
                if (server != null && !server.isEmpty()) {
                    lastServers.put(uuid, server);
                }
            } catch (IllegalArgumentException ignored) {
                // skip malformed keys
            }
        }

        plugin.getLogger().info("[Session] โหลด " + lastServers.size() + " รายการ");
    }

    /** บันทึกแบบ synchronous — ใช้ตอน shutdown */
    public synchronized void save() {
        if (!dirty) return;

        Configuration out = new Configuration();
        for (Map.Entry<UUID, String> e : lastServers.entrySet()) {
            out.set(e.getKey().toString(), e.getValue());
        }

        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(out, file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Session] บันทึก sessions.yml ไม่สำเร็จ", e);
        }
    }

    /** บันทึกแบบ async — ใช้ตอน runtime เพื่อไม่บล็อก main thread */
    public void saveAsync() {
        if (!dirty) return;
        plugin.getProxy().getScheduler().runAsync(plugin, this::save);
    }

    public String getLastServer(UUID uuid) {
        return lastServers.get(uuid);
    }

    public void setLastServer(UUID uuid, String serverName) {
        if (uuid == null || serverName == null || serverName.isEmpty()) return;
        String prev = lastServers.put(uuid, serverName);
        if (!serverName.equals(prev)) {
            dirty = true;
        }
    }

    public void removeLastServer(UUID uuid) {
        if (lastServers.remove(uuid) != null) {
            dirty = true;
        }
    }

    /** ตั้ง task บันทึกอัตโนมัติทุก AUTO_SAVE_SECONDS วินาที */
    public void startAutoSave() {
        stopAutoSave();
        saveTaskId = plugin.getProxy().getScheduler()
                .schedule(plugin, this::saveAsync, AUTO_SAVE_SECONDS, AUTO_SAVE_SECONDS, TimeUnit.SECONDS)
                .getId();
    }

    private void stopAutoSave() {
        if (saveTaskId != -1) {
            try {
                plugin.getProxy().getScheduler().cancel(saveTaskId);
            } catch (Exception ignored) {
            }
            saveTaskId = -1;
        }
    }

    public void stop() {
        stopAutoSave();
        save();
    }
}