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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Universal Potion Effect Editor supporting ALL Minecraft potion effects with levels up to 250 and duration 9999s.
 */
public class PotionEffectGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final List<PotionEffectType> allEffects;

    public PotionEffectGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#34D399§lMeryITEM &#334155» &#CBD5E1Potion Effects (Max 250, 9999s)"));

        this.allEffects = new ArrayList<>();
        for (PotionEffectType pet : PotionEffectType.values()) {
            if (pet != null) {
                allEffects.add(pet);
            }
        }
        this.allEffects.sort(Comparator.comparing(PotionEffectType::getName));
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
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
        int maxPages = Math.max(1, (int) Math.ceil((double) allEffects.size() / itemsPerPage));
        if (page >= maxPages) page = maxPages - 1;
        if (page < 0) page = 0;

        int startIndex = page * itemsPerPage;
        for (int i = 0; i < itemsPerPage; i++) {
            int index = startIndex + i;
            if (index < allEffects.size()) {
                PotionEffectType effect = allEffects.get(index);
                String name = formatName(effect.getName());
                int currentLevel = (cItem != null) ? cItem.getPotionLevel(effect) : 0;

                List<String> lore = new ArrayList<>();
                if (currentLevel > 0) {
                    lore.add(ColorUtil.color("&#94A3B8Active Level: &#34D399" + currentLevel + " &#64748B(Max 250, 9999s)"));
                } else {
                    lore.add(ColorUtil.color("&#94A3B8Active Level: &#64748BNone"));
                }
                lore.add("");
                lore.add(ColorUtil.color("&#34D399» Left-Click: Type Level in chat (1 - 250)"));
                lore.add(ColorUtil.color("&#FB7185» Right-Click: Remove Effect"));

                Material potionMat = (currentLevel > 0) ? Material.SPLASH_POTION : Material.POTION;
                String title = (currentLevel > 0 ? "&#34D399§l" : "&#CBD5E1") + name;
                inventory.setItem(SLOTS[i], createItem(potionMat, title, lore));
            } else {
                inventory.setItem(SLOTS[i], border);
            }
        }

        // Bottom Bar
        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&#F472B6← Previous Page", Arrays.asList("&#94A3B8Go to page " + page + " of " + maxPages)));
        } else {
            inventory.setItem(45, createItem(Material.GRAY_STAINED_GLASS_PANE, "&#64748BFirst Page", null));
        }

        // Trigger Switch button at Slot 47 (HOLD vs RIGHT vs LEFT)
        String trigMode = (cItem != null) ? cItem.getPotionTrigger() : "HOLD";
        String modeColor = trigMode.equals("HOLD") ? "&#34D399" : "&#38BDF8";
        inventory.setItem(47, createItem(Material.CLOCK, "&#FDE047⚡ Trigger: " + modeColor + "[" + trigMode + "]",
                Arrays.asList(
                        "&#94A3B8Current Mode: " + modeColor + trigMode,
                        "",
                        "&#CBD5E1• &#34D399HOLD: &#94A3B8Passive effect while held in hand",
                        "&#CBD5E1• &#38BDF8RIGHT: &#94A3B8Click Right-Click to apply (9999s)",
                        "&#CBD5E1• &#38BDF8LEFT: &#94A3B8Click Left-Click to apply (9999s)",
                        "",
                        "&#F472B6» Click to Switch Mode (HOLD ➔ RIGHT ➔ LEFT)"
                )));

        inventory.setItem(48, createItem(Material.ARROW, "&#F472B6« Back to Editor", Arrays.asList("&#94A3B8Return to item editor menu")));

        // Type directly in chat button at Slot 49
        inventory.setItem(49, createItem(Material.WRITABLE_BOOK, "&#FDE047⌨ Type in Chat directly",
                Arrays.asList(
                        "&#94A3B8Click to type effect and level directly",
                        "&#64748BExample: 'speed 250' or 'strength 100'",
                        "",
                        "&#34D399» Click to type in chat (Max 250, 9999s)"
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

        int maxPages = Math.max(1, (int) Math.ceil((double) allEffects.size() / SLOTS.length));
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

        // Toggle Trigger Mode (Slot 47)
        if (slot == 47) {
            String current = cItem.getPotionTrigger();
            String next;
            if (current.equals("HOLD")) next = "RIGHT";
            else if (current.equals("RIGHT")) next = "LEFT";
            else next = "HOLD";

            cItem.setPotionTrigger(next);
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Potion trigger mode set to: &#38BDF8[" + next + "]"));
            render();
            return;
        }

        // Type directly in chat (Slot 49)
        if (slot == 49) {
            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type effect and level (e.g. 'speed 250' or 'strength 100 9999'):", input -> {
                String[] parts = input.trim().split("\\s+");
                if (parts.length >= 2) {
                    String effName = parts[0].toUpperCase();
                    PotionEffectType pet = PotionEffectType.getByName(effName);
                    if (pet == null) pet = PotionEffectType.getByKey(NamespacedKey.minecraft(effName.toLowerCase()));
                    if (pet != null) {
                        try {
                            int level = Integer.parseInt(parts[1]);
                            finalItem.setPotionEffect(pet, Math.min(250, level));
                            applyAndSync(held, finalItem);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#34D399✔ Added potion effect: &#CBD5E1" + formatName(pet.getName()) + " " + level + " (Max 250, 9999s)"));
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtil.color("&#FB7185Invalid level number."));
                        }
                    } else {
                        player.sendMessage(ColorUtil.color("&#FB7185Potion effect '" + parts[0] + "' not found."));
                    }
                }
                new PotionEffectGUI(plugin, player).open();
            });
            return;
        }

        // Effect slots in grid
        for (int i = 0; i < SLOTS.length; i++) {
            if (slot == SLOTS[i]) {
                int effectIndex = (page * SLOTS.length) + i;
                if (effectIndex < allEffects.size()) {
                    PotionEffectType pet = allEffects.get(effectIndex);

                    if (event.getClick() == ClickType.RIGHT) {
                        cItem.removePotionEffect(pet);
                        applyAndSync(held, cItem);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.0f);
                        player.sendMessage(ColorUtil.color("&#FB7185Removed effect: " + formatName(pet.getName())));
                        render();
                        return;
                    }

                    final CustomItem finalItem = cItem;
                    ChatInputListener.startPrompt(player, "Type level for " + formatName(pet.getName()) + " (1 - 250, duration 9999s):", input -> {
                        try {
                            int lvl = Integer.parseInt(input.trim());
                            finalItem.setPotionEffect(pet, Math.min(250, Math.max(1, lvl)));
                            applyAndSync(held, finalItem);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#34D399✔ Set " + formatName(pet.getName()) + " to level &#F472B6" + lvl + " &#64748B(Duration 9999s)"));
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtil.color("&#FB7185Invalid number. Must be between 1 and 250."));
                        }
                        new PotionEffectGUI(plugin, player).open();
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

    private String formatName(String raw) {
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
