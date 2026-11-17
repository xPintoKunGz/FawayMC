package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.spawner.Spawner;
import com.bedwars.arena.spawner.SpawnerType;
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

public class CenterSpawnerGUI {

    public static final String TITLE_PREFIX = "§8✦ §6§lCenter Spawners Setup §8» §e";

    public static void open(Player player, Arena arena) {
        String title = TITLE_PREFIX + arena.getName();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        inv.setItem(4, new ItemBuilder(Material.BEACON)
                .name("§b§l✦ Setup Diamond & Emerald Spawners ✦")
                .lore(
                        "§7Stand on a Diamond island or Middle island",
                        "§7and click below to place a spawner at your location!",
                        "§7Current Spawners: §e" + arena.getSpawners().size() + " §7nodes"
                ).build());

        // 1. Add Diamond Spawner
        inv.setItem(11, new ItemBuilder(Material.DIAMOND_BLOCK)
                .name("§b§l➕ Add Diamond Spawner")
                .lore(
                        "§7Stand on the Diamond Island",
                        "§7and click here to spawn diamonds at your exact spot!",
                        "",
                        "§e▶ Click to add Diamond Spawner at your location"
                ).build());

        // 2. Add Emerald Spawner
        inv.setItem(15, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("§a§l➕ Add Emerald Spawner")
                .lore(
                        "§7Stand on the Middle Emerald Island",
                        "§7and click here to spawn emeralds at your exact spot!",
                        "",
                        "§e▶ Click to add Emerald Spawner at your location"
                ).build());

        // Configured Spawners Cards (Slots 18 to 35)
        int slot = 18;
        for (int i = 0; i < arena.getSpawners().size(); i++) {
            Spawner sp = arena.getSpawners().get(i);
            if (sp.getType() != SpawnerType.DIAMOND && sp.getType() != SpawnerType.EMERALD) continue;
            if (slot > 35) break;

            boolean isDia = sp.getType() == SpawnerType.DIAMOND;
            Material icon = isDia ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
            String name = (isDia ? "§b§lDiamond Spawner #" : "§a§lEmerald Spawner #") + (i + 1);
            org.bukkit.Location loc = sp.getLocation();
            String coords = (loc != null)
                    ? "§fX: §e" + loc.getBlockX() + " §7Y: §e" + loc.getBlockY() + " §7Z: §e" + loc.getBlockZ()
                    : "§cUnknown";

            inv.setItem(slot, new ItemBuilder(icon)
                    .name(name)
                    .lore(
                            "§7Type: §f" + sp.getType().getDisplayName(),
                            "§7Location: " + coords,
                            "§7World: §f" + (loc != null && loc.getWorld() != null ? loc.getWorld().getName() : "Unknown"),
                            "§8────────────────────",
                            "§a▶ Left-Click: §fTeleport to this spawner",
                            "§e▶ Shift-Left / Middle: §fRelocate here",
                            "§c▶ Right-Click: §fDelete / Remove spawner"
                    ).build());
            slot++;
        }

        // 3. Clear Center Spawners
        inv.setItem(40, new ItemBuilder(Material.BARRIER)
                .name("§c§lClear All Center Spawners")
                .lore(
                        "§7Click if you wish to reset and remove all center spawners",
                        "§7to re-configure them from scratch."
                ).build());

        // Back button
        inv.setItem(49, new ItemBuilder(Material.ARROW)
                .name("§c§lBack to Arena Setup")
                .build());

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
