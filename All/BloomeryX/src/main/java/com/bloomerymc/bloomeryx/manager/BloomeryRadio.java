package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BloomeryRadio {

    private final BloomeryX plugin;
    private final MusicManager musicManager;
    private final Set<UUID> listeners = ConcurrentHashMap.newKeySet();

    private List<MusicManager.Song> playlist = new ArrayList<>();
    private int currentSongIndex = 0;
    private int currentTick = 0;
    private BukkitTask radioTask;
    private boolean enabled = true;

    private static final String[] SOUNDWAVE_FRAMES = {
            " ılı.lıllılı. ",
            " ıl.ılı.lılı. ",
            " .lıllılı.ıl. ",
            " lı.lı.ılıll. ",
            " .ılıllı.lıl. "
    };

    public BloomeryRadio(BloomeryX plugin, MusicManager musicManager) {
        this.plugin = plugin;
        this.musicManager = musicManager;
        startRadio();
    }

    public void reloadPlaylist() {
        List<MusicManager.Song> list = new ArrayList<>(musicManager.getSongs().values());
        Collections.shuffle(list);
        this.playlist = list;
        this.currentSongIndex = 0;
        this.currentTick = 0;
    }

    public void startRadio() {
        if (radioTask != null) {
            radioTask.cancel();
        }

        reloadPlaylist();

        radioTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled || playlist.isEmpty()) return;

            if (currentSongIndex >= playlist.size()) {
                currentSongIndex = 0;
                Collections.shuffle(playlist);
            }

            MusicManager.Song song = playlist.get(currentSongIndex);
            NoteBlockSong nbSong = musicManager.getNoteBlockSong(song.getId());

            int totalTicks = (nbSong != null) ? nbSong.getTotalTicks() : song.getDuration() * 20;

            // Only broadcast notes/particles if there are active listeners
            if (!listeners.isEmpty()) {
                boolean hasNotes = (nbSong != null) && nbSong.hasNotesAt(currentTick);

                Iterator<UUID> it = listeners.iterator();
                while (it.hasNext()) {
                    UUID u = it.next();
                    Player p = Bukkit.getPlayer(u);
                    if (p == null || !p.isOnline()) {
                        it.remove();
                        continue;
                    }

                    float volumeMod = plugin.getPlayerDataManager().getMusicVolume(u) / 100.0f;

                    // Play note tick directly to player entity
                    if (nbSong != null) {
                        nbSong.playTick(p, currentTick, volumeMod);
                    } else if (currentTick == 0 && !song.isNoteBlock()) {
                        p.playSound(p, song.getSound(), SoundCategory.RECORDS, 1000.0f, 1.0f);
                    }

                    // Particles
                    if (hasNotes && plugin.getPlayerDataManager().isMusicParticles(u)) {
                        double angle = (currentTick * 0.4) % (2 * Math.PI);
                        double x = Math.cos(angle) * 0.8;
                        double z = Math.sin(angle) * 0.8;
                        double noteColor = (currentTick % 25);
                        p.getWorld().spawnParticle(Particle.NOTE, p.getLocation().add(x, 1.2 + Math.sin(angle) * 0.3, z), 0, noteColor / 24.0, 0, 0, 1.0);
                    }

                    // Update action bar every 10 ticks
                    if (currentTick % 10 == 0 && plugin.getConfig().getBoolean("music.action-bar", true)) {
                        String frame = SOUNDWAVE_FRAMES[(currentTick / 10) % SOUNDWAVE_FRAMES.length];
                        String timeStr = formatSeconds(currentTick / 20) + " / " + formatSeconds(totalTicks / 20);
                        String bar = ColorUtil.colorize("&#FF7EB3📻 &lRADIO &8[" + timeStr + "]&#FFD166" + frame + "&#B2EBF2" + song.getTitle() + " &8(&a" + listeners.size() + " listening&8)");
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(bar));
                    }
                }
            }

            currentTick++;
            if (currentTick >= totalTicks) {
                currentTick = 0;
                currentSongIndex = (currentSongIndex + 1) % playlist.size();
            }
        }, 10L, 1L);
    }

    public void tuneIn(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        // Stop personal song first
        musicManager.stopMusicOnly(player);

        listeners.add(uuid);
        plugin.getPlayerDataManager().setRadioListening(uuid, true);
        plugin.getPlayerDataManager().setMusicMuted(uuid, false);

        MusicManager.Song song = getCurrentSong();
        String songName = song != null ? song.getTitle() : "Tuning in...";
        MessageUtil.sendMessage(player, "&#70E000✔ &aConnected to &#FF7EB3&lBloomery Radio&a!");
        MessageUtil.sendMessage(player, "&#B2EBF2🎵 Now playing: &f" + songName);
        player.sendTitle(ColorUtil.colorize("&#FF7EB3📻 &lBloomery Radio"), ColorUtil.colorize("&f" + songName), 10, 45, 15);
        MessageUtil.playSuccess(player);
    }

    public void tuneOut(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        listeners.remove(uuid);
        plugin.getPlayerDataManager().setRadioListening(uuid, false);
        player.stopSound(SoundCategory.RECORDS);
        MessageUtil.sendMessage(player, "&#FF758C✕ &cDisconnected from Bloomery Radio.");
    }

    public boolean isListening(UUID uuid) {
        return listeners.contains(uuid);
    }

    public void toggleListener(Player player) {
        if (isListening(player.getUniqueId())) {
            tuneOut(player);
        } else {
            tuneIn(player);
        }
    }

    public void skipSong() {
        currentTick = 0;
        if (!playlist.isEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % playlist.size();
        }
    }

    public MusicManager.Song getCurrentSong() {
        if (playlist.isEmpty()) return null;
        return playlist.get(currentSongIndex % playlist.size());
    }

    public int getListenerCount() {
        return listeners.size();
    }

    public void stop() {
        if (radioTask != null) {
            radioTask.cancel();
            radioTask = null;
        }
        listeners.clear();
    }

    private String formatSeconds(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%02d:%02d", m, s);
    }
}
