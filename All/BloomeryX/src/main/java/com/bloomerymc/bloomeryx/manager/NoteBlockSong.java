package com.bloomerymc.bloomeryx.manager;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.*;

public class NoteBlockSong {

    private final String id;
    private final String title;
    private final String author;
    private final int totalTicks;
    private final Map<Integer, List<Note>> track = new HashMap<>();

    public static class Note {
        private final Sound sound;
        private final float pitch;
        private final float volume;

        public Note(Sound sound, float pitch, float volume) {
            this.sound = sound;
            this.pitch = pitch;
            this.volume = volume;
        }

        public void play(Player player, float volumeModifier) {
            if (player != null && player.isOnline()) {
                float effectiveVolume = Math.max(0.05f, Math.min(2.0f, volume * volumeModifier));
                player.playSound(player, sound, SoundCategory.RECORDS, effectiveVolume, pitch);
            }
        }

        public Sound getSound() { return sound; }
        public float getPitch() { return pitch; }
        public float getVolume() { return volume; }
    }

    public NoteBlockSong(String id, String title, String author, int totalTicks) {
        this.id = id.toLowerCase();
        this.title = title;
        this.author = author;
        this.totalTicks = Math.max(20, totalTicks);
    }

    public void addNote(int tick, Sound instrument, int semitoneFromFSharp) {
        addNote(tick, instrument, semitoneFromFSharp, 1.0f);
    }

    public void addNote(int tick, Sound instrument, int semitoneFromFSharp, float volume) {
        // Minecraft semitone 0 to 24 (F#3 to F#5)
        float pitch = (float) Math.pow(2.0, (semitoneFromFSharp - 12) / 12.0);
        track.computeIfAbsent(tick, k -> new ArrayList<>()).add(new Note(instrument, pitch, volume));
    }

    public void addDrum(int tick, Sound drum, float volume) {
        track.computeIfAbsent(tick, k -> new ArrayList<>()).add(new Note(drum, 1.0f, volume));
    }

    public boolean hasNotesAt(int tick) {
        List<Note> notes = track.get(tick);
        return notes != null && !notes.isEmpty();
    }

    public void playTick(Player player, int tick, float volumeModifier) {
        List<Note> notes = track.get(tick);
        if (notes != null) {
            for (Note n : notes) {
                n.play(player, volumeModifier);
            }
        }
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getTotalTicks() { return totalTicks; }
}
