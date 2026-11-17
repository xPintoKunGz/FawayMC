package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.replay.ReplayData;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReplayGUI {

    public static final String GUI_TITLE = "§8✦ §6§lMATCH REPLAYS §8(Past 7 Days)";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Map to link open inventories to replay lists
    private static final Map<UUID, List<ReplayData>> cachedPlayerReplays = new HashMap<>();

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Border glass
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);
        inv.setItem(27, border);
        inv.setItem(35, border);
        inv.setItem(36, border);
        inv.setItem(44, border);

        // Info icon
        inv.setItem(4, new ItemBuilder(Material.CLOCK)
                .name("§6§l✦ Match History & Replays ✦")
                .lore(
                        "§7Browse all your recorded Bedwars matches.",
                        "§7Replay playback tracks movement, kills, and bed breaks.",
                        "",
                        "§c⏳ Replays expire automatically after 7 days!"
                ).build());

        // Close button
        inv.setItem(49, new ItemBuilder(Material.BARRIER).name("§cClose Menu").build());

        // Loading placeholder in center
        inv.setItem(22, new ItemBuilder(Material.COMPASS).name("§eLoading matches...").build());
        player.openInventory(inv);

        // Fetch replays asynchronously so we never lag the main server thread!
        Bukkit.getScheduler().runTaskAsynchronously(BedwarsPlugin.getInstance(), () -> {
            List<ReplayData> replays = BedwarsPlugin.getInstance().getReplayManager().getPlayerReplays(player.getUniqueId());

            Bukkit.getScheduler().runTask(BedwarsPlugin.getInstance(), () -> {
                if (!player.isOnline() || !player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
                    return;
                }

                cachedPlayerReplays.put(player.getUniqueId(), replays);
                inv.setItem(22, new ItemBuilder(Material.AIR).build());

                if (replays.isEmpty()) {
                    inv.setItem(22, new ItemBuilder(Material.BARRIER)
                            .name("§cNo Matches Recorded Yet")
                            .lore(
                                    "§7No recorded matches found for your account",
                                    "§7within the last 7 days.",
                                    "",
                                    "§ePlay a full match to record a replay!"
                            ).build());
                    return;
                }

                int[] allowedSlots = {
                        10, 11, 12, 13, 14, 15, 16,
                        19, 20, 21, 22, 23, 24, 25,
                        28, 29, 30, 31, 32, 33, 34,
                        37, 38, 39, 40, 41, 42, 43
                };

                for (int i = 0; i < replays.size() && i < allowedSlots.length; i++) {
                    ReplayData data = replays.get(i);
                    int minutes = data.getDurationSeconds() / 60;
                    int seconds = data.getDurationSeconds() % 60;
                    String durationStr = String.format("%02d:%02d", minutes, seconds);
                    String dateStr = DATE_FORMAT.format(new Date(data.getTimestamp()));

                    long ageMs = System.currentTimeMillis() - data.getTimestamp();
                    long daysLeft = Math.max(0, 7 - (ageMs / (24L * 60 * 60 * 1000L)));

                    List<String> lore = new ArrayList<>();
                    lore.add("§8Match ID: §7" + data.getReplayId().substring(0, Math.min(8, data.getReplayId().length())));
                    lore.add("§7Date: §f" + dateStr);
                    lore.add("§7Duration: §f" + durationStr);
                    lore.add("§7Winner: §a" + (data.getWinnerTeam() != null ? data.getWinnerTeam() : "Draw"));
                    lore.add("§7Participants: §b" + data.getPlayerNames().size() + " players");
                    lore.add("§7Events: §e" + data.getEvents().size() + " recorded");
                    lore.add("");
                    lore.add("§c⏳ Expiring in: §e" + daysLeft + " day(s)");
                    lore.add("");
                    lore.add("§a▶ Left-Click to start Replay Playback!");
                    lore.add("§c✖ Right-Click to Delete this Replay!");

                    inv.setItem(allowedSlots[i], new ItemBuilder(Material.FILLED_MAP)
                            .name("§6§l" + data.getArenaName() + " §7- §f" + dateStr)
                            .lore(lore)
                            .build());
                }
            });
        });
    }

    public static List<ReplayData> getCachedReplays(UUID uuid) {
        return cachedPlayerReplays.get(uuid);
    }

    public static void clearCache(UUID uuid) {
        cachedPlayerReplays.remove(uuid);
    }
}

