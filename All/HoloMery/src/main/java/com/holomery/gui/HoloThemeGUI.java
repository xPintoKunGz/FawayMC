package com.holomery.gui;

import com.holomery.color.ColorParser;
import com.holomery.manager.ThemeManager;
import com.holomery.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1-Click Gradient Theme Gallery GUI with Holder support.
 */
public class HoloThemeGUI {

    public static void open(Player player, Hologram holo) {
        if (holo == null) return;

        String title = ColorParser.colorize("<gradient:#00F2FE:#FF0080>&l✦ Select Theme: </gradient>&b" + holo.getId());
        HoloHolders.ThemeHolder holder = new HoloHolders.ThemeHolder(holo);
        Inventory inv = Bukkit.createInventory(holder, 36, title);
        holder.setInventory(inv);

        ItemStack border = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);

        List<String> themeNames = new ArrayList<>(ThemeManager.getInstance().getThemeNames());
        int slot = 9;

        for (String theme : themeNames) {
            if (slot >= 27) break;
            Material mat = getGlassForTheme(theme);
            String coloredSample = ColorParser.colorize("<" + theme + ">&l✦ " + theme.toUpperCase() + " PALETTE ✦</" + theme + ">");

            ItemStack item = new GuiItemBuilder(mat)
                    .name(coloredSample)
                    .lore(
                            "&7Click to apply this theme to",
                            "&7all lines on the current page!",
                            "",
                            "&a▶ Click to Apply"
                    )
                    .data("theme_name", theme)
                    .glow(true)
                    .build();

            inv.setItem(slot++, item);
        }

        inv.setItem(31, new GuiItemBuilder(Material.ARROW)
                .name("&e◀ Back to Editor")
                .data("action", "back_to_editor")
                .build());

        player.openInventory(inv);
    }

    private static Material getGlassForTheme(String theme) {
        return switch (theme.toLowerCase()) {
            case "cyberpunk" -> Material.MAGENTA_STAINED_GLASS;
            case "aurora" -> Material.LIME_STAINED_GLASS;
            case "sunset" -> Material.ORANGE_STAINED_GLASS;
            case "neon" -> Material.GREEN_STAINED_GLASS;
            case "sakura", "candy" -> Material.PINK_STAINED_GLASS;
            case "amethyst" -> Material.PURPLE_STAINED_GLASS;
            case "galaxy", "ocean" -> Material.BLUE_STAINED_GLASS;
            case "fire" -> Material.RED_STAINED_GLASS;
            case "ice" -> Material.LIGHT_BLUE_STAINED_GLASS;
            case "gold" -> Material.YELLOW_STAINED_GLASS;
            default -> Material.CYAN_STAINED_GLASS;
        };
    }
}
