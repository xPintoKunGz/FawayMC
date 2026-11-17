package com.holomery.gui;

import com.holomery.color.ColorParser;
import com.holomery.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Line Type Selector GUI with Holder support.
 */
public class HoloAddLineGUI {

    public static void open(Player player, Hologram holo) {
        if (holo == null) return;

        String title = ColorParser.colorize("<gradient:#00F2FE:#FF0080>&l✦ Add Line to: </gradient>&b" + holo.getId());
        HoloHolders.AddLineHolder holder = new HoloHolders.AddLineHolder(holo);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        ItemStack border = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 36; i < 45; i++) inv.setItem(i, border);

        // 1. Text Line
        inv.setItem(19, new GuiItemBuilder(Material.WRITABLE_BOOK)
                .name("<gradient:#00F2FE:#4FACFE>&l✦ Custom Text Line ✦</gradient>")
                .lore(
                        "&7Add custom formatted text with",
                        "&7MiniMessage, Hex, or 15+ gradient themes!",
                        "",
                        "&a▶ Click to type in chat"
                )
                .data("type", "text")
                .glow(true)
                .build());

        // 2. Floating Item
        inv.setItem(21, new GuiItemBuilder(Material.NETHER_STAR)
                .name("<gradient:#FA709A:#FEE140>&l✦ Floating Item (ItemDisplay) ✦</gradient>")
                .lore(
                        "&7Add a floating 3D item!",
                        "&7Defaults: Nether Star, Diamond Sword, Elytra...",
                        "",
                        "&a▶ Left-Click: Add your currently held item",
                        "&b▶ Right-Click: Add Nether Star"
                )
                .data("type", "item")
                .glow(true)
                .build());

        // 3. Floating Block
        inv.setItem(23, new GuiItemBuilder(Material.BEACON)
                .name("<gradient:#00FF87:#60EFFF>&l✦ Floating Block (BlockDisplay) ✦</gradient>")
                .lore(
                        "&7Add a floating 3D block!",
                        "&7Defaults: Beacon, Netherite Block, Dragon Egg...",
                        "",
                        "&a▶ Left-Click: Add your currently held block",
                        "&b▶ Right-Click: Add Beacon"
                )
                .data("type", "block")
                .glow(true)
                .build());

        // 4. Floating Mob / Monster
        inv.setItem(25, new GuiItemBuilder(Material.ALLAY_SPAWN_EGG)
                .name("<gradient:#8A2387:#E94057:#F27121>&l✦ Floating Mob / Creature ✦</gradient>")
                .lore(
                        "&7Add a floating creature or monster!",
                        "&7No AI, completely silent, invulnerable & decorative.",
                        "",
                        "&a▶ Left-Click: Add Allay",
                        "&b▶ Right-Click: Add Blaze",
                        "&6▶ Shift-Click: Add Warden"
                )
                .data("type", "mob")
                .glow(true)
                .build());

        inv.setItem(40, new GuiItemBuilder(Material.ARROW)
                .name("&e◀ Back to Editor")
                .data("action", "back_to_editor")
                .build());

        player.openInventory(inv);
    }
}
