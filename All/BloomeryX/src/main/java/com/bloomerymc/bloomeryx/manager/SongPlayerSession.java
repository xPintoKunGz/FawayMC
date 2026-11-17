package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class SongPlayerSession {

    private final BloomeryX plugin;
    private final MusicManager musicManager;
    private final UUID playerUuid;
    private final MusicManager.Song song;
    private final NoteBlockSong noteBlockSong;
    private final int totalTicks;

    private int currentTick = 0;
    private boolean paused = false;
    private BukkitTask task;
    private String lastWorldName = null;

    private static final String[] SOUNDWAVE_FRAMES = {
            " ılı.lıllılı. ",
            " ıl.ılı.lılı. ",
            " .lıllılı.ıl. ",
            " lı.lı.ılıll. ",
            " .ılıllı.lıl. "
    };

    public SongPlayerSession(BloomeryX plugin, MusicManager musicManager, Player player, MusicManager.Song song) {
        this.plugin = plugin;
        this.musicManager = musicManager;
        this.playerUuid = player.getUniqueId();
        this.song = song;
        this.noteBlockSong = musicManager.getNoteBlockSong(song.getId());
        this.totalTicks = (noteBlockSong != null) ? noteBlockSong.getTotalTicks() : song.getDuration() * 20;
        this.lastWorldName = player.getWorld().getName();
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) return;

        // Start non-NBS disc attached directly to the player entity (1000.0f volume follows everywhere)
        if (noteBlockSong == null && !song.isNoteBlock()) {
            player.playSound(player, song.getSound(), SoundCategory.RECORDS, 1000.0f, 1.0f);
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                Player p = Bukkit.getPlayer(playerUuid);
                if (p == null || !p.isOnline()) {
                    cancel();
                    musicManager.onSessionEnded(playerUuid);
                    return;
                }

                if (paused) return;

                // Dimension switch tracking - re-anchor sound if player teleported across worlds
                String currentWorld = p.getWorld().getName();
                if (lastWorldName != null && !lastWorldName.equals(currentWorld)) {
                    if (noteBlockSong == null && !song.isNoteBlock()) {
                        p.playSound(p, song.getSound(), SoundCategory.RECORDS, 1000.0f, 1.0f);
                    }
                }
                lastWorldName = currentWorld;

                float volumeMod = plugin.getPlayerDataManager().getMusicVolume(playerUuid) / 100.0f;
                boolean hasNotes = false;

                if (noteBlockSong != null) {
                    hasNotes = noteBlockSong.hasNotesAt(currentTick);
                    noteBlockSong.playTick(p, currentTick, volumeMod);
                }

                // Note particles dancing to the beat
                if (hasNotes && plugin.getPlayerDataManager().isMusicParticles(playerUuid)) {
                    double angle = (currentTick * 0.45) % (2 * Math.PI);
                    double x = Math.cos(angle) * 0.75;
                    double z = Math.sin(angle) * 0.75;
                    double noteColor = (currentTick % 25);
                    p.getWorld().spawnParticle(Particle.NOTE, p.getLocation().add(x, 1.3 + Math.sin(angle) * 0.2, z), 0, noteColor / 24.0, 0, 0, 1.0);
                }

                // Action Bar live visualizer
                if (currentTick % 10 == 0 && plugin.getConfig().getBoolean("music.action-bar", true)) {
                    String frame = SOUNDWAVE_FRAMES[(currentTick / 10) % SOUNDWAVE_FRAMES.length];
                    String timeStr = formatSeconds(currentTick / 20) + " / " + formatSeconds(totalTicks / 20);
                    String bar = ColorUtil.colorize("&#FF7EB3♬ &l" + (paused ? "⏸ PAUSED" : "PLAYING") + " &8[" + timeStr + "]&#FFD166" + frame + "&#B2EBF2" + song.getTitle() + " &7- &#FFB4A2" + song.getAuthor());
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
                }

                currentTick++;

                // Song finished
                if (currentTick >= totalTicks) {
                    cancel();
                    musicManager.handleSongCompletion(p, song);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void pause() {
        this.paused = true;
        Player p = Bukkit.getPlayer(playerUuid);
        if (p != null && p.isOnline()) {
            p.stopSound(SoundCategory.RECORDS);
        }
    }

    public void resume() {
        this.paused = false;
        Player p = Bukkit.getPlayer(playerUuid);
        if (p != null && p.isOnline() && noteBlockSong == null && !song.isNoteBlock()) {
            p.playSound(p, song.getSound(), SoundCategory.RECORDS, 1000.0f, 1.0f);
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        Player p = Bukkit.getPlayer(playerUuid);
        if (p != null && p.isOnline()) {
            p.stopSound(SoundCategory.RECORDS);
            if (!song.isNoteBlock()) {
                p.stopSound(song.getSound(), SoundCategory.RECORDS);
            }
        }
    }

    public MusicManager.Song getSong() {
        return song;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    private String formatSeconds(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%02d:%02d", m, s);
    }
}

