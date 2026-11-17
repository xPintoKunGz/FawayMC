package com.bloomerymc.bloomeryx.listeners;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.manager.MusicManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MusicListener implements Listener {

    private final BloomeryX plugin;

    public MusicListener(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJukeboxBlockClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.JUKEBOX) return;

        if (!plugin.getConfig().getBoolean("music.jukebox-block-click", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("bloomeryx.command.music")) return;

        event.setCancelled(true);
        plugin.getMusicManager().openJukeboxGui(player, 0);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("BloomeryX Music")) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            int page = 0;
            if (title.contains("Page ")) {
                try {
                    String pageStr = title.substring(title.indexOf("Page ") + 5).trim();
                    page = Math.max(0, Integer.parseInt(pageStr) - 1);
                } catch (Exception ignored) {}
            }

            int slot = event.getRawSlot();

            // 45: Previous Page
            if (slot == 45) {
                if (page > 0) {
                    plugin.getMusicManager().openJukeboxGui(player, page - 1);
                }
                return;
            }

            // 46: Previous Song
            if (slot == 46) {
                plugin.getMusicManager().previousSong(player);
                plugin.getMusicManager().openJukeboxGui(player, page);
                return;
            }

            // 47: Pause / Resume
            if (slot == 47) {
                plugin.getMusicManager().togglePauseResume(player);
                plugin.getMusicManager().openJukeboxGui(player, page);
                return;
            }

            // 48: Stop
            if (slot == 48) {
                plugin.getMusicManager().muteAndStopMusic(player);
                plugin.getMusicManager().openJukeboxGui(player, page);
                return;
            }

            // 49: Next Song
            if (slot == 49) {
                plugin.getMusicManager().nextSong(player);
                plugin.getMusicManager().openJukeboxGui(player, page);
                return;
            }

            // 50: Shuffle Toggle
            if (slot == 50) {
                plugin.getMusicManager().toggleShuffle(player);
                plugin.getMusicManager().openJukeboxGui(player, page);
                return;
            }

            // 51: Loop Mode Cycle
            if (slot == 51) {
                plugin.getMusicManager().toggleLoop(player);
                plugin.getMusicManager().openJukeboxGui(player, page);
                return;
            }

            // 52: Volume Cycle
            if (slot == 52) {
                plugin.getMusicManager().cycleVolume(player);
                plugin.getMusicManager().openJukeboxGui(player, page);
                return;
            }

            // 53: Next Page
            if (slot == 53) {
                plugin.getMusicManager().openJukeboxGui(player, page + 1);
                return;
            }

            // 0-44: Song Click
            if (slot >= 0 && slot < 45) {
                ItemStack item = event.getCurrentItem();
                if (item != null && item.hasItemMeta()) {
                    List<MusicManager.Song> songList = new ArrayList<>(plugin.getMusicManager().getSongs().values());
                    int songIndex = (page * 45) + slot;
                    if (songIndex < songList.size()) {
                        MusicManager.Song song = songList.get(songIndex);
                        plugin.getMusicManager().playSong(player, song);
                        plugin.getMusicManager().openJukeboxGui(player, page);
                    }
                }
            }
        }
    }
}
