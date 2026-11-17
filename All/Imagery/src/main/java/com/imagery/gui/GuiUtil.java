package com.imagery.gui;

import com.imagery.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper to build aesthetic Style 26.3 inventory items.
 */
public final class GuiUtil {

    private GuiUtil() {}

    public static ItemStack createItem(Material material, String displayName, String... lore) {
        return createItem(material, 1, displayName, Arrays.asList(lore));
    }

    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        return createItem(material, 1, displayName, lore);
    }

    public static ItemStack createItem(Material material, int amount, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(ColorUtil.color(displayName));
            }
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ColorUtil.color(line));
                }
                meta.setLore(coloredLore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Applies the Style 26.3 frame border using Cyan, Tinted, and Dark glass panes.
     */
    public static void fillBorders(Inventory inv) {
        int size = inv.getSize();
        int rows = size / 9;

        ItemStack cyanGlass = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        ItemStack blueGlass = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        ItemStack blackGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        // Top and bottom rows
        for (int c = 0; c < 9; c++) {
            ItemStack glass = (c == 0 || c == 8) ? blueGlass : cyanGlass;
            inv.setItem(c, glass);
            inv.setItem(size - 9 + c, glass);
        }

        // Side columns
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * 9, blackGlass);
            inv.setItem(r * 9 + 8, blackGlass);
        }
    }
}

