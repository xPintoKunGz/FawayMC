package com.holomery.gui;

import com.holomery.color.ColorParser;
import com.holomery.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Click Actions Management GUI.
 */
public class HoloActionGUI {

    public static void open(Player player, Hologram holo) {
        if (holo == null) return;

        String title = ColorParser.colorize("<gradient:#FF0080:#FA709A>&l✦ Actions: </gradient>&b" + holo.getId() + " &7[P" + (holo.getActivePageIndex() + 1) + "]");
        HoloHolders.ActionHolder holder = new HoloHolders.ActionHolder(holo);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        ItemStack border = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();

        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 36; i < 45; i++) inv.setItem(i, border);

        // Current Actions list summary (Slot 4)
        List<String> actions = holo.getClickActions();
        List<String> currentLore = new ArrayList<>();
        currentLore.add("&7Configured Actions for Page " + (holo.getActivePageIndex() + 1) + ":");
        if (actions.isEmpty()) {
            currentLore.add("&8  (None - defaults to next page)");
        } else {
            for (int i = 0; i < actions.size(); i++) {
                currentLore.add("&8  #" + (i + 1) + " &b" + actions.get(i));
            }
        }
        currentLore.add("");
        currentLore.add("&c▶ Click to Clear All Actions on this page");

        inv.setItem(4, new GuiItemBuilder(Material.CHEST)
                .name("<#00FF87>&lCurrent Actions (" + actions.size() + ")")
                .lore(currentLore)
                .data("action_btn", "clear")
                .glow(!actions.isEmpty())
                .build());

        // 1. Jump to Page (PAGE:N)
        inv.setItem(19, new GuiItemBuilder(Material.WRITABLE_BOOK)
                .name("<gradient:#00F2FE:#4FACFE>&l1. Jump to Page (PAGE:N)</gradient>")
                .lore(
                        "&7Clicking the hologram jumps to a specific page!",
                        "",
                        "&a▶ Left-Click: &eJump to Page 1",
                        "&b▶ Right-Click: &eJump to Next Page (PAGE:NEXT)",
                        "&6▶ Shift-Click: &eEnter custom page number in chat"
                )
                .data("action_btn", "page_jump")
                .glow(true)
                .build());

        // 2. Player Command ([player] command)
        inv.setItem(21, new GuiItemBuilder(Material.PLAYER_HEAD)
                .name("<gradient:#FA709A:#FEE140>&l2. Run Player Command</gradient>")
                .lore(
                        "&7Executes a command as the clicking player!",
                        "&7Supports &e%player_name%&7 and PlaceholderAPI.",
                        "&7Examples: &fspawn&7, &fwarp pvp&7, &fshop",
                        "",
                        "&a▶ Click to type command in chat"
                )
                .data("action_btn", "player_cmd")
                .glow(true)
                .build());

        // 3. Console Command ([console] command)
        inv.setItem(23, new GuiItemBuilder(Material.COMMAND_BLOCK)
                .name("<gradient:#FF0844:#FFB199>&l3. Run Console Command</gradient>")
                .lore(
                        "&7Executes a command with console permissions!",
                        "&7Examples: &fgive %player_name% diamond 1",
                        "&8           &feco give %player_name% 100",
                        "",
                        "&a▶ Click to type command in chat"
                )
                .data("action_btn", "console_cmd")
                .glow(true)
                .build());

        // 4. Send Message / Sound
        inv.setItem(25, new GuiItemBuilder(Material.BELL)
                .name("<gradient:#8A2387:#E94057>&l4. Message or Sound Effect</gradient>")
                .lore(
                        "&7Send a colored chat message or play a sound!",
                        "",
                        "&a▶ Left-Click: &eAdd Chat Message",
                        "&b▶ Right-Click: &eAdd Sound (EXPERIENCE_ORB_PICKUP)"
                )
                .data("action_btn", "msg_sound")
                .glow(true)
                .build());

        // Back to editor
        inv.setItem(40, new GuiItemBuilder(Material.ARROW)
                .name("&e◀ Back to Editor")
                .data("action_btn", "back")
                .build());

        player.openInventory(inv);
    }
}

