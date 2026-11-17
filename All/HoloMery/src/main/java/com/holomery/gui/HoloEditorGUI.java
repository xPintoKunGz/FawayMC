package com.holomery.gui;

import com.holomery.color.ColorParser;
import com.holomery.model.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Feature-packed in-game Hologram Editor GUI with smooth in-place rendering.
 */
public class HoloEditorGUI {

    public static void open(Player player, Hologram holo) {
        if (holo == null) return;

        String title = ColorParser.colorize("<gradient:#00F2FE:#FF0080>&l✦ Edit: </gradient>&b" + holo.getId());
        HoloHolders.EditHolder holder = new HoloHolders.EditHolder(holo);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        render(inv, holo);
        player.openInventory(inv);
    }

    public static void render(Inventory inv, Hologram holo) {
        inv.clear();
        ItemStack border = new GuiItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();

        // Fill background borders
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int r = 1; r < 5; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        // --- TOP ROW: Page Controls & Info ---
        inv.setItem(0, new GuiItemBuilder(Material.BOOK)
                .name("<#00F2FE>&l✦ Hologram: &b" + holo.getId())
                .lore(
                        "&7World: &f" + (holo.getLocation().getWorld() != null ? holo.getLocation().getWorld().getName() : "world"),
                        "&7Coords: &f" + String.format("%.1f, %.1f, %.1f", holo.getLocation().getX(), holo.getLocation().getY(), holo.getLocation().getZ()),
                        "&7Total Pages: &b" + holo.getPageCount(),
                        "&7Current Page: &b" + (holo.getActivePageIndex() + 1),
                        "&7Lines on Page: &b" + holo.getLineCount()
                )
                .build());

        // Slot 1: Click Actions & Commands on this page
        java.util.List<String> actions = holo.getClickActions();
        java.util.List<String> actionLore = new java.util.ArrayList<>();
        actionLore.add("&7Configured Actions for Page " + (holo.getActivePageIndex() + 1) + ":");
        if (actions.isEmpty()) {
            actionLore.add("&8  (None - flips to next page by default)");
        } else {
            for (int i = 0; i < actions.size(); i++) {
                actionLore.add("&8  #" + (i + 1) + " &b" + actions.get(i));
            }
        }
        actionLore.add("");
        actionLore.add("&a▶ Click: &eManage Click Commands & Page Jumps");

        inv.setItem(1, new GuiItemBuilder(Material.COMMAND_BLOCK)
                .name("<gradient:#FF0080:#FA709A>&l⚡ Click Actions (" + actions.size() + ")</gradient>")
                .lore(actionLore)
                .data("control", "click_actions")
                .glow(!actions.isEmpty())
                .build());

        // Previous page button
        inv.setItem(2, new GuiItemBuilder(Material.SPECTRAL_ARROW)
                .name("<#00FF87>◀ Previous Page")
                .lore("&7Click to switch to page " + (holo.getActivePageIndex() == 0 ? holo.getPageCount() : holo.getActivePageIndex()))
                .data("control", "page_prev")
                .build());

        // Current Page indicator & page management
        inv.setItem(4, new GuiItemBuilder(Material.WRITABLE_BOOK)
                .name("<gradient:#FA709A:#FEE140>&lPage " + (holo.getActivePageIndex() + 1) + " of " + holo.getPageCount() + "</gradient>")
                .lore(
                        "&7Click-To-Flip: &b" + (holo.isClickPageSwitch() ? "&aENABLED" : "&cDISABLED"),
                        "&7Auto-Cycle Timer: &b" + (holo.getAutoPageIntervalSeconds() > 0 ? holo.getAutoPageIntervalSeconds() + "s" : "&7Off"),
                        "",
                        "&a▶ Left-Click: &eToggle Click-to-Flip",
                        "&b▶ Right-Click: &eCycle Auto-Timer (Off/3s/5s/10s)",
                        "&6▶ Shift-Left-Click: &a+ Add New Page",
                        "&c▶ Shift-Right-Click: &c- Delete This Page"
                )
                .data("control", "page_manage")
                .glow(true)
                .build());

        // Next page button
        inv.setItem(6, new GuiItemBuilder(Material.SPECTRAL_ARROW)
                .name("<#00FF87>Next Page ▶")
                .lore("&7Click to switch to page " + ((holo.getActivePageIndex() + 1) % holo.getPageCount() + 1))
                .data("control", "page_next")
                .build());

        // Spin 360 Animation toggle
        inv.setItem(7, new GuiItemBuilder(Material.BLAZE_ROD)
                .name("<#00FF87>&l🔄 Spin 360° Animation: " + (holo.isSpin() ? "&aON (หมุนรอบ)" : "&7OFF (ตามผู้เล่น)"))
                .lore(
                        "&7Toggle continuous 360-degree rotation animation",
                        "&7for all floating items, blocks & creatures!",
                        "",
                        "&a▶ Click to Toggle"
                )
                .data("control", "spin")
                .glow(holo.isSpin())
                .build());

        // Teleport button
        inv.setItem(8, new GuiItemBuilder(Material.ENDER_PEARL)
                .name("<#00F2FE>Teleport to Hologram")
                .lore("&7Click to warp directly to this hologram")
                .data("control", "teleport")
                .build());

        // --- MIDDLE: Active Page Lines (Slots 10-16, 19-25, 28-34) ---
        HoloPage activePage = holo.getActivePage();
        int slot = 10;
        int lineIdx = 0;

        for (HoloLine line : activePage.getLines()) {
            while (slot % 9 == 0 || slot % 9 == 8) slot++;
            if (slot > 34) break;

            Material iconMat = getMaterialForLine(line);
            String preview = ColorParser.colorize(line.getRawText());

            ItemStack lineItem = new GuiItemBuilder(iconMat)
                    .name("&bLine #" + (lineIdx + 1) + " &7[" + line.getType().name() + "]")
                    .lore(
                            "&7Preview: &r" + preview,
                            "&8Raw: &7" + line.getRawText(),
                            "",
                            "&a▶ Left-Click: &eEdit Content",
                            "&b▶ Shift-Left-Click: &b▲ Move Up",
                            "&6▶ Shift-Right-Click: &6▼ Move Down",
                            "&c▶ Right-Click: &cDelete Line"
                    )
                    .data("line_index", String.valueOf(lineIdx))
                    .build();

            inv.setItem(slot++, lineItem);
            lineIdx++;
        }

        // Add Line Button
        if (slot <= 34) {
            while (slot % 9 == 0 || slot % 9 == 8) slot++;
            if (slot <= 34) {
                inv.setItem(slot, new GuiItemBuilder(Material.EMERALD)
                        .name("<#00FF87>&l+ Add New Line")
                        .lore(
                                "&7Click to choose line type:",
                                "&8 ▪ &fText / Gradients",
                                "&8 ▪ &bFloating Item (ITEM:NAME)",
                                "&8 ▪ &eFloating Block (BLOCK:NAME)",
                                "&8 ▪ &dFloating Mob (MOB:NAME)"
                        )
                        .data("control", "add_line")
                        .glow(true)
                        .build());
            }
        }

        // --- BOTTOM ROW: Customization & Controls (Slots 45-53) ---
        // 45: Billboard
        inv.setItem(45, new GuiItemBuilder(Material.COMPASS)
                .name("<#00F2FE>Billboard: &b" + holo.getBillboard().name())
                .lore("&7Click to cycle: &fCENTER ➔ FIXED ➔ VERTICAL ➔ HORIZONTAL")
                .data("control", "billboard")
                .build());

        // 46: Scale
        inv.setItem(46, new GuiItemBuilder(Material.SLIME_BALL)
                .name("<#00F2FE>Scale: &b" + String.format("%.1fx", holo.getScale()))
                .lore(
                        "&7Adjust hologram text/item scale:",
                        "&a▶ Left-Click: &a+0.2x",
                        "&c▶ Right-Click: &c-0.2x"
                )
                .data("control", "scale")
                .build());

        // 47: Themes Palette
        inv.setItem(47, new GuiItemBuilder(Material.DRAGON_BREATH)
                .name("<gradient:#FF0080:#7928CA:#00F2FE>&l🎨 Themes Palette</gradient>")
                .lore("&7Click to open 1-click gradient theme gallery!")
                .data("control", "theme_palette")
                .glow(true)
                .build());

        // 48: Background (Black Box Toggle)
        boolean isBlackBg = !holo.isTransparentBackground();
        Material bgIcon = isBlackBg ? Material.BLACK_CONCRETE : Material.GLASS;
        String bgName = isBlackBg ? "<#FF5555>&l⬛ พื้นหลังสีดำ: &aเปิด (ON)" : "<#00FF87>&l⬜ พื้นหลังสีดำ: &cปิด (โปร่งใส)";
        inv.setItem(48, new GuiItemBuilder(bgIcon)
                .name(bgName)
                .lore(
                        "&7เปิด/ปิดกล่องสี่เหลี่ยมสีดำด้านหลังตัวหนังสือ",
                        isBlackBg ? "&8 ▪ &fสถานะ: &eมีพื้นหลังสีดำ (Black Box)" : "&8 ▪ &fสถานะ: &bโปร่งใส ไร้พื้นหลังดำ (Transparent)",
                        "",
                        "&a▶ คลิกเพื่อ: &f" + (isBlackBg ? "ปิดพื้นหลังดำ (ให้โปร่งใส)" : "เปิดพื้นหลังดำ")
                )
                .data("control", "background")
                .glow(isBlackBg)
                .build());

        // 49: See-Through
        inv.setItem(49, new GuiItemBuilder(Material.ENDER_EYE)
                .name("<#00F2FE>See-Through Walls: &b" + (holo.isSeeThrough() ? "&aON" : "&cOFF"))
                .lore("&7Click to toggle visibility through solid blocks")
                .data("control", "seethrough")
                .build());

        // 50: Shadow
        inv.setItem(50, new GuiItemBuilder(Material.COAL)
                .name("<#00F2FE>Text Shadow: &b" + (holo.isShadow() ? "&aON" : "&cOFF"))
                .lore("&7Click to toggle text shadow")
                .data("control", "shadow")
                .build());

        // 51: Spacing
        inv.setItem(51, new GuiItemBuilder(Material.FEATHER)
                .name("<#00F2FE>Line Spacing: &b" + String.format("%.2f", holo.getLineSpacing()))
                .lore(
                        "&7Adjust vertical space between lines:",
                        "&a▶ Left-Click: &a+0.05",
                        "&c▶ Right-Click: &c-0.05"
                )
                .data("control", "spacing")
                .build());

        // 52: Back to list
        inv.setItem(52, new GuiItemBuilder(Material.ARROW)
                .name("&e◀ Back to Holograms List")
                .data("control", "back_to_list")
                .build());

        // 53: Delete
        inv.setItem(53, new GuiItemBuilder(Material.TNT)
                .name("<#FF0844>&lDelete Hologram")
                .lore("&7Click to permanently delete this hologram")
                .data("control", "delete_holo")
                .build());
    }

    private static Material getMaterialForLine(HoloLine line) {
        if (line.getType() == LineType.ITEM) {
            String val = LineType.extractValue(line.getRawText()).toUpperCase();
            Material m = Material.matchMaterial(val);
            return (m != null) ? m : Material.DIAMOND;
        } else if (line.getType() == LineType.BLOCK) {
            String val = LineType.extractValue(line.getRawText()).toUpperCase();
            Material m = Material.matchMaterial(val);
            return (m != null && m.isBlock()) ? m : Material.BEACON;
        } else if (line.getType() == LineType.MOB) {
            String val = LineType.extractValue(line.getRawText()).toUpperCase();
            try {
                EntityType type = EntityType.valueOf(val);
                Material egg = Material.matchMaterial(type.name() + "_SPAWN_EGG");
                return (egg != null) ? egg : Material.ALLAY_SPAWN_EGG;
            } catch (Exception e) {
                return Material.ALLAY_SPAWN_EGG;
            }
        }
        return Material.NAME_TAG;
    }
}
