package com.bloomerymc.bloomeryx.manager;

import org.bukkit.Sound;

import java.io.*;

public class NBSReader {

    public static NoteBlockSong readNBS(File file) {
        if (!file.exists() || !file.getName().toLowerCase().endsWith(".nbs")) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            // Little-endian reading
            short length = readShort(in);
            byte version = 0;
            byte defaultInstruments = 16;
            short songLength = length;

            if (length == 0) {
                // New Open Note Block Studio format (1, 2, 3, 4, 5)
                version = in.readByte();
                defaultInstruments = in.readByte();
                songLength = readShort(in);
            }

            short layers = readShort(in);
            String songName = readString(in);
            String songAuthor = readString(in);
            String originalAuthor = readString(in);
            String description = readString(in);
            short tempo = readShort(in); // tempo * 100

            // Skip remaining header fields
            in.readByte(); // auto-save
            in.readByte(); // auto-save duration
            in.readByte(); // time signature
            readInt(in);   // minutes spent
            readInt(in);   // left clicks
            readInt(in);   // right clicks
            readInt(in);   // blocks added
            readInt(in);   // blocks removed
            readString(in);// midi/schematic file name

            if (version >= 4) {
                in.readByte(); // loop
                in.readByte(); // max loop count
                readShort(in); // loop start tick
            }

            // Calculate tick step based on tempo (default 1000 = 10 tps = 2 MC ticks)
            double tps = tempo > 0 ? tempo / 100.0 : 10.0;
            double mcTicksPerNbsTick = 20.0 / Math.max(1.0, tps);

            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase();
            String title = (songName != null && !songName.trim().isEmpty()) ? songName.trim() : id;
            String author = (songAuthor != null && !songAuthor.trim().isEmpty()) ? songAuthor.trim() :
                    ((originalAuthor != null && !originalAuthor.trim().isEmpty()) ? originalAuthor.trim() : "Custom");

            int totalMcTicks = (int) Math.ceil(songLength * mcTicksPerNbsTick) + 30;
            NoteBlockSong song = new NoteBlockSong(id, title, author, totalMcTicks);

            // Read notes
            int currentNbsTick = -1;
            while (true) {
                short tickJumps = readShort(in);
                if (tickJumps == 0) break;
                currentNbsTick += tickJumps;

                int currentLayer = -1;
                while (true) {
                    short layerJumps = readShort(in);
                    if (layerJumps == 0) break;
                    currentLayer += layerJumps;

                    byte instrument = in.readByte();
                    byte key = in.readByte(); // 33-57, where 45 is F#4

                    float velocity = 1.0f;
                    if (version >= 4) {
                        byte vel = in.readByte(); // 0-100
                        velocity = Math.max(0.1f, vel / 100.0f);
                        in.readByte(); // panning
                        readShort(in); // pitch detune
                    }

                    int mcTick = (int) Math.round(currentNbsTick * mcTicksPerNbsTick);
                    Sound sound = getInstrument(instrument);

                    // Map NBS key to Minecraft semitones (0 to 24)
                    int semitone = key - 33;
                    while (semitone < 0) semitone += 12;
                    while (semitone > 24) semitone -= 12;

                    song.addNote(mcTick, sound, semitone, velocity);
                }
            }

            return song;
        } catch (Exception e) {
            return null;
        }
    }

    private static Sound getInstrument(byte inst) {
        switch (inst) {
            case 0: return Sound.BLOCK_NOTE_BLOCK_HARP;
            case 1: return Sound.BLOCK_NOTE_BLOCK_BASS;
            case 2: return Sound.BLOCK_NOTE_BLOCK_BASEDRUM;
            case 3: return Sound.BLOCK_NOTE_BLOCK_SNARE;
            case 4: return Sound.BLOCK_NOTE_BLOCK_HAT;
            case 5: return Sound.BLOCK_NOTE_BLOCK_GUITAR;
            case 6: return Sound.BLOCK_NOTE_BLOCK_FLUTE;
            case 7: return Sound.BLOCK_NOTE_BLOCK_BELL;
            case 8: return Sound.BLOCK_NOTE_BLOCK_CHIME;
            case 9: return Sound.BLOCK_NOTE_BLOCK_XYLOPHONE;
            case 10: return Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE;
            case 11: return Sound.BLOCK_NOTE_BLOCK_COW_BELL;
            case 12: return Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO;
            case 13: return Sound.BLOCK_NOTE_BLOCK_BIT;
            case 14: return Sound.BLOCK_NOTE_BLOCK_BANJO;
            case 15: return Sound.BLOCK_NOTE_BLOCK_PLING;
            default: return Sound.BLOCK_NOTE_BLOCK_HARP;
        }
    }

    private static short readShort(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        return (short) (b1 | (b2 << 8));
    }

    private static int readInt(DataInputStream in) throws IOException {
        int b1 = in.readUnsignedByte();
        int b2 = in.readUnsignedByte();
        int b3 = in.readUnsignedByte();
        int b4 = in.readUnsignedByte();
        return b1 | (b2 << 8) | (b3 << 16) | (b4 << 24);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readInt(in);
        if (length <= 0) return "";
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }
}
