package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaManager;
import com.bedwars.arena.ArenaState;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArenaSelectorGUI {

    public static final String GUI_TITLE_PREFIX = "§8✦ §6§lBEDWARS §8| ";

    public enum ModeCategory {
        ALL("All Modes", Material.COMPASS, 0),
        SOLO("Solo (1xTeam)", Material.IRON_SWORD, 1),
        DOUBLES("Doubles (2xTeam)", Material.GOLDEN_SWORD, 2),
        TRIOS("3v3v3v3 (3xTeam)", Material.DIAMOND_SWORD, 3),
        FOURS("4v4v4v4 (4xTeam)", Material.NETHERITE_SWORD, 4);

        private final String displayName;
        private final Material icon;
        private final int slot;

        ModeCategory(String displayName, Material icon, int slot) {
            this.displayName = displayName;
            this.icon = icon;
            this.slot = slot;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public int getSlot() {
            return slot;
        }
    }

    public static void open(Player player) {
        open(player, ModeCategory.ALL);
    }

    public static void open(Player player, ModeCategory currentCategory) {
        if (currentCategory == null) currentCategory = ModeCategory.ALL;
        Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE_PREFIX + currentCategory.getDisplayName());
        ArenaManager am = BedwarsPlugin.getInstance().getArenaManager();

        // Row 0: Category Filter Tabs
        for (ModeCategory cat : ModeCategory.values()) {
            boolean isCurrent = (cat == currentCategory);
            ItemBuilder ib = new ItemBuilder(cat.getIcon())
                    .name((isCurrent ? "§a§l▶ " : "§e") + cat.getDisplayName())
                    .lore(
                            "§7Filter arenas for this game mode",
                            "",
                            isCurrent ? "§a✔ Currently Selected" : "§e▶ Click to filter"
                    );
            if (isCurrent) {
                ib.enchant(Enchantment.LUCK_OF_THE_SEA, 1);
                ib.flags(ItemFlag.HIDE_ENCHANTS);
            }
            inv.setItem(cat.getSlot(), ib.build());
        }

        // Slot 8: Quick Play button
        inv.setItem(8, new ItemBuilder(Material.NETHER_STAR)
                .name("§6§l✦ Quick Play ✦")
                .lore(
                        "§7Click to instantly join the best available arena!",
                        "",
                        "§e▶ Click to play now!"
                ).build());

        // Fillers for edges
        ItemStack blackGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        inv.setItem(5, blackGlass);
        inv.setItem(6, blackGlass);
        inv.setItem(7, blackGlass);

        for (int i = 36; i < 45; i++) inv.setItem(i, blackGlass);
        inv.setItem(9, grayGlass);
        inv.setItem(17, grayGlass);
        inv.setItem(18, grayGlass);
        inv.setItem(26, grayGlass);
        inv.setItem(27, grayGlass);
        inv.setItem(35, grayGlass);

        // Fill Arenas matching category
        int slot = 10;
        int displayedCount = 0;
        for (Arena arena : am.getArenas()) {
            if (!arena.isEnabled()) continue;
            if (!matchesCategory(arena, currentCategory)) continue;

            if (slot >= 35) break;
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27) slot += 2;

            Material iconMat;
            String statusText;
            String actionText;

            if (arena.getState() == ArenaState.WAITING) {
                iconMat = Material.LIME_CONCRETE;
                statusText = "§a● Waiting for players...";
                actionText = "§e▶ Click to join";
            } else if (arena.getState() == ArenaState.STARTING) {
                iconMat = Material.YELLOW_CONCRETE;
                statusText = "§e● Starting soon (" + arena.getCountdown() + "s)";
                actionText = "§e▶ Click to join";
            } else if (arena.getState() == ArenaState.IN_GAME) {
                iconMat = Material.RED_CONCRETE;
                statusText = "§c● In Game";
                actionText = "§7▶ Click to spectate";
            } else {
                iconMat = Material.GRAY_CONCRETE;
                statusText = "§8● Resetting...";
                actionText = "§8Unavailable";
            }

            int teamCount = Math.max(1, arena.getTeams().size());
            int ppt = (arena.getPlayersPerTeam() > 0) ? arena.getPlayersPerTeam() : Math.max(1, arena.getMaxPlayers() / teamCount);
            String formatStr = teamCount + " Teams x " + ppt + " (" + teamCount + "x" + ppt + ")";

            List<String> lore = new ArrayList<>();
            lore.add("§8────────────────────");
            lore.add("§7Map: §f" + MessageUtil.color(arena.getDisplayName()));
            lore.add("§7ID: §8" + arena.getName());
            lore.add("§7Mode: §e" + arena.getMode() + " §7(" + formatStr + ")");
            lore.add("§7Players: §b" + arena.getPlayers().size() + "§8/§b" + arena.getMaxPlayers());
            lore.add("§7Status: " + statusText);
            lore.add("§8────────────────────");
            lore.add(actionText);
            lore.add("§b▶ Right-Click for Bot Practice (ซ้อมกับบอท)");

            ItemStack arenaItem = new ItemBuilder(iconMat)
                    .name("§e§l" + MessageUtil.color(arena.getDisplayName()))
                    .lore(lore)
                    .build();

            inv.setItem(slot++, arenaItem);
            displayedCount++;
        }

        if (displayedCount == 0) {
            inv.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("§c§lNo Arenas Found")
                    .lore("§7No active arenas match the " + currentCategory.getDisplayName() + " filter.",
                          "§eClick 'All Modes' to view all arenas.")
                    .build());
        }

        // Close button
        inv.setItem(40, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1.2f);
    }

    public static boolean matchesCategory(Arena arena, ModeCategory category) {
        if (category == ModeCategory.ALL) return true;

        int teamCount = Math.max(1, arena.getTeams().size());
        int ppt = (arena.getPlayersPerTeam() > 0) ? arena.getPlayersPerTeam() : Math.max(1, arena.getMaxPlayers() / teamCount);
        String modeName = (arena.getMode() != null) ? arena.getMode().toLowerCase() : "";

        return switch (category) {
            case SOLO -> ppt == 1 || modeName.contains("solo") || modeName.contains("1v1") || modeName.contains("1x");
            case DOUBLES -> ppt == 2 || modeName.contains("double") || modeName.contains("duo") || modeName.contains("2v2") || modeName.contains("2x");
            case TRIOS -> ppt == 3 || modeName.contains("trio") || modeName.contains("3v3") || modeName.contains("3x");
            case FOURS -> ppt == 4 || modeName.contains("four") || modeName.contains("4v4") || modeName.contains("4x");
            default -> true;
        };
    }
}
