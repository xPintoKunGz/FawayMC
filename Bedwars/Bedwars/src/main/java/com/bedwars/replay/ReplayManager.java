package com.bedwars.replay;

import com.bedwars.BedwarsPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ReplayManager {

    private final BedwarsPlugin plugin;
    private final File replaysFolder;
    private final Gson gson = new GsonBuilder().create();
    private final Map<UUID, ReplaySession> activeSessions = new ConcurrentHashMap<>();

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000L;

    public ReplayManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        this.replaysFolder = new File(plugin.getDataFolder(), "replays");
        if (!replaysFolder.exists()) {
            replaysFolder.mkdirs();
        }
        purgeExpiredReplays();
    }

    public void saveReplayAsync(ReplayData data) {
        if (data == null || data.getReplayId() == null) return;
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File file = new File(replaysFolder, data.getReplayId() + ".bwreplay");
                try (OutputStream fos = new FileOutputStream(file);
                     GZIPOutputStream gzos = new GZIPOutputStream(fos);
                     Writer writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {
                    gson.toJson(data, writer);
                }
                plugin.getLogger().info("Saved match replay: " + file.getName() + " (" + (file.length() / 1024) + " KB)");
                purgeExpiredReplays();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save replay: " + e.getMessage());
            }
        });
    }

    public List<ReplayData> getPlayerReplays(UUID playerUuid) {
        purgeExpiredReplays();
        List<ReplayData> list = new ArrayList<>();
        File[] files = replaysFolder.listFiles((dir, name) -> name.endsWith(".bwreplay"));
        if (files == null) return list;

        String uuidStr = playerUuid.toString();
        for (File f : files) {
            try (InputStream fis = new FileInputStream(f);
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 Reader reader = new InputStreamReader(gzis, StandardCharsets.UTF_8)) {
                ReplayData data = gson.fromJson(reader, ReplayData.class);
                if (data != null && data.getPlayerNames().containsKey(uuidStr)) {
                    list.add(data);
                }
            } catch (Exception ignored) {}
        }

        // Sort descending by timestamp (newest first)
        list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return list;
    }

    public void purgeExpiredReplays() {
        File[] files = replaysFolder.listFiles((dir, name) -> name.endsWith(".bwreplay"));
        if (files == null) return;

        long now = System.currentTimeMillis();
        int deletedCount = 0;
        for (File f : files) {
            if (now - f.lastModified() > SEVEN_DAYS_MS) {
                if (f.delete()) {
                    deletedCount++;
                }
            }
        }
        if (deletedCount > 0) {
            plugin.getLogger().info("Purged " + deletedCount + " expired replay(s) older than 1 week.");
        }
    }

    public boolean deleteReplay(String replayId) {
        if (replayId == null || replayId.isEmpty()) return false;
        File file = new File(replaysFolder, replayId + ".bwreplay");
        if (file.exists()) {
            return file.delete();
        }
        File[] files = replaysFolder.listFiles((dir, name) -> name.startsWith(replayId) && name.endsWith(".bwreplay"));
        if (files != null && files.length > 0) {
            return files[0].delete();
        }
        return false;
    }

    public void startPlayback(Player player, ReplayData data) {
        if (player == null || data == null) return;
        stopPlayback(player, true);
        com.bedwars.arena.Arena currentArena = plugin.getArenaManager().getArena(player);
        if (currentArena != null) {
            if (currentArena.getState() == com.bedwars.arena.ArenaState.IN_GAME) {
                com.bedwars.util.MessageUtil.sendMessage(player, "§c✖ You cannot watch replays while playing an active match! Leave or finish first.");
                return;
            }
            currentArena.leave(player);
        }
        ReplaySession session = new ReplaySession(plugin, player, data);
        activeSessions.put(player.getUniqueId(), session);
        session.start();
    }

    public void stopPlayback(Player player) {
        stopPlayback(player, false);
    }

    public void stopPlayback(Player player, boolean silent) {
        if (player == null) return;
        ReplaySession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.stop(silent);
        }
    }

    public void removeSession(UUID uuid) {
        if (uuid != null) {
            activeSessions.remove(uuid);
        }
    }

    public boolean isWatchingReplay(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public ReplaySession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public void stopAll() {
        for (ReplaySession s : activeSessions.values()) {
            s.stop();
        }
        activeSessions.clear();
    }
}

