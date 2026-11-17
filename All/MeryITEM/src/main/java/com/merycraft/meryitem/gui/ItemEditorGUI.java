package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemRarity;
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
 * Interactive In-Hand Item Editor GUI (100% English, fully polished and balanced layout).
 */
public class ItemEditorGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public ItemEditorGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, ColorUtil.color("&#F472B6§lMeryITEM &#334155» &#CBD5E1Item Editor"));
    }

    public void open() {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand first!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
            return;
        }

        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.4f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, border);
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        CustomItem cItem = getOrCreateHeldCustomItem(held);

        // Preview item at Slot 13
        ItemStack preview = cItem.buildItemStack(1);
        inventory.setItem(13, preview);

        // 1. Edit Name / Title (Paper) at Slot 19
        inventory.setItem(19, createItem(Material.PAPER, "&#F472B6✏ Edit Title (Name)",
                Arrays.asList(
                        "&#94A3B8Current: " + (cItem.getDisplayName() != null ? cItem.getDisplayName() : "&#64748BDefault"),
                        "",
                        "&#34D399» Left-Click: Type new name in chat",
                        "&#FB7185» Right-Click: Reset name to default",
                        "&#64748B(Supports hex &#RRGGBB & color codes)"
                )));

        // 2. Manage Description (Writable Book) at Slot 20
        inventory.setItem(20, createItem(Material.WRITABLE_BOOK, "&#FDA4AF📝 Manage Description",
                Arrays.asList(
                        "&#94A3B8Lines count: &#F472B6" + cItem.getCustomLore().size(),
                        "",
                        "&#38BDF8» Click to open Description Manager",
                        "&#64748B(Edit, add, or delete specific lines individually)"
                )));

        // 3. Enchantments (Enchanting Table) at Slot 21
        inventory.setItem(21, createItem(Material.ENCHANTING_TABLE, "&#C084FC🔮 Enchantments (Max 255)",
                Arrays.asList(
                        "&#94A3B8Active enchantments: &#C084FC" + cItem.getEnchantments().size(),
                        "",
                        "&#38BDF8» Click to open Enchantment Editor",
                        "&#64748B(Supports ALL enchantments up to level 255)"
                )));

        // 4. Potion Effects (Potion) at Slot 22
        inventory.setItem(22, createItem(Material.POTION, "&#34D399🧪 Potion Effects (Max 250, 9999s)",
                Arrays.asList(
                        "&#94A3B8Active effects: &#34D399" + cItem.getPotionEffects().size(),
                        "&#94A3B8Trigger: &#38BDF8[" + cItem.getPotionTrigger() + "]",
                        "",
                        "&#38BDF8» Click to open Potion Effects Editor"
                )));

        // 5. Weapon Aura Particle Effect (Nether Star) at Slot 23
        inventory.setItem(23, createItem(Material.NETHER_STAR, "&#FDE047✨ Weapon Aura Effect",
                Arrays.asList(
                        "&#94A3B8Current Effect: " + cItem.getEffect().getHexColor() + cItem.getEffect().getDisplayName(),
                        "&#94A3B8Motion Pattern: &#38BDF8" + cItem.getParticleMotion().getDisplayName(),
                        "",
                        "&#38BDF8» Click to open Particle Studio"
                )));

        // 6. Custom RPG Stats (Iron Sword) at Slot 24
        inventory.setItem(24, createItem(Material.IRON_SWORD, "&#F87171⚔ 100+ Custom RPG Stats",
                Arrays.asList(
                        "&#94A3B8Configure over 100+ RPG custom stats",
                        "",
                        "&#38BDF8» Click to adjust RPG stats"
                )));

        // 7. Special Abilities & Skills (Command Block) at Slot 25
        inventory.setItem(25, createItem(Material.COMMAND_BLOCK, "&#FBBF24⚡ Special Abilities & Skills",
                Arrays.asList(
                        "&#94A3B8Attach 14 MMO skills, Warp/TP, Commands, etc.",
                        "",
                        "&#38BDF8» Click to manage special abilities"
                )));

        // Row 3 (Slots 28 to 34):
        // 8. Upgrade & Smithing Station (Anvil) at Slot 28
        inventory.setItem(28, createItem(Material.ANVIL, "&#38BDF8🔨 Upgrade & Smithing Station",
                Arrays.asList(
                        "&#94A3B8Upgrade weapon using ANY material held in hand!",
                        "&#64748B(Custom recipes, stats, or commands)",
                        "",
                        "&#38BDF8» Click to open Upgrade Station"
                )));

        // 9. Rarity & Tag Toggle (Amethyst Shard) at Slot 29
        String rarityState = cItem.isShowRarity() ? "&#34D399[ENABLED]" : "&#64748B[DISABLED]";
        inventory.setItem(29, createItem(Material.AMETHYST_SHARD, "&#C084FC💠 Rarity Display: " + rarityState,
                Arrays.asList(
                        "&#94A3B8Current Tier: " + cItem.getRarity().getFormattedName(),
                        "",
                        "&#34D399» Left-Click: Cycle Rarity (Common ➔ Divine)",
                        "&#FBBF24» Right-Click: Toggle Rarity Display [ON / OFF]"
                )));

        // 10. Ability Lore Toggle (Book) at Slot 30
        String abLoreState = cItem.isShowAbilityLore() ? "&#34D399[VISIBLE]" : "&#64748B[HIDDEN]";
        inventory.setItem(30, createItem(Material.BOOK, "&#FDE047📖 Ability Lore: " + abLoreState,
                Arrays.asList(
                        "&#94A3B8Show/hide ability name and cooldown in lore",
                        "",
                        "&#34D399» Click to toggle [VISIBLE / HIDDEN]"
                )));

        // 11. Unbreakable Toggle (Diamond) at Slot 31
        String unbState = cItem.isUnbreakable() ? "&#34D399[TRUE]" : "&#64748B[FALSE]";
        inventory.setItem(31, createItem(Material.DIAMOND, "&#38BDF8💎 Unbreakable: " + unbState,
                Arrays.asList(
                        "&#94A3B8Infinite durability for weapons and armor",
                        "",
                        "&#34D399» Click to toggle"
                )));

        // 12. Glowing Glint Toggle (Glowstone Dust) at Slot 32
        String glowState = cItem.isGlowing() ? "&#34D399[TRUE]" : "&#64748B[FALSE]";
        inventory.setItem(32, createItem(Material.GLOWSTONE_DUST, "&#FACC15🌟 Glowing Glint: " + glowState,
                Arrays.asList(
                        "&#94A3B8Enchantment shimmer shine effect",
                        "",
                        "&#34D399» Click to toggle"
                )));

        // 13. Custom Model Data at Slot 33
        inventory.setItem(33, createItem(Material.ITEM_FRAME, "&#CBD5E1🎨 Custom Model Data: &#F472B6" + cItem.getCustomModelData(),
                Arrays.asList(
                        "&#94A3B8Set custom texture / model ID for resource packs",
                        "",
                        "&#34D399» Left-Click: Type new Model Data in chat",
                        "&#FB7185» Right-Click: Reset to 0"
                )));

        // 14. Save to Catalog (Emerald) at Slot 34
        boolean isTemp = cItem.getId().startsWith("ITEM_") && plugin.getItemManager().getItem(cItem.getId()) == null;
        List<String> saveLore = new ArrayList<>();
        saveLore.add(ColorUtil.color("&#94A3B8Current ID: &#CBD5E1" + cItem.getId()));
        saveLore.add(ColorUtil.color("&#94A3B8Folder: &#FDE047" + cItem.getFolder()));
        saveLore.add("");
        if (!isTemp) {
            saveLore.add(ColorUtil.color("&#34D399» Left-Click: Quick SAVE changes immediately"));
            saveLore.add(ColorUtil.color("&#38BDF8» Right-Click: Rename / Save with new ID in chat"));
        } else {
            saveLore.add(ColorUtil.color("&#34D399» Click to set ID and save to catalog!"));
        }
        inventory.setItem(34, createItem(Material.EMERALD, "&#34D399💾 Save to Catalog", saveLore));

        // 15. Folder / Category (Chest) at Slot 38
        inventory.setItem(38, createItem(Material.CHEST, "&#FDE047📁 Folder: &#CBD5E1" + cItem.getFolder(),
                Arrays.asList(
                        "&#94A3B8Group this item into custom folder / category",
                        "&#94A3B8Current: &#FDE047" + cItem.getFolder(),
                        "",
                        "&#34D399» Left-Click: Type folder name in chat",
                        "&#FB7185» Right-Click: Reset folder to 'Default'"
                )));

        // 16. Hide Enchants Toggle (Enchanted Book) at Slot 39
        String hideEnchState = cItem.isHideEnchants() ? "&#FB7185[HIDDEN]" : "&#34D399[VISIBLE]";
        inventory.setItem(39, createItem(Material.ENCHANTED_BOOK, "&#C084FC🔮 Enchant Display: " + hideEnchState,
                Arrays.asList(
                        "&#94A3B8Hide enchantment names and levels from lore",
                        "&#64748B(Enchantments remain 100% active in gameplay)",
                        "",
                        "&#34D399» Click to toggle [VISIBLE / HIDDEN]"
                )));

        // Close button at Slot 40
        inventory.setItem(40, createItem(Material.OAK_DOOR, "&#FB7185✖ Close Editor", Arrays.asList("&#94A3B8Close this window")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand first!"));
            player.closeInventory();
            return;
        }

        CustomItem cItem = getOrCreateHeldCustomItem(held);

        // 1. Edit Name (Slot 19)
        if (slot == 19) {
            if (event.getClick() == ClickType.RIGHT) {
                cItem.setDisplayName(null);
                applyAndSync(held, cItem);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.0f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Reset item name to default."));
                render();
                return;
            }

            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type the new title for your item (Supports hex &#RRGGBB & color codes):", input -> {
                finalItem.setDisplayName(input.trim());
                applyAndSync(held, finalItem);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Item name updated to: " + input.trim()));
                new ItemEditorGUI(plugin, player).open();
            });
            return;
        }

        // 2. Manage Description (Slot 20)
        if (slot == 20) {
            new LoreManagerGUI(plugin, player).open();
            return;
        }

        // 3. Enchantments (Slot 21)
        if (slot == 21) {
            new EnchantSelectorGUI(plugin, player).open();
            return;
        }

        // 4. Potion Effects (Slot 22)
        if (slot == 22) {
            new PotionEffectGUI(plugin, player).open();
            return;
        }

        // 5. Weapon Aura Effect (Slot 23)
        if (slot == 23) {
            new EffectSelectorGUI(plugin, player).open();
            return;
        }

        // 6. RPG Stats (Slot 24)
        if (slot == 24) {
            new StatEditorGUI(plugin, player).open();
            return;
        }

        // 7. Special Abilities & Commands (Slot 25)
        if (slot == 25) {
            new SpecialAbilityGUI(plugin, player).open();
            return;
        }

        // 8. Upgrade Station (Slot 28)
        if (slot == 28) {
            new UpgradeStationGUI(plugin, player).open();
            return;
        }

        // 9. Rarity Toggle (Slot 29)
        if (slot == 29) {
            if (event.getClick() == ClickType.RIGHT) {
                cItem.setShowRarity(!cItem.isShowRarity());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Rarity display: " + (cItem.isShowRarity() ? "&#34D399ENABLED" : "&#64748BDISABLED")));
            } else {
                ItemRarity[] rarities = ItemRarity.values();
                int nextIndex = (cItem.getRarity().ordinal() + 1) % rarities.length;
                cItem.setRarity(rarities[nextIndex]);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.3f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Rarity set to: " + cItem.getRarity().getFormattedName()));
            }
            applyAndSync(held, cItem);
            render();
            return;
        }

        // 10. Ability Lore Toggle (Slot 30)
        if (slot == 30) {
            cItem.setShowAbilityLore(!cItem.isShowAbilityLore());
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Ability Lore display: " + (cItem.isShowAbilityLore() ? "&#34D399VISIBLE" : "&#64748BHIDDEN")));
            render();
            return;
        }

        // 11. Unbreakable Toggle (Slot 31)
        if (slot == 31) {
            cItem.setUnbreakable(!cItem.isUnbreakable());
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Unbreakable set to: " + (cItem.isUnbreakable() ? "&#34D399TRUE" : "&#64748BFALSE")));
            render();
            return;
        }

        // 12. Glowing Toggle (Slot 32)
        if (slot == 32) {
            cItem.setGlowing(!cItem.isGlowing());
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Glowing set to: " + (cItem.isGlowing() ? "&#34D399TRUE" : "&#64748BFALSE")));
            render();
            return;
        }

        // 13. Custom Model Data (Slot 33)
        if (slot == 33) {
            if (event.getClick() == ClickType.RIGHT) {
                cItem.setCustomModelData(0);
                applyAndSync(held, cItem);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.0f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Custom Model Data reset to 0."));
                render();
                return;
            }

            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type Custom Model Data number (e.g. 1001):", input -> {
                try {
                    int cmd = Integer.parseInt(input.trim());
                    finalItem.setCustomModelData(Math.max(0, cmd));
                    applyAndSync(held, finalItem);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Custom Model Data set to: &#F472B6" + cmd));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&#FB7185Invalid number. Must be an integer."));
                }
                new ItemEditorGUI(plugin, player).open();
            });
            return;
        }

        // 14. Save to Catalog (Slot 34)
        if (slot == 34) {
            final CustomItem finalItem = cItem;
            boolean isTemporary = finalItem.getId().startsWith("ITEM_") && plugin.getItemManager().getItem(finalItem.getId()) == null;

            // If item already has a registered custom ID, Left-Click saves immediately!
            if (!isTemporary && event.getClick() != ClickType.RIGHT) {
                applyAndSync(held, finalItem);
                plugin.getItemManager().saveCustomItem(finalItem);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#34D399✔ Saved changes to item &#CBD5E1" + finalItem.getId() + " &#34D399in catalog!"));
                render();
                return;
            }

            ChatInputListener.startPrompt(player, "Type ID to save this item as in catalog (e.g. 'MY_LEGENDARY_SWORD'):", idInput -> {
                String cleanId = idInput.trim().toUpperCase().replace(" ", "_");
                finalItem.setId(cleanId);
                applyAndSync(held, finalItem);
                plugin.getItemManager().saveCustomItem(finalItem);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#34D399✔ Successfully saved item to catalog with ID: &#CBD5E1" + cleanId));
                new ItemEditorGUI(plugin, player).open();
            });
            return;
        }

        // 15. Folder / Category (Slot 38)
        if (slot == 38) {
            if (event.getClick() == ClickType.RIGHT) {
                cItem.setFolder("Default");
                applyAndSync(held, cItem);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.0f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Folder reset to: &#FDE047Default"));
                render();
                return;
            }

            final CustomItem finalItem = cItem;
            ChatInputListener.startPrompt(player, "Type folder / category name for this item (e.g. 'Weapons' or 'Boss_Drops'):", fInput -> {
                String folderName = fInput.trim().replace(" ", "_");
                if (folderName.isEmpty()) folderName = "Default";
                finalItem.setFolder(folderName);
                plugin.getItemManager().createFolder(folderName);
                applyAndSync(held, finalItem);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Item assigned to folder: &#FDE047" + folderName));
                new ItemEditorGUI(plugin, player).open();
            });
            return;
        }

        // 16. Hide Enchants Toggle (Slot 39)
        if (slot == 39) {
            cItem.setHideEnchants(!cItem.isHideEnchants());
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Enchantment display in lore: "
                    + (cItem.isHideEnchants() ? "&#FB7185HIDDEN (Enchants still active)" : "&#34D399VISIBLE")));
            render();
            return;
        }

        // Close Menu (Slot 40)
        if (slot == 40) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f);
        }
    }

    private void applyAndSync(ItemStack held, CustomItem cItem) {
        player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
        if (plugin.getItemManager().getItem(cItem.getId()) != null) {
            plugin.getItemManager().saveCustomItem(cItem);
        }
    }

    private CustomItem getOrCreateHeldCustomItem(ItemStack held) {
        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);
        if (cItem == null) {
            String id = "ITEM_" + (System.currentTimeMillis() % 100000);
            String name = held.hasItemMeta() && held.getItemMeta().hasDisplayName()
                    ? held.getItemMeta().getDisplayName()
                    : "&#F472B6✦ " + held.getType().name();
            ItemType inferredType = inferType(held.getType());
            cItem = new CustomItem(id, held.getType(), name, ItemRarity.COMMON, inferredType);
            cItem.setShowRarity(false);
        }
        return cItem;
    }

    private ItemType inferType(Material mat) {
        String name = mat.name();
        if (name.contains("SWORD") || name.contains("AXE")) {
            return ItemType.SWORD;
        } else if (name.contains("BOW")) {
            return ItemType.BOW;
        } else if (name.contains("CROSSBOW")) {
            return ItemType.CROSSBOW;
        } else if (name.contains("SHIELD")) {
            return ItemType.SHIELD;
        } else if (name.contains("HELMET")) {
            return ItemType.HELMET;
        } else if (name.contains("CHESTPLATE")) {
            return ItemType.CHESTPLATE;
        } else if (name.contains("LEGGINGS")) {
            return ItemType.LEGGINGS;
        } else if (name.contains("BOOTS")) {
            return ItemType.BOOTS;
        }
        return ItemType.MISC;
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
