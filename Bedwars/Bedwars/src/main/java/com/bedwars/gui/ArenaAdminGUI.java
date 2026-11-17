package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaManager;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArenaAdminGUI {

    public static final String ADMIN_TITLE = "§8✦ §6§lMap Manager §8✦";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ADMIN_TITLE);
        ArenaManager am = BedwarsPlugin.getInstance().getArenaManager();

        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Header
        inv.setItem(4, new ItemBuilder(Material.BOOKSHELF)
                .name("§6§l✦ Server Arenas & Maps ✦")
                .lore(
                        "§7Manage, configure, and rename Bedwars arenas",
                        "§7Total Arenas: §e" + am.getArenas().size()
                ).build());

        // Fill Arenas
        int slot = 9;
        for (Arena arena : am.getArenas()) {
            if (slot >= 45) break;

            boolean ready = am.isArenaReady(arena);
            Material mat = arena.isEnabled() ? (ready ? Material.MAP : Material.PAPER) : Material.BARRIER;

            List<String> lore = new ArrayList<>();
            lore.add("§8────────────────────");
            lore.add("§7Map ID: §f" + arena.getName());
            lore.add("§7Display Name: §e" + MessageUtil.color(arena.getDisplayName()));
            lore.add("§7Mode: §b" + arena.getMode() + " §7(" + arena.getPlayersPerTeam() + " players/team)");
            lore.add("§7Teams: §a" + arena.getTeams().size() + " §7colors configured");
            lore.add("§7Center Spawners: §e" + arena.getSpawners().size() + " §7nodes");
            lore.add("§7Configuration: " + (ready ? "§a✔ 100% Complete" : "§c✖ Incomplete Setup"));
            lore.add("§7Status: " + (arena.isEnabled() ? "§aEnabled" : "§cDisabled"));
            lore.add("§8────────────────────");
            lore.add("§e▶ Left-Click: §fEdit / Setup Map");
            lore.add("§b▶ Right-Click: §fTeleport to Map");
            lore.add("§c▶ Shift + Right-Click: §fToggle Enable/Disable");

            ItemStack card = new ItemBuilder(mat)
                    .name("§e§lArena: " + MessageUtil.color(arena.getDisplayName()))
                    .lore(lore)
                    .build();

            inv.setItem(slot++, card);
        }

        // Add New Map Button
        inv.setItem(49, new ItemBuilder(Material.NETHER_STAR)
                .name("§a§l➕ Create New Arena")
                .lore(
                        "§7Click to view setup instructions",
                        "§7Or use command: §e/bw setup <name>"
                ).build());

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
