package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemRarity;
import com.merycraft.meryitem.item.ItemStat;
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

/**
 * Visual menu to adjust 100+ Custom RPG stats with paging and chat input.
 */
public class StatEditorGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final ItemStat[] allStats;

    public StatEditorGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#F87171§lMeryITEM &#334155» &#CBD5E1100+ RPG Stats"));
        this.allStats = ItemStat.values();
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.3f);
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
        int maxPages = Math.max(1, (int) Math.ceil((double) allStats.length / itemsPerPage));
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int statIndex = startIndex + i;
            if (statIndex < allStats.length) {
                ItemStat stat = allStats[statIndex];
                double val = (cItem != null) ? cItem.getStat(stat) : 0.0;

                Material mat;
                if (stat.name().contains("DAMAGE") || stat.name().contains("ATTACK") || stat.name().contains("CRIT")) {
                    mat = (val > 0) ? Material.NETHERITE_SWORD : Material.IRON_SWORD;
                } else if (stat.name().contains("DEFENSE") || stat.name().contains("ARMOR") || stat.name().contains("RESISTANCE")) {
                    mat = (val > 0) ? Material.NETHERITE_CHESTPLATE : Material.SHIELD;
                } else if (stat.name().contains("HEALTH") || stat.name().contains("HEAL") || stat.name().contains("LIFE")) {
                    mat = Material.GOLDEN_APPLE;
                } else if (stat.name().contains("SPEED") || stat.name().contains("SWIFT") || stat.name().contains("JUMP")) {
                    mat = Material.SUGAR;
                } else if (stat.name().contains("MAGIC") || stat.name().contains("MANA")) {
                    mat = Material.BLAZE_ROD;
                } else {
                    mat = (val > 0) ? Material.EMERALD : Material.DIAMOND;
                }

                List<String> lore = new ArrayList<>();
                lore.add(ColorUtil.color("&#94A3B8Current Value: " + stat.getHexColor() + stat.formatValue(val)));
                lore.add("");
                lore.add(ColorUtil.color("&#34D399» Left-Click: +5"));
                lore.add(ColorUtil.color("&#38BDF8» Shift + Left-Click: +1"));
                lore.add(ColorUtil.color("&#FB7185» Right-Click: -5"));
                lore.add(ColorUtil.color("&#F43F5E» Shift + Right-Click: -1"));
                lore.add(ColorUtil.color("&#FBBF24» Middle-Click: Type custom value"));

                inventory.setItem(SLOTS[i], createItem(mat, stat.getHexColor() + stat.getIcon() + " " + stat.getDisplayName(), lore));
            } else {
                inventory.setItem(SLOTS[i], border);
            }
        }

        // Paging controls
        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&#F472B6← Previous Page", Arrays.asList("&#94A3B8Go to page " + page + " of " + maxPages)));
        } else {
            inventory.setItem(45, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BFirst Page", null));
        }

        inventory.setItem(48, createItem(Material.ARROW, "&#F472B6« Back to Editor", Arrays.asList("&#94A3B8Return to item editor menu")));

        // Type directly in chat button at Slot 49
        inventory.setItem(49, createItem(Material.WRITABLE_BOOK, "&#FDE047⌨ Type in Chat directly",
                Arrays.asList(
                        "&#94A3B8Click to type stat and value directly in chat",
                        "&#64748BExample: 'attack_damage 150' or 'defense 80'",
                        "",
                        "&#34D399» Click to type in chat"
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

        if (slot == 48) {
            new ItemEditorGUI(plugin, player).open();
            return;
        }

        if (slot == 45 && page > 0) {
            page--;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
            render();
            return;
        }

        int maxPages = Math.max(1, (int) Math.ceil((double) allStats.length / SLOTS.length));
        if (slot == 53 && page < maxPages - 1) {
            page++;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
            render();
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
            ChatInputListener.startPrompt(player, "Type stat and value (e.g. 'attack_damage 150' or 'defense 80'):", input -> {
                String[] parts = input.trim().split("\\s+");
                if (parts.length >= 2) {
                    ItemStat stat = ItemStat.fromString(parts[0]);
                    if (stat != null) {
                        try {
                            double val = Double.parseDouble(parts[1]);
                            finalItem.setStat(stat, val);
                            applyAndSync(held, finalItem);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#34D399✔ Set " + stat.getDisplayName() + " to: " + stat.getHexColor() + stat.formatValue(val)));
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtil.color("&#FB7185Invalid number."));
                        }
                    } else {
                        player.sendMessage(ColorUtil.color("&#FB7185Stat '" + parts[0] + "' not found."));
                    }
                }
                new StatEditorGUI(plugin, player).open();
            });
            return;
        }

        // Stat slots
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot == SLOTS[i]) {
                int statIndex = (page * SLOTS.length) + i;
                if (statIndex < allStats.length) {
                    ItemStat stat = allStats[statIndex];
                    double currentVal = cItem.getStat(stat);
                    double delta = 0.0;

                    if (event.getClick() == ClickType.LEFT) {
                        delta = 5.0;
                    } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                        delta = 1.0;
                    } else if (event.getClick() == ClickType.RIGHT) {
                        delta = -5.0;
                    } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        delta = -1.0;
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        final CustomItem finalItem = cItem;
                        ChatInputListener.startPrompt(player, "Type new value for " + stat.getDisplayName() + ":", input -> {
                            try {
                                double val = Double.parseDouble(input.trim());
                                finalItem.setStat(stat, val);
                                applyAndSync(held, finalItem);
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                                player.sendMessage(ColorUtil.color("&#34D399✔ Set " + stat.getDisplayName() + " to: " + stat.getHexColor() + stat.formatValue(val)));
                            } catch (NumberFormatException e) {
                                player.sendMessage(ColorUtil.color("&#FB7185Invalid number."));
                            }
                            new StatEditorGUI(plugin, player).open();
                        });
                        return;
                    }

                    if (delta != 0.0) {
                        double newVal = Math.max(0.0, currentVal + delta);
                        cItem.setStat(stat, newVal);
                        applyAndSync(held, cItem);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                        render();
                    }
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
