package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemType;
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
import java.util.stream.Collectors;

/**
 * Modern, intuitive graphical Item Browser for MeryITEM (100% English).
 */
public class ItemBrowserGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;
    private String currentCategory = "ALL";
    private String currentFolder = "ALL";

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public ItemBrowserGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#F472B6§lMeryITEM &#334155» &#CBD5E1Item Catalog"));
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.4f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Folder Filter Button at Slot 0
        inventory.setItem(0, createItem(Material.CHEST, "&#FDE047§l📁 Folder: &#CBD5E1" + currentFolder,
                Arrays.asList(
                        "&#94A3B8Filter items by custom folder",
                        "&#94A3B8Current: &#FDE047" + currentFolder,
                        "",
                        "&#34D399» Left-Click: Cycle folder",
                        "&#FB7185» Right-Click: Show ALL folders"
                )));

        // Top Row: Category Filter Buttons (Slots 1, 2, 3, 4, 5)
        inventory.setItem(1, createCategoryButton("ALL", Material.NETHER_STAR, "&#F472B6✦ ALL ITEMS"));
        inventory.setItem(2, createCategoryButton("WEAPONS", Material.NETHERITE_SWORD, "&#F87171⚔ WEAPONS"));
        inventory.setItem(3, createCategoryButton("ARMOR", Material.NETHERITE_CHESTPLATE, "&#60A5FA🛡 ARMOR"));
        inventory.setItem(4, createCategoryButton("ACCESSORIES", Material.AMETHYST_SHARD, "&#C084FC💍 ACCESSORIES"));
        inventory.setItem(5, createCategoryButton("CONSUMABLES", Material.POTION, "&#34D399🧪 CONSUMABLES"));

        // Player Stats Button at Slot 7
        inventory.setItem(7, createItem(Material.PLAYER_HEAD, "&#38BDF8✦ Your Active Stats",
                Arrays.asList(
                        "&#94A3B8Click to inspect your current",
                        "&#94A3B8RPG stats and bonuses.",
                        "",
                        "&#38BDF8» Click to inspect"
                )));

        // Create Folder Shortcut at Slot 8
        inventory.setItem(8, createItem(Material.WRITABLE_BOOK, "&#34D399§l+ New Folder",
                Arrays.asList(
                        "&#94A3B8Create a new folder category",
                        "",
                        "&#34D399» Click to create in chat"
                )));

        List<CustomItem> items = filterItems();

        // Empty State: If no saved items in catalog
        if (items.isEmpty()) {
            inventory.setItem(22, createItem(Material.ANVIL, "&#F472B6§l✦ Item Catalog is Empty!",
                    Arrays.asList(
                            "&#94A3B8No custom items have been saved yet.",
                            "",
                            "&#CBD5E1» Hold an item in your main hand to create one.",
                            "&#34D399» Click here to open In-Hand Item Editor!"
                    )));
        } else {
            int itemsPerPage = ITEM_SLOTS.length;
            int maxPages = Math.max(1, (int) Math.ceil((double) items.size() / itemsPerPage));
            if (page >= maxPages) page = maxPages - 1;
            if (page < 0) page = 0;

            int startIndex = page * itemsPerPage;
            for (int i = 0; i < itemsPerPage; i++) {
                int slot = ITEM_SLOTS[i];
                int itemIndex = startIndex + i;
                if (itemIndex < items.size()) {
                    CustomItem cItem = items.get(itemIndex);
                    ItemStack stack = cItem.buildItemStack(1);
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                        lore.add("");
                        lore.add(ColorUtil.color("&#38BDF8» Left-Click: &#CBD5E1EDIT this item (Open Editor)"));
                        lore.add(ColorUtil.color("&#FB7185» Right-Click: &#CBD5E1DELETE from catalog"));
                        lore.add(ColorUtil.color("&#F472B6» Shift-Left: &#CBD5E1Take 1x item"));
                        lore.add(ColorUtil.color("&#FDA4AF» Shift-Right: &#CBD5E1Take 64x items"));
                        meta.setLore(lore);
                        stack.setItemMeta(meta);
                    }
                    inventory.setItem(slot, stack);
                } else {
                    inventory.setItem(slot, border);
                }
            }
        }

        // Bottom Controls Row
        int maxPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEM_SLOTS.length));

        // Previous page at 45
        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&#F472B6← Previous Page",
                    Arrays.asList("&#94A3B8Go to page " + page + " of " + maxPages)));
        } else {
            inventory.setItem(45, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BFirst Page", null));
        }

        // Open in-hand editor shortcut at Slot 48
        inventory.setItem(48, createItem(Material.CRAFTING_TABLE, "&#F472B6✏ Edit In-Hand Item",
                Arrays.asList(
                        "&#94A3B8Open the editor for item currently held",
                        "&#64748B(Requires an item in main hand)",
                        "",
                        "&#34D399» Click to open editor"
                )));

        // Page info at Slot 49
        inventory.setItem(49, createItem(Material.COMPASS,
                String.format("&#FDA4AFPage %d of %d", page + 1, maxPages),
                Arrays.asList(
                        "&#94A3B8Saved Items: &#F472B6" + items.size() + " &#94A3B8items",
                        "&#94A3B8Category: &#CBD5E1" + currentCategory,
                        "",
                        "&#64748B✦ MeryITEM Catalog"
                )));

        // Next page at 53
        if (page < maxPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "&#F472B6Next Page →",
                    Arrays.asList("&#94A3B8Go to page " + (page + 2) + " of " + maxPages)));
        } else {
            inventory.setItem(53, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BLast Page", null));
        }
    }

    private List<CustomItem> filterItems() {
        return plugin.getItemManager().getAllItems().stream().filter(item -> {
            if (!"ALL".equalsIgnoreCase(currentFolder)) {
                if (!currentFolder.equalsIgnoreCase(item.getFolder())) {
                    return false;
                }
            }
            ItemType t = item.getType();
            switch (currentCategory) {
                case "WEAPONS":
                    return t.isWeapon();
                case "ARMOR":
                    return t.isArmorOrDefense();
                case "ACCESSORIES":
                    return t == ItemType.ACCESSORY;
                case "CONSUMABLES":
                    return t == ItemType.CONSUMABLE;
                default:
                    return true;
            }
        }).collect(Collectors.toList());
    }

    private ItemStack createCategoryButton(String category, Material mat, String name) {
        boolean selected = currentCategory.equals(category);
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtil.color(selected ? "&#34D399✔ Currently Selected" : "&#94A3B8Click to filter by " + category.toLowerCase()));
        return createItem(mat, selected ? "&#FDE047§l" + ColorUtil.strip(name) : name, lore);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Folder Filter (Slot 0)
        if (slot == 0) {
            if (event.getClick() == ClickType.RIGHT) {
                currentFolder = "ALL";
            } else {
                List<String> list = new ArrayList<>();
                list.add("ALL");
                list.addAll(plugin.getItemManager().getAllFolders());
                int idx = list.indexOf(currentFolder);
                int next = (idx + 1) % list.size();
                currentFolder = list.get(next);
            }
            page = 0;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            render();
            return;
        }

        // Categories
        if (slot == 1) { switchCategory("ALL"); return; }
        if (slot == 2) { switchCategory("WEAPONS"); return; }
        if (slot == 3) { switchCategory("ARMOR"); return; }
        if (slot == 4) { switchCategory("ACCESSORIES"); return; }
        if (slot == 5) { switchCategory("CONSUMABLES"); return; }

        // Player Stats
        if (slot == 7) {
            new StatsGUI(plugin, player).open();
            return;
        }

        // Create Folder (Slot 8)
        if (slot == 8) {
            ChatInputListener.startPrompt(player, "Type name for new item folder (e.g. 'Boss_Drops'):", fInput -> {
                String cleanName = fInput.trim().replace(" ", "_");
                if (!cleanName.isEmpty()) {
                    plugin.getItemManager().createFolder(cleanName);
                    this.currentFolder = cleanName;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#34D399✔ Created and switched to folder: &#FDE047" + cleanName));
                }
                new ItemBrowserGUI(plugin, player).open();
            });
            return;
        }

        // Empty state Anvil button (Slot 22) or Crafting table button (Slot 48)
        if (slot == 22 || slot == 48) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand first!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
                return;
            }
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
        if (slot == 53) {
            List<CustomItem> items = filterItems();
            int maxPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEM_SLOTS.length));
            if (page < maxPages - 1) {
                page++;
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                render();
            }
            return;
        }

        // Item slots
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (slot == ITEM_SLOTS[i]) {
                List<CustomItem> items = filterItems();
                int itemIndex = (page * ITEM_SLOTS.length) + i;
                if (itemIndex < items.size()) {
                    CustomItem customItem = items.get(itemIndex);

                    // 1. Shift-Left / Shift-Right: Take item into inventory
                    if (event.getClick() == ClickType.SHIFT_LEFT) {
                        player.getInventory().addItem(customItem.buildItemStack(1));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.4f);
                        player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Given &#F472B61x " + customItem.getDisplayName()));
                        return;
                    }
                    if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        player.getInventory().addItem(customItem.buildItemStack(64));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.4f);
                        player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Given &#F472B664x " + customItem.getDisplayName()));
                        return;
                    }

                    // 2. Right-Click: DELETE item from catalog permanently
                    if (event.getClick() == ClickType.RIGHT) {
                        String idToDelete = customItem.getId();
                        boolean deleted = plugin.getItemManager().deleteCustomItem(idToDelete);
                        if (deleted) {
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.2f);
                            player.sendMessage(ColorUtil.color("&#FB7185✔ Successfully deleted item '&#F472B6" + idToDelete + "&#FB7185' from catalog!"));
                            render();
                        }
                        return;
                    }

                    // 3. Left-Click (Default): Open ItemEditorGUI directly so user can edit and save!
                    player.getInventory().setItemInMainHand(customItem.buildItemStack(1));
                    new ItemEditorGUI(plugin, player).open();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#34D399✔ Opened item &#CBD5E1" + customItem.getId() + " &#34D399in Item Editor!"));
                    return;
                }
            }
        }
    }

    private void switchCategory(String category) {
        this.currentCategory = category;
        this.page = 0;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        render();
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
