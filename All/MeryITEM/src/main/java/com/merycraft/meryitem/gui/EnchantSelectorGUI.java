package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemRarity;
import com.merycraft.meryitem.item.ItemType;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Universal Enchantment Editor supporting ALL enchantments in Minecraft up to level 255.
 */
public class EnchantSelectorGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final List<Enchantment> allEnchants;

    public EnchantSelectorGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#C084FC§lMeryITEM &#334155» &#CBD5E1All Enchantments (Max 255)"));

        this.allEnchants = new ArrayList<>(Arrays.asList(Enchantment.values()));
        this.allEnchants.sort(Comparator.comparing(e -> e.getKey().getKey()));
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);

        int itemsPerPage = SLOTS.length;
        int maxPages = Math.max(1, (int) Math.ceil((double) allEnchants.size() / itemsPerPage));
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int enchantIndex = startIndex + i;
            if (enchantIndex < allEnchants.size()) {
                Enchantment ench = allEnchants.get(enchantIndex);
                String name = formatEnchantName(ench.getKey().getKey());
                int currentLevel = (cItem != null) ? cItem.getEnchantmentLevel(ench) : 0;

                List<String> lore = new ArrayList<>();
                if (currentLevel > 0) {
                    lore.add(ColorUtil.color("&#94A3B8Current Level: &#34D399" + currentLevel + " &#64748B(Max 255)"));
                } else {
                    lore.add(ColorUtil.color("&#94A3B8Current Level: &#64748BNone"));
                }
                lore.add("");
                lore.add(ColorUtil.color("&#34D399» Left-Click: Type Level in chat (1 - 255)"));
                lore.add(ColorUtil.color("&#FB7185» Right-Click: Remove Enchantment"));

                Material bookMat = (currentLevel > 0) ? Material.ENCHANTED_BOOK : Material.BOOK;
                String title = (currentLevel > 0 ? "&#C084FC§l" : "&#CBD5E1") + name;
                inventory.setItem(SLOTS[i], createItem(bookMat, title, lore));
            } else {
                inventory.setItem(SLOTS[i], border);
            }
        }

        // Bottom Navigation Bar
        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&#F472B6← Previous Page",
                    Arrays.asList("&#94A3B8Go to page " + page + " of " + maxPages)));
        } else {
            inventory.setItem(45, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BFirst Page", null));
        }

        // Hide Enchants toggle at Slot 47
        boolean isHidden = (cItem != null && cItem.isHideEnchants());
        String hideState = isHidden ? "&#FB7185[HIDDEN]" : "&#34D399[VISIBLE]";
        inventory.setItem(47, createItem(Material.ENDER_EYE, "&#C084FC🔮 Display in Lore: " + hideState,
                Arrays.asList(
                        "&#94A3B8Control whether enchantment text appears in lore",
                        "&#64748B(Enchantments stay active even when hidden)",
                        "",
                        "&#34D399» Click to toggle [VISIBLE / HIDDEN]"
                )));

        inventory.setItem(48, createItem(Material.ARROW, "&#F472B6« Back to Editor",
                Arrays.asList("&#94A3B8Return to item editor menu")));

        // Type Custom Enchantment button at Slot 49
        inventory.setItem(49, createItem(Material.WRITABLE_BOOK, "&#FDE047⌨ Type in Chat directly",
                Arrays.asList(
                        "&#94A3B8Click to type name and level directly",
                        "&#64748BExample: 'sharpness 255' or 'unbreaking 100'",
                        "",
                        "&#34D399» Click to type in chat (Max 255)"
                )));

        if (page < maxPages - 1) {
            inventory.setItem(53, createItem(Material.ARROW, "&#F472B6Next Page →",
                    Arrays.asList("&#94A3B8Go to page " + (page + 2) + " of " + maxPages)));
        } else {
            inventory.setItem(53, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BLast Page", null));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Toggle Hide Enchants (Slot 47)
        if (slot == 47) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem != null && !heldItem.getType().isAir()) {
                CustomItem c = plugin.getItemManager().getFromItemStack(heldItem);
                if (c == null) {
                    c = new CustomItem("ITEM_" + (System.currentTimeMillis() % 100000), heldItem.getType(),
                            heldItem.hasItemMeta() && heldItem.getItemMeta().hasDisplayName() ? heldItem.getItemMeta().getDisplayName() : heldItem.getType().name(),
                            ItemRarity.COMMON, ItemType.SWORD);
                }
                c.setHideEnchants(!c.isHideEnchants());
                player.getInventory().setItemInMainHand(c.buildItemStack(heldItem.getAmount()));
                if (plugin.getItemManager().getItem(c.getId()) != null) {
                    plugin.getItemManager().saveCustomItem(c);
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Enchantment display in lore: "
                        + (c.isHideEnchants() ? "&#FB7185HIDDEN (Enchants still active)" : "&#34D399VISIBLE")));
                render();
            }
            return;
        }

        // Back to editor
        if (slot == 48) {
            new ItemEditorGUI(plugin, player).open();
            return;
        }

        // Previous page
        if (slot == 45 && page > 0) {
            page--;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
            render();
            return;
        }

        // Next page
        if (slot == 53) {
            int maxPages = Math.max(1, (int) Math.ceil((double) allEnchants.size() / SLOTS.length));
            if (page < maxPages - 1) {
                page++;
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                render();
            }
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(ColorUtil.color("&#FB7185No item found in main hand!"));
            player.closeInventory();
            return;
        }

        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);
        if (cItem == null) {
            cItem = new CustomItem("ITEM_" + System.currentTimeMillis() % 100000, held.getType(),
                    held.hasItemMeta() && held.getItemMeta().hasDisplayName() ? held.getItemMeta().getDisplayName() : "&#F472B6✦ " + held.getType().name(),
                    ItemRarity.COMMON, ItemType.SWORD);
            cItem.setShowRarity(false);
        }

        // Type directly in chat (Slot 49)
        if (slot == 49) {
            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type enchantment name and level (e.g. 'sharpness 255' or 'efficiency 100'):", input -> {
                String[] parts = input.trim().split("\\s+");
                if (parts.length >= 2) {
                    String enchName = parts[0].toLowerCase();
                    Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchName));
                    if (ench == null) ench = Enchantment.getByName(enchName.toUpperCase());
                    if (ench != null) {
                        try {
                            int level = Integer.parseInt(parts[1]);
                            finalItem.setEnchantment(ench, Math.min(255, level));
                            applyAndSync(held, finalItem);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#34D399✔ Added enchantment: &#CBD5E1" + formatEnchantName(ench.getKey().getKey()) + " " + level));
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtil.color("&#FB7185Invalid level number."));
                        }
                    } else {
                        player.sendMessage(ColorUtil.color("&#FB7185Enchantment '" + parts[0] + "' not found."));
                    }
                }
                new EnchantSelectorGUI(plugin, player).open();
            });
            return;
        }

        // Enchant slots in grid
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot == SLOTS[i]) {
                int enchantIndex = (page * SLOTS.length) + i;
                if (enchantIndex < allEnchants.size()) {
                    Enchantment ench = allEnchants.get(enchantIndex);

                    if (event.getClick() == ClickType.RIGHT) {
                        cItem.removeEnchantment(ench);
                        applyAndSync(held, cItem);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.0f);
                        player.sendMessage(ColorUtil.color("&#FB7185Removed enchantment: " + formatEnchantName(ench.getKey().getKey())));
                        render();
                        return;
                    }

                    final CustomItem finalItem = cItem;
                    ChatInputListener.startPrompt(player, "Type level for " + formatEnchantName(ench.getKey().getKey()) + " (1 - 255):", input -> {
                        try {
                            int lvl = Integer.parseInt(input.trim());
                            finalItem.setEnchantment(ench, Math.min(255, Math.max(1, lvl)));
                            applyAndSync(held, finalItem);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#34D399✔ Set " + formatEnchantName(ench.getKey().getKey()) + " to level &#F472B6" + lvl));
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtil.color("&#FB7185Invalid number. Must be between 1 and 255."));
                        }
                        new EnchantSelectorGUI(plugin, player).open();
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

    private String formatEnchantName(String raw) {
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
        }
        return sb.toString();
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
