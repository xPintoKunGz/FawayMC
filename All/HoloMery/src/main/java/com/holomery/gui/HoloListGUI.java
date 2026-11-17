package com.holomery.gui;

import com.holomery.color.ColorParser;
import com.holomery.manager.HologramManager;
import com.holomery.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual Hologram Browser & Selector GUI with Holder support.
 */
public class HoloListGUI {

    public static void open(Player player, int page) {
        List<Hologram> holos = new ArrayList<>(HologramManager.getInstance().getAllHolograms());
        int pageSize = 28;
        int maxPages = Math.max(1, (int) Math.ceil((double) holos.size() / pageSize));
        int safePage = Math.max(0, Math.min(page, maxPages - 1));

        String title = ColorParser.colorize("<gradient:#00F2FE:#4FACFE:#FF0080>&l✦ HoloMery Holograms ✦</gradient> &7(" + (safePage + 1) + "/" + maxPages + ")");
        HoloHolders.ListHolder holder = new HoloHolders.ListHolder(safePage);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        render(inv, safePage);
        player.openInventory(inv);
    }

    public static void render(Inventory inv, int page) {
        inv.clear();
        ItemStack border = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

        // Borders
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int r = 1; r < 5; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        List<Hologram> holos = new ArrayList<>(HologramManager.getInstance().getAllHolograms());
        int pageSize = 28;
        int maxPages = Math.max(1, (int) Math.ceil((double) holos.size() / pageSize));
        int safePage = Math.max(0, Math.min(page, maxPages - 1));

        int start = safePage * pageSize;
        int end = Math.min(start + pageSize, holos.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            while (slot % 9 == 0 || slot % 9 == 8) slot++;
            Hologram holo = holos.get(i);

            ItemStack item = new GuiItemBuilder(Material.BEACON)
                    .name("<gradient:#00F2FE:#FF0080>&l" + holo.getId() + "</gradient>")
                    .lore(
                            "&7Location: &f" + (holo.getLocation().getWorld() != null ? holo.getLocation().getWorld().getName() : "world") +
                                    " &8[" + (int) holo.getLocation().getX() + ", " + (int) holo.getLocation().getY() + ", " + (int) holo.getLocation().getZ() + "]",
                            "&7Pages: &b" + holo.getPageCount() + " &8| &7Lines: &b" + holo.getLineCount(),
                            "&7Billboard: &b" + holo.getBillboard().name() + " &8| &7Scale: &b" + holo.getScale(),
                            "",
                            "&a▶ Left-Click to &eOpen Editor",
                            "&b▶ Right-Click to &eTeleport",
                            "&c▶ Shift-Right-Click to &cDelete"
                    )
                    .data("holo_id", holo.getId())
                    .glow(true)
                    .build();

            inv.setItem(slot++, item);
        }

        // Navigation controls
        if (safePage > 0) {
            inv.setItem(45, new GuiItemBuilder(Material.ARROW)
                    .name("&b◀ Previous Page")
                    .lore("&7Go to Page " + safePage)
                    .data("action", "prev_page")
                    .build());
        }
        if (safePage < maxPages - 1) {
            inv.setItem(53, new GuiItemBuilder(Material.ARROW)
                    .name("&bNext Page ▶")
                    .lore("&7Go to Page " + (safePage + 2))
                    .data("action", "next_page")
                    .build());
        }

        inv.setItem(49, new GuiItemBuilder(Material.NETHER_STAR)
                .name("<#00FF87>&l+ Create New Hologram")
                .lore("&7Click to create a new hologram at your current eye position!")
                .data("action", "create_holo")
                .glow(true)
                .build());

        inv.setItem(48, new GuiItemBuilder(Material.BARRIER)
                .name("&cClose Menu")
                .data("action", "close")
                .build());
    }
}
