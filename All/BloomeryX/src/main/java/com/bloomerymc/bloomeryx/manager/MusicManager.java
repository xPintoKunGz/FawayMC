package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MusicManager {

    private final BloomeryX plugin;
    private final File musicFile;
    private final File songsFolder;
    private FileConfiguration musicConfig;

    private final Map<String, Song> songs = new LinkedHashMap<>();
    private final Map<String, NoteBlockSong> noteBlockSongs = new LinkedHashMap<>();
    private final Map<UUID, SongPlayerSession> activeSessions = new ConcurrentHashMap<>();

    private BloomeryRadio radio;

    public static class Song {
        private final String id;
        private final String title;
        private final String author;
        private final String sound;
        private final Material material;
        private final int duration;
        private final boolean isNoteBlock;

        public Song(String id, String title, String author, String sound, Material material, int duration, boolean isNoteBlock) {
            this.id = id.toLowerCase();
            this.title = title;
            this.author = author;
            this.sound = sound;
            this.material = material != null ? material : Material.MUSIC_DISC_CAT;
            this.duration = duration;
            this.isNoteBlock = isNoteBlock;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getSound() { return sound; }
        public Material getMaterial() { return material; }
        public int getDuration() { return duration; }
        public boolean isNoteBlock() { return isNoteBlock; }
    }

    public MusicManager(BloomeryX plugin) {
        this.plugin = plugin;
        this.musicFile = new File(plugin.getDataFolder(), "music.yml");
        this.songsFolder = new File(plugin.getDataFolder(), "songs");
        if (!songsFolder.exists()) {
            songsFolder.mkdirs();
        }
        loadMusic();
        this.radio = new BloomeryRadio(plugin, this);
    }

    public void loadMusic() {
        songs.clear();
        noteBlockSongs.clear();

        // 1. Scan and load all Note Block Studio (.nbs) files in songs/ folder
        loadNbsFilesFromFolder();

        // 2. Scan and load all custom song .yml files in songs/ folder
        loadCustomSongsFromFolder();

        // 3. Load from music.yml (Vanilla Minecraft discs - zero resource pack needed)
        if (!musicFile.exists()) {
            createDefaultMusicFile();
        }
        musicConfig = YamlConfiguration.loadConfiguration(musicFile);
        ConfigurationSection sec = musicConfig.getConfigurationSection("songs");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                loadSongFromSection(key, sec.getConfigurationSection(key));
            }
        }

        if (radio != null) {
            radio.reloadPlaylist();
        }
    }

    private void loadNbsFilesFromFolder() {
        File[] nbsFiles = songsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".nbs"));
        if (nbsFiles == null || nbsFiles.length == 0) {
            createNbsReadmeGuide();
            return;
        }

        for (File f : nbsFiles) {
            try {
                NoteBlockSong nbSong = NBSReader.readNBS(f);
                if (nbSong != null) {
                    noteBlockSongs.put(nbSong.getId(), nbSong);
                    songs.put(nbSong.getId(), new Song(
                            nbSong.getId(),
                            nbSong.getTitle(),
                            nbSong.getAuthor(),
                            "noteblock",
                            Material.JUKEBOX,
                            nbSong.getTotalTicks() / 20,
                            true
                    ));
                    plugin.getLogger().info("Loaded Note Block Studio song: " + nbSong.getTitle() + " (" + f.getName() + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load .nbs file: " + f.getName());
            }
        }
    }

    private void loadSongFromSection(String key, ConfigurationSection s) {
        if (s == null) return;
        if (songs.containsKey(key.toLowerCase())) return;

        String title = s.getString("title", key);
        String author = s.getString("author", "Unknown");
        String sound = s.getString("sound", "music_disc.pigstep");
        String matName = s.getString("material", "MUSIC_DISC_PIGSTEP");
        int duration = s.getInt("duration", 120);

        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.MUSIC_DISC_CAT;

        songs.put(key.toLowerCase(), new Song(key.toLowerCase(), title, author, sound, mat, duration, false));
    }

    private void loadCustomSongsFromFolder() {
        File[] files = songsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }

        for (File f : files) {
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
                String id = f.getName().substring(0, f.getName().length() - 4).toLowerCase();
                if (songs.containsKey(id)) continue;

                String title = cfg.getString("title", id);
                String author = cfg.getString("author", "Custom Artist");
                String sound = cfg.getString("sound", "music_disc.otherside");
                String matName = cfg.getString("material", "MUSIC_DISC_OTHERSIDE");
                int duration = cfg.getInt("duration", 180);

                Material mat = Material.matchMaterial(matName);
                if (mat == null) mat = Material.JUKEBOX;

                songs.put(id, new Song(id, title, author, sound, mat, duration, false));
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load song file: " + f.getName());
            }
        }
    }

    private void createNbsReadmeGuide() {
        File readme = new File(songsFolder, "README_HOW_TO_ADD_SONGS.txt");
        if (!readme.exists()) {
            try (FileWriter writer = new FileWriter(readme)) {
                writer.write("====================================================================\n");
                writer.write("          BloomeryX — Note Block Studio (.nbs) Songs Folder         \n");
                writer.write("====================================================================\n\n");
                writer.write("How to add custom songs:\n");
                writer.write("  1. Create or export your song as .nbs from Minecraft Note Block Studio.\n");
                writer.write("  2. Place the .nbs file into this folder (plugins/BloomeryX/songs/).\n");
                writer.write("  3. Run /music reload in-game or from console.\n");
                writer.write("  4. Open /music to play your song instantly with NO resource pack required!\n");
            } catch (IOException ignored) {}
        }
    }

    private void createDefaultMusicFile() {
        musicConfig = new YamlConfiguration();
        musicConfig.set("autoplay-on-join", true);
        musicConfig.set("default-song", "pigstep");

        addDefaultSong(musicConfig, "pigstep", "Pigstep", "Lena Raine", "music_disc.pigstep", "MUSIC_DISC_PIGSTEP", 148);
        addDefaultSong(musicConfig, "otherside", "Otherside", "Lena Raine", "music_disc.otherside", "MUSIC_DISC_OTHERSIDE", 195);
        addDefaultSong(musicConfig, "relic", "Relic", "Aaron Cherof", "music_disc.relic", "MUSIC_DISC_RELIC", 218);
        addDefaultSong(musicConfig, "creator", "Creator", "Lena Raine", "music_disc.creator", "MUSIC_DISC_CREATOR", 176);
        addDefaultSong(musicConfig, "cat", "Cat", "C418", "music_disc.cat", "MUSIC_DISC_CAT", 185);

        try {
            musicConfig.save(musicFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create music.yml");
        }
    }

    private void addDefaultSong(FileConfiguration config, String id, String title, String author, String sound, String mat, int duration) {
        config.set("songs." + id + ".title", title);
        config.set("songs." + id + ".author", author);
        config.set("songs." + id + ".sound", sound);
        config.set("songs." + id + ".material", mat);
        config.set("songs." + id + ".duration", duration);
    }

    /**
     * Plays song for a player using a dedicated SongPlayerSession.
     */
    public void playSong(Player player, Song song) {
        if (player == null || song == null) return;
        UUID uuid = player.getUniqueId();

        // Tune out of radio if currently listening
        if (radio != null && radio.isListening(uuid)) {
            radio.tuneOut(player);
        }

        // Stop current session if any
        stopMusicOnly(player);

        // Unmute player preference
        plugin.getPlayerDataManager().setMusicMuted(uuid, false);

        SongPlayerSession session = new SongPlayerSession(plugin, this, player, song);
        activeSessions.put(uuid, session);
        session.start();

        MessageUtil.sendMessage(player, "&#FF7EB3🎵 Now Playing: &#B2EBF2" + song.getTitle() + " &7- &#FFB4A2" + song.getAuthor());
        player.sendTitle(ColorUtil.colorize("&#FF7EB3🎵 &l" + song.getTitle()), ColorUtil.colorize("&7" + song.getAuthor()), 10, 45, 15);
        MessageUtil.playSuccess(player);
    }

    public void pauseSong(Player player) {
        if (player == null) return;
        SongPlayerSession session = activeSessions.get(player.getUniqueId());
        if (session != null && !session.isPaused()) {
            session.pause();
            MessageUtil.sendMessage(player, "&#FFD166⏸ Music paused. Type &f/music resume &eto continue.");
        }
    }

    public void resumeSong(Player player) {
        if (player == null) return;
        SongPlayerSession session = activeSessions.get(player.getUniqueId());
        if (session != null && session.isPaused()) {
            session.resume();
            MessageUtil.sendMessage(player, "&#70E000▶ Music resumed: &#B2EBF2" + session.getSong().getTitle());
        }
    }

    public void togglePauseResume(Player player) {
        if (player == null) return;
        SongPlayerSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            if (session.isPaused()) {
                resumeSong(player);
            } else {
                pauseSong(player);
            }
        }
    }

    /**
     * Stops the song playback without setting music-muted.
     */
    public void stopMusicOnly(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        SongPlayerSession session = activeSessions.remove(uuid);
        if (session != null) {
            session.stop();
        }
    }

    /**
     * Completely stops playback and saves persistent mute state.
     */
    public void muteAndStopMusic(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (radio != null && radio.isListening(uuid)) {
            radio.tuneOut(player);
        }
        stopMusicOnly(player);
        plugin.getPlayerDataManager().setMusicMuted(uuid, true);
        MessageUtil.sendMessage(player, "&#FF758C■ Music stopped.");
    }

    public void nextSong(Player player) {
        if (player == null) return;
        List<Song> songList = new ArrayList<>(songs.values());
        if (songList.isEmpty()) return;

        SongPlayerSession session = activeSessions.get(player.getUniqueId());
        int nextIndex = 0;
        if (session != null) {
            for (int i = 0; i < songList.size(); i++) {
                if (songList.get(i).getId().equalsIgnoreCase(session.getSong().getId())) {
                    nextIndex = (i + 1) % songList.size();
                    break;
                }
            }
        }
        playSong(player, songList.get(nextIndex));
    }

    public void previousSong(Player player) {
        if (player == null) return;
        List<Song> songList = new ArrayList<>(songs.values());
        if (songList.isEmpty()) return;

        SongPlayerSession session = activeSessions.get(player.getUniqueId());
        int prevIndex = songList.size() - 1;
        if (session != null) {
            for (int i = 0; i < songList.size(); i++) {
                if (songList.get(i).getId().equalsIgnoreCase(session.getSong().getId())) {
                    prevIndex = (i - 1 + songList.size()) % songList.size();
                    break;
                }
            }
        }
        playSong(player, songList.get(prevIndex));
    }

    public void playRandomSong(Player player) {
        if (player == null || songs.isEmpty()) return;
        List<Song> songList = new ArrayList<>(songs.values());
        Song r = songList.get(new Random().nextInt(songList.size()));
        playSong(player, r);
    }

    public void handleSongCompletion(Player player, Song finishedSong) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        activeSessions.remove(uuid);

        String loopMode = plugin.getPlayerDataManager().getMusicLoop(uuid);
        boolean shuffle = plugin.getPlayerDataManager().isMusicShuffle(uuid);

        if ("ONE".equalsIgnoreCase(loopMode)) {
            playSong(player, finishedSong);
        } else if (shuffle) {
            playRandomSong(player);
        } else if ("ALL".equalsIgnoreCase(loopMode)) {
            nextSong(player);
        }
    }

    public void onSessionEnded(UUID uuid) {
        activeSessions.remove(uuid);
    }

    public void toggleLoop(Player player) {
        UUID uuid = player.getUniqueId();
        String current = plugin.getPlayerDataManager().getMusicLoop(uuid);
        String next;
        if ("OFF".equalsIgnoreCase(current)) {
            next = "ONE";
            MessageUtil.sendMessage(player, "&#70E000🔁 Loop Mode: &aRepeat One");
        } else if ("ONE".equalsIgnoreCase(current)) {
            next = "ALL";
            MessageUtil.sendMessage(player, "&#70E000🔁 Loop Mode: &aRepeat All");
        } else {
            next = "OFF";
            MessageUtil.sendMessage(player, "&#FF758C🔁 Loop Mode: &cOff");
        }
        plugin.getPlayerDataManager().setMusicLoop(uuid, next);
        MessageUtil.playSuccess(player);
    }

    public void toggleShuffle(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = plugin.getPlayerDataManager().isMusicShuffle(uuid);
        boolean next = !current;
        plugin.getPlayerDataManager().setMusicShuffle(uuid, next);
        MessageUtil.sendMessage(player, next ?
                "&#70E000🔀 Shuffle Mode: &aEnabled (ON)" :
                "&#FF758C🔀 Shuffle Mode: &cDisabled (OFF)");
        MessageUtil.playSuccess(player);
    }

    public void cycleVolume(Player player) {
        UUID uuid = player.getUniqueId();
        int cur = plugin.getPlayerDataManager().getMusicVolume(uuid);
        int next;
        if (cur <= 20) next = 40;
        else if (cur <= 40) next = 60;
        else if (cur <= 60) next = 80;
        else if (cur <= 80) next = 100;
        else next = 20;

        plugin.getPlayerDataManager().setMusicVolume(uuid, next);
        MessageUtil.sendMessage(player, "&#70E000🔊 Volume: &#B2EBF2" + next + "%");
        MessageUtil.playSuccess(player);
    }

    public boolean isGlobalAutoplayEnabled() {
        return musicConfig != null && musicConfig.getBoolean("autoplay-on-join", true);
    }

    public void setGlobalAutoplay(boolean enabled) {
        if (musicConfig == null) musicConfig = new YamlConfiguration();
        musicConfig.set("autoplay-on-join", enabled);
        try {
            musicConfig.save(musicFile);
        } catch (IOException ignored) {}
    }

    public void setDefaultSong(String songId) {
        if (musicConfig == null) musicConfig = new YamlConfiguration();
        musicConfig.set("default-song", songId.toLowerCase());
        try {
            musicConfig.save(musicFile);
        } catch (IOException ignored) {}
    }

    public void handleJoinAutoplay(Player player) {
        if (!isGlobalAutoplayEnabled()) return;
        if (plugin.getPlayerDataManager().isMusicMuted(player.getUniqueId())) return;

        if (plugin.getPlayerDataManager().isRadioListening(player.getUniqueId()) && radio != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    radio.tuneIn(player);
                }
            }, 30L);
            return;
        }

        String defId = musicConfig != null ? musicConfig.getString("default-song", "pigstep") : "pigstep";
        Song song = songs.get(defId);
        if (song == null && !songs.isEmpty()) {
            song = songs.values().iterator().next();
        }

        if (song != null) {
            final Song finalSong = song;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !plugin.getPlayerDataManager().isMusicMuted(player.getUniqueId())) {
                    playSong(player, finalSong);
                }
            }, 30L);
        }
    }

    /**
     * Ultra-Clean, Super-Intuitive Music Menu.
     * Songs are at slots 0-44 (45 songs per page).
     * Bottom dock (45-53) contains clear control icons.
     */
    public void openJukeboxGui(Player player, int page) {
        List<Song> songList = new ArrayList<>(songs.values());

        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.colorize("&#FF7EB3&lBloomeryX Music &8| Page " + (page + 1)));

        int pageSize = 45;
        int maxPages = Math.max(1, (int) Math.ceil((double) songList.size() / pageSize));
        int currentPage = Math.max(0, Math.min(page, maxPages - 1));

        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, songList.size());

        SongPlayerSession curSession = activeSessions.get(player.getUniqueId());
        String currentId = (curSession != null) ? curSession.getSong().getId() : null;

        // Render song discs (Slots 0 to 44)
        for (int i = startIndex; i < endIndex; i++) {
            Song song = songList.get(i);
            int slot = i - startIndex;

            boolean isCurrent = song.getId().equalsIgnoreCase(currentId);

            ItemStack item = new ItemStack(song.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.colorize((isCurrent ? "&#70E000▶ &l" : "&#FF7EB3") + song.getTitle()));

                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.colorize("&7Artist: &#B2EBF2" + song.getAuthor()));
                lore.add(ColorUtil.colorize("&7Duration: &#FFD166" + formatSeconds(song.getDuration())));
                lore.add("");
                if (isCurrent) {
                    lore.add(ColorUtil.colorize("&#70E000♬ Currently Playing"));
                    meta.addEnchant(Enchantment.LUCK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(ColorUtil.colorize("&a▶ Click to Play"));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }

        // Bottom Control Dock (Slots 45 to 53)
        // 45: Previous Page
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pMeta = prev.getItemMeta();
            if (pMeta != null) {
                pMeta.setDisplayName(ColorUtil.colorize("&#B2EBF2◀ Previous Page"));
                prev.setItemMeta(pMeta);
            }
            inv.setItem(45, prev);
        }

        // 46: Previous Song
        ItemStack prevSong = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta psMeta = prevSong.getItemMeta();
        if (psMeta != null) {
            psMeta.setDisplayName(ColorUtil.colorize("&#B2EBF2⏮ Previous Song"));
            prevSong.setItemMeta(psMeta);
        }
        inv.setItem(46, prevSong);

        // 47: Pause / Resume
        boolean isPaused = (curSession != null && curSession.isPaused());
        ItemStack pauseItem = new ItemStack(isPaused ? Material.LIME_DYE : Material.REDSTONE);
        ItemMeta pauMeta = pauseItem.getItemMeta();
        if (pauMeta != null) {
            pauMeta.setDisplayName(ColorUtil.colorize(isPaused ? "&#70E000▶ Resume" : "&#FFD166⏸ Pause"));
            pauseItem.setItemMeta(pauMeta);
        }
        inv.setItem(47, pauseItem);

        // 48: Stop
        ItemStack stopItem = new ItemStack(Material.BARRIER);
        ItemMeta sMeta = stopItem.getItemMeta();
        if (sMeta != null) {
            sMeta.setDisplayName(ColorUtil.colorize("&#FF4B4B⏹ Stop Music"));
            stopItem.setItemMeta(sMeta);
        }
        inv.setItem(48, stopItem);

        // 49: Next Song
        ItemStack nextSong = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta nsMeta = nextSong.getItemMeta();
        if (nsMeta != null) {
            nsMeta.setDisplayName(ColorUtil.colorize("&#B2EBF2⏭ Next Song"));
            nextSong.setItemMeta(nsMeta);
        }
        inv.setItem(49, nextSong);

        // 50: Shuffle
        boolean shuffle = plugin.getPlayerDataManager().isMusicShuffle(player.getUniqueId());
        ItemStack shufItem = new ItemStack(shuffle ? Material.HOPPER : Material.CAULDRON);
        ItemMeta shMeta = shufItem.getItemMeta();
        if (shMeta != null) {
            shMeta.setDisplayName(ColorUtil.colorize("&#FFD166🔀 Shuffle: " + (shuffle ? "&aON" : "&cOFF")));
            shufItem.setItemMeta(shMeta);
        }
        inv.setItem(50, shufItem);

        // 51: Loop Mode
        String loopMode = plugin.getPlayerDataManager().getMusicLoop(player.getUniqueId());
        ItemStack loopItem = new ItemStack(Material.REPEATER);
        ItemMeta lMeta = loopItem.getItemMeta();
        if (lMeta != null) {
            String loopText = "Off";
            if ("ONE".equalsIgnoreCase(loopMode)) loopText = "Repeat One";
            else if ("ALL".equalsIgnoreCase(loopMode)) loopText = "Repeat All";
            lMeta.setDisplayName(ColorUtil.colorize("&#FF7EB3🔁 Loop: &a" + loopText));
            loopItem.setItemMeta(lMeta);
        }
        inv.setItem(51, loopItem);

        // 52: Volume
        int vol = plugin.getPlayerDataManager().getMusicVolume(player.getUniqueId());
        ItemStack volItem = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta vMeta = volItem.getItemMeta();
        if (vMeta != null) {
            vMeta.setDisplayName(ColorUtil.colorize("&#B2EBF2🔊 Volume: &#70E000" + vol + "%"));
            volItem.setItemMeta(vMeta);
        }
        inv.setItem(52, volItem);

        // 53: Next Page
        if (currentPage < maxPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nMeta = next.getItemMeta();
            if (nMeta != null) {
                nMeta.setDisplayName(ColorUtil.colorize("&#B2EBF2Next Page ▶"));
                next.setItemMeta(nMeta);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
        MessageUtil.playSuccess(player);
    }

    public Map<String, Song> getSongs() {
        return songs;
    }

    public Song getSong(String id) {
        return songs.get(id.toLowerCase());
    }

    public NoteBlockSong getNoteBlockSong(String id) {
        return noteBlockSongs.get(id.toLowerCase());
    }

    public SongPlayerSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public BloomeryRadio getRadio() {
        return radio;
    }

    public void cleanupPlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (radio != null) {
            radio.tuneOut(player);
        }
        stopMusicOnly(player);
    }

    public void stop() {
        if (radio != null) {
            radio.stop();
        }
        for (SongPlayerSession s : activeSessions.values()) {
            s.stop();
        }
        activeSessions.clear();
    }

    private String formatSeconds(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        return String.format("%02d:%02d", m, s);
    }
}
