package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visual GUI to view, edit, add, or delete specific description (lore) lines (Supports unlimited lines with paging).
 */
public class LoreManagerGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public LoreManagerGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#FDA4AF§lMeryITEM &#334155» &#CBD5E1Description Manager"));
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);
        List<String> lines = (cItem != null) ? cItem.getCustomLore() : new ArrayList<>();

        int itemsPerPage = SLOTS.length;
        int maxPages = Math.max(1, (int) Math.ceil((double) lines.size() / itemsPerPage));
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int lineIndex = startIndex + i;
            if (lineIndex < lines.size()) {
                String lineText = lines.get(lineIndex);
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.color("&#94A3B8Text: &#CBD5E1" + lineText));
                lore.add("");
                lore.add(ColorUtil.color("&#34D399» Left-Click: EDIT this line in chat"));
                lore.add(ColorUtil.color("&#FB7185» Right-Click: DELETE this line"));
                lore.add(ColorUtil.color("&#38BDF8» Shift-Left: Move UP ▲ (สลับขึ้น)"));
                lore.add(ColorUtil.color("&#FBBF24» Shift-Right: Move DOWN ▼ (สลับลง)"));
                lore.add(ColorUtil.color("&#C084FC» Drop (Q Key): Move to TOP ⤒ (ย้ายขึ้นบนสุด)"));
                lore.add(ColorUtil.color("&#A855F7» Middle-Click: Move to BOTTOM ⤓ (ย้ายลงล่างสุด)"));

                inventory.setItem(SLOTS[i], createItem(Material.PAPER, "&#FDA4AFLine #" + (lineIndex + 1), lore));
            } else {
                inventory.setItem(SLOTS[i], border);
            }
        }

        // Navigation (Bottom Row: 45 - 53)
        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&#F472B6← Previous Page", Arrays.asList("&#94A3B8Go to page " + page + " of " + maxPages)));
        } else {
            inventory.setItem(45, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BFirst Page", null));
        }

        // Swap 2 Lines at Slot 46
        inventory.setItem(46, createItem(Material.HOPPER, "&#FDE047§l🔄 Swap 2 Lines",
                Arrays.asList(
                        "&#94A3B8Swap two line positions in chat",
                        "&#64748B(e.g. Type '1 4' to swap Line #1 with #4)",
                        "",
                        "&#FDE047» Click to swap lines"
                )));

        // Jump to / Edit line by number at Slot 47
        inventory.setItem(47, createItem(Material.NAME_TAG, "&#38BDF8# Edit by Line Number",
                Arrays.asList(
                        "&#94A3B8Type line number in chat to edit directly",
                        "&#64748B(e.g. Type '3' then type replacement text)",
                        "",
                        "&#38BDF8» Click to select line number"
                )));

        // Back Button at Slot 48
        inventory.setItem(48, createItem(Material.ARROW, "&#F472B6« Back to Editor",
                Arrays.asList("&#94A3B8Return to item editor menu")));

        // Add Line Button at Slot 49
        inventory.setItem(49, createItem(Material.WRITABLE_BOOK, "&#34D399+ Add New Line",
                Arrays.asList(
                        "&#94A3B8Click to append a new description line",
                        "&#64748B(Supports hex &#RRGGBB, %sec%, %item_name%)",
                        "",
                        "&#34D399» Click to add line"
                )));

        // Delete line by number at Slot 50
        inventory.setItem(50, createItem(Material.SHEARS, "&#F87171✂ Delete by Line Number",
                Arrays.asList(
                        "&#94A3B8Type line number in chat to remove immediately",
                        "&#64748B(e.g. Type '2' to delete Line #2)",
                        "",
                        "&#F87171» Click to delete by number"
                )));

        // Clear All Lines Button at Slot 51
        inventory.setItem(51, createItem(Material.LAVA_BUCKET, "&#FB7185✖ Clear All Lines",
                Arrays.asList("&#94A3B8Delete all description lines from this item", "", "&#FB7185» Click to clear all")));

        // Invert / Reverse Order at Slot 52
        inventory.setItem(52, createItem(Material.CLOCK, "&#C084FC§l⇅ Reverse All Lines",
                Arrays.asList(
                        "&#94A3B8Flip all lines upside down (Bottom ➔ Top)",
                        "&#64748B(Reverses entire lore order with 1 click)",
                        "",
                        "&#C084FC» Click to reverse line order"
                )));

        if (page < maxPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "&#F472B6Next Page →", Arrays.asList("&#94A3B8Go to page " + (page + 2) + " of " + maxPages)));
        } else {
            inventory.setItem(53, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BLast Page", null));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(ColorUtil.color("&#FB7185No item found in main hand!"));
            player.closeInventory();
            return;
        }

        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);
        if (cItem == null) {
            new ItemEditorGUI(plugin, player).open();
            return;
        }

        // Paging
        if (slot == 45 && page > 0) {
            page--;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
            render();
            return;
        }
        int maxPages = Math.max(1, (int) Math.ceil((double) cItem.getCustomLore().size() / SLOTS.length));
        if (slot == 53 && page < maxPages - 1) {
            page++;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
            render();
            return;
        }

        // Back to editor
        if (slot == 48) {
            new ItemEditorGUI(plugin, player).open();
            return;
        }

        // Add Line Button (Slot 49)
        if (slot == 49) {
            final CustomItem finalItem = cItem;
            player.sendMessage(ColorUtil.color("&#38BDF8[💡 Placeholders: %sec% = Cooldown, %cd% = Cooldown 's', %item_name% = Name, %rarity% = Rarity]"));
            ChatInputListener.startPrompt(player, "Type new line of lore to append (Supports hex &#RRGGBB & color codes):", input -> {
                List<String> list = new ArrayList<>(finalItem.getCustomLore());
                list.add(input.trim());
                finalItem.setCustomLore(list);
                applyAndSync(held, finalItem);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#34D399✔ Appended line: &#CBD5E1" + input.trim()));
                new LoreManagerGUI(plugin, player).open();
            });
            return;
        }

        // Clear All Lines Button (Slot 51)
        if (slot == 51) {
            cItem.setCustomLore(new ArrayList<>());
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.0f);
            player.sendMessage(ColorUtil.color("&#FB7185Cleared all custom description lines from item."));
            render();
            return;
        }

        // Swap 2 lines by numbers (Slot 46)
        if (slot == 46) {
            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type two line numbers to SWAP (e.g. '1 4' to swap Line #1 with #4):", input -> {
                String[] parts = input.trim().split("\\s+");
                if (parts.length < 2) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please provide two line numbers separated by space (e.g. '1 3')."));
                    new LoreManagerGUI(plugin, player).open();
                    return;
                }
                try {
                    int idxA = Integer.parseInt(parts[0]) - 1;
                    int idxB = Integer.parseInt(parts[1]) - 1;
                    List<String> list = new ArrayList<>(finalItem.getCustomLore());
                    if (idxA < 0 || idxA >= list.size() || idxB < 0 || idxB >= list.size()) {
                        player.sendMessage(ColorUtil.color("&#FB7185Line numbers out of range (Total lines: " + list.size() + ")."));
                        new LoreManagerGUI(plugin, player).open();
                        return;
                    }
                    java.util.Collections.swap(list, idxA, idxB);
                    finalItem.setCustomLore(list);
                    applyAndSync(held, finalItem);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#34D399✔ Swapped Line #" + (idxA + 1) + " with Line #" + (idxB + 1) + "!"));
                    new LoreManagerGUI(plugin, player).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&#FB7185Invalid numbers provided."));
                    new LoreManagerGUI(plugin, player).open();
                }
            });
            return;
        }

        // Edit by line number (Slot 47)
        if (slot == 47) {
            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type the Line Number to EDIT (e.g. '1' or '3'):", numInput -> {
                try {
                    int lineNum = Integer.parseInt(numInput.trim()) - 1;
                    if (lineNum < 0 || lineNum >= finalItem.getCustomLore().size()) {
                        player.sendMessage(ColorUtil.color("&#FB7185Line number out of range (Total lines: " + finalItem.getCustomLore().size() + ")."));
                        new LoreManagerGUI(plugin, player).open();
                        return;
                    }
                    player.sendMessage(ColorUtil.color("&#38BDF8[💡 Placeholders: %sec% = Cooldown, %cd% = Cooldown 's', %item_name% = Name]"));
                    ChatInputListener.startPrompt(player, "Type replacement text for Line #" + (lineNum + 1) + ":", textInput -> {
                        List<String> list = new ArrayList<>(finalItem.getCustomLore());
                        list.set(lineNum, textInput.trim());
                        finalItem.setCustomLore(list);
                        applyAndSync(held, finalItem);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                        player.sendMessage(ColorUtil.color("&#34D399✔ Updated Line #" + (lineNum + 1) + " to: &#CBD5E1" + textInput.trim()));
                        new LoreManagerGUI(plugin, player).open();
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&#FB7185Invalid number."));
                    new LoreManagerGUI(plugin, player).open();
                }
            });
            return;
        }

        // Delete by line number (Slot 50)
        if (slot == 50) {
            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type the Line Number to DELETE (e.g. '2'):", numInput -> {
                try {
                    int lineNum = Integer.parseInt(numInput.trim()) - 1;
                    if (lineNum < 0 || lineNum >= finalItem.getCustomLore().size()) {
                        player.sendMessage(ColorUtil.color("&#FB7185Line number out of range (Total lines: " + finalItem.getCustomLore().size() + ")."));
                        new LoreManagerGUI(plugin, player).open();
                        return;
                    }
                    List<String> list = new ArrayList<>(finalItem.getCustomLore());
                    String removed = list.remove(lineNum);
                    finalItem.setCustomLore(list);
                    applyAndSync(held, finalItem);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                    player.sendMessage(ColorUtil.color("&#FB7185Deleted Line #" + (lineNum + 1) + ": " + removed));
                    new LoreManagerGUI(plugin, player).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&#FB7185Invalid number."));
                    new LoreManagerGUI(plugin, player).open();
                }
            });
            return;
        }

        // Invert / Reverse All Lines (Slot 52)
        if (slot == 52) {
            List<String> list = new ArrayList<>(cItem.getCustomLore());
            if (list.size() > 1) {
                java.util.Collections.reverse(list);
                cItem.setCustomLore(list);
                applyAndSync(held, cItem);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#C084FC✔ Reversed all description lines (Bottom ➔ Top)!"));
                render();
            } else {
                player.sendMessage(ColorUtil.color("&#FB7185Need at least 2 lines to reverse order."));
            }
            return;
        }

        // Click individual line in grid
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot == SLOTS[i]) {
                int lineIndex = (page * SLOTS.length) + i;
                if (lineIndex < cItem.getCustomLore().size()) {
                    ClickType ct = event.getClick();

                    // Shift-Left: Move UP ▲
                    if (ct == ClickType.SHIFT_LEFT) {
                        if (lineIndex > 0) {
                            List<String> list = new ArrayList<>(cItem.getCustomLore());
                            java.util.Collections.swap(list, lineIndex, lineIndex - 1);
                            cItem.setCustomLore(list);
                            applyAndSync(held, cItem);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#38BDF8Moved Line #" + (lineIndex + 1) + " UP to #" + lineIndex));
                            render();
                        } else {
                            player.sendMessage(ColorUtil.color("&#FB7185This line is already at the top!"));
                        }
                        return;
                    }

                    // Shift-Right: Move DOWN ▼
                    if (ct == ClickType.SHIFT_RIGHT) {
                        List<String> list = new ArrayList<>(cItem.getCustomLore());
                        if (lineIndex < list.size() - 1) {
                            java.util.Collections.swap(list, lineIndex, lineIndex + 1);
                            cItem.setCustomLore(list);
                            applyAndSync(held, cItem);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#FBBF24Moved Line #" + (lineIndex + 1) + " DOWN to #" + (lineIndex + 2)));
                            render();
                        } else {
                            player.sendMessage(ColorUtil.color("&#FB7185This line is already at the bottom!"));
                        }
                        return;
                    }

                    // Drop (Q key): Move to TOP ⤒
                    if (ct == ClickType.DROP || ct == ClickType.CONTROL_DROP) {
                        if (lineIndex > 0) {
                            List<String> list = new ArrayList<>(cItem.getCustomLore());
                            String moved = list.remove(lineIndex);
                            list.add(0, moved);
                            cItem.setCustomLore(list);
                            applyAndSync(held, cItem);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#C084FCMoved Line #" + (lineIndex + 1) + " to the TOP (#1)!"));
                            render();
                        } else {
                            player.sendMessage(ColorUtil.color("&#FB7185This line is already at the top!"));
                        }
                        return;
                    }

                    // Middle-Click: Move to BOTTOM ⤓
                    if (ct == ClickType.MIDDLE) {
                        List<String> list = new ArrayList<>(cItem.getCustomLore());
                        if (lineIndex < list.size() - 1) {
                            String moved = list.remove(lineIndex);
                            list.add(moved);
                            cItem.setCustomLore(list);
                            applyAndSync(held, cItem);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#A855F7Moved Line #" + (lineIndex + 1) + " to the BOTTOM (#" + list.size() + ")!"));
                            render();
                        } else {
                            player.sendMessage(ColorUtil.color("&#FB7185This line is already at the bottom!"));
                        }
                        return;
                    }

                    if (ct == ClickType.RIGHT) {
                        // Delete line
                        List<String> list = new ArrayList<>(cItem.getCustomLore());
                        String removed = list.remove(lineIndex);
                        cItem.setCustomLore(list);
                        applyAndSync(held, cItem);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                        player.sendMessage(ColorUtil.color("&#FB7185Deleted Line #" + (lineIndex + 1) + ": " + removed));
                        render();
                        return;
                    }

                    // Edit line
                    final CustomItem finalItem = cItem;
                    player.sendMessage(ColorUtil.color("&#38BDF8[💡 Placeholders: %sec% = Cooldown, %cd% = Cooldown 's', %item_name% = Name]"));
                    ChatInputListener.startPrompt(player, "Type new text for Line #" + (lineIndex + 1) + ":", input -> {
                        List<String> list = new ArrayList<>(finalItem.getCustomLore());
                        list.set(lineIndex, input.trim());
                        finalItem.setCustomLore(list);
                        applyAndSync(held, finalItem);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                        player.sendMessage(ColorUtil.color("&#34D399✔ Updated Line #" + (lineIndex + 1) + " to: &#CBD5E1" + input.trim()));
                        new LoreManagerGUI(plugin, player).open();
                    });
                    return;
                }
            }
        }
    }

    private void applyAndSync(ItemStack held, CustomItem cItem) {
        player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
        if (plugin.getItemManager().getItem(cItem.getId()) != null) {
            plugin.getItemManager().saveCustomItem(cItem);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            if (lore != null) {
                meta.setLore(ColorUtil.color(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
