package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemAbility;
import com.merycraft.meryitem.item.ItemStat;
import com.merycraft.meryitem.upgrade.UpgradeRecipe;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
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
 * Visual Upgrade & Smithing Station GUI (54 slots).
 * Allows players to select any item from their inventory to upgrade, view live before/after previews,
 * apply custom materials/gems, and create or edit upgrade recipes independently.
 */
public class UpgradeStationGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    private int targetInventorySlot = -1;
    private boolean selectingItem = false;

    private static final int[] GRID_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int[] RECIPE_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public UpgradeStationGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#38BDF8§lMeryITEM &#334155» &#CBD5E1Upgrade & Smithing"));

        // If player is holding an item in main hand, pre-select it
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held != null && !held.getType().isAir()) {
            this.targetInventorySlot = player.getInventory().getHeldItemSlot();
            this.selectingItem = false;
        } else {
            this.targetInventorySlot = -1;
            this.selectingItem = true; // Open directly in Item Selection Mode!
        }
    }

    public UpgradeStationGUI(MeryItemPlugin plugin, Player player, int targetInventorySlot) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#38BDF8§lMeryITEM &#334155» &#CBD5E1Upgrade & Smithing"));
        this.targetInventorySlot = targetInventorySlot;
        this.selectingItem = false;
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        if (selectingItem) {
            renderItemSelection();
        } else {
            renderUpgradeView();
        }
    }

    /**
     * Mode B: Show all upgradeable items in player's inventory to choose from.
     */
    private void renderItemSelection() {
        // Header at Slot 4
        inventory.setItem(4, createItem(Material.CHEST, "&#FDE047📦 Select Item to Upgrade",
                Arrays.asList(
                        "&#94A3B8Click any weapon, armor, or item from below",
                        "&#94A3B8to place it into the Upgrade Station.",
                        "",
                        "&#64748B(Displays items from your inventory)"
                )));

        int gridIdx = 0;
        for (int i = 0; i < 36 && gridIdx < GRID_SLOTS.length; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && !stack.getType().isAir()) {
                ItemStack displayStack = stack.clone();
                ItemMeta meta = displayStack.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("");
                    lore.add(ColorUtil.color("&#34D399» Click to SELECT this item for upgrade!"));
                    lore.add(ColorUtil.color("&#64748B(Inventory Slot: " + i + ")"));
                    meta.setLore(lore);
                    displayStack.setItemMeta(meta);
                }
                inventory.setItem(GRID_SLOTS[gridIdx], displayStack);
                gridIdx++;
            }
        }

        // Fill remaining slots
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        while (gridIdx < GRID_SLOTS.length) {
            inventory.setItem(GRID_SLOTS[gridIdx], border);
            gridIdx++;
        }

        // Bottom Controls
        if (targetInventorySlot != -1) {
            inventory.setItem(49, createItem(Material.ARROW, "&#F472B6« Back to Upgrades", Arrays.asList("&#94A3B8Return to upgrade station")));
        } else {
            inventory.setItem(49, createItem(Material.OAK_DOOR, "&#FB7185✖ Close Menu", Arrays.asList("&#94A3B8Close upgrade station")));
        }
    }

    /**
     * Mode A: Show the target item and all available upgrade materials/recipes.
     */
    private void renderUpgradeView() {
        ItemStack targetItem = (targetInventorySlot >= 0 && targetInventorySlot < player.getInventory().getSize())
                ? player.getInventory().getItem(targetInventorySlot)
                : null;

        if (targetItem == null || targetItem.getType().isAir()) {
            selectingItem = true;
            renderItemSelection();
            return;
        }

        CustomItem cItem = plugin.getItemManager().getFromItemStack(targetItem);

        // Top Row:
        // Slot 2: Create Custom Recipe
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String detectedMat = (offHand != null && !offHand.getType().isAir())
                ? (offHand.hasItemMeta() && offHand.getItemMeta().hasDisplayName() ? offHand.getItemMeta().getDisplayName() : offHand.getType().name())
                : "DIAMOND";
        inventory.setItem(2, createItem(Material.ANVIL, "&#FDE047+ Create Upgrade Recipe",
                Arrays.asList(
                        "&#94A3B8Create a new upgrade material/recipe",
                        "&#94A3B8Detected off-hand: &#38BDF8" + detectedMat,
                        "",
                        "&#34D399» Click to create new recipe"
                )));

        // Slot 4: Change Selected Item
        inventory.setItem(4, createItem(Material.HOPPER, "&#38BDF8🔄 Change Target Item",
                Arrays.asList(
                        "&#94A3B8Currently upgrading slot #" + targetInventorySlot,
                        "",
                        "&#34D399» Click to select another item from inventory"
                )));

        // Slot 6: Reset Default Recipes
        inventory.setItem(6, createItem(Material.LAVA_BUCKET, "&#FB7185✖ Reset Defaults",
                Arrays.asList(
                        "&#94A3B8Reset all upgrade recipes back to factory defaults",
                        "",
                        "&#FB7185» Click to reset"
                )));

        // Slot 13: Target Item Live Preview
        ItemStack preview = (cItem != null) ? cItem.buildItemStack(targetItem.getAmount()) : targetItem.clone();
        ItemMeta pMeta = preview.getItemMeta();
        if (pMeta != null) {
            List<String> lore = pMeta.hasLore() ? new ArrayList<>(pMeta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ColorUtil.color("&#FDE047✦ ITEM IN UPGRADE ANVIL ✦"));
            lore.add(ColorUtil.color("&#94A3B8Select an upgrade material below to enhance!"));
            pMeta.setLore(lore);
            preview.setItemMeta(pMeta);
        }
        inventory.setItem(13, preview);

        // Display Recipes in Rows 3 & 4 (RECIPE_SLOTS)
        List<UpgradeRecipe> recipes = new ArrayList<>(plugin.getUpgradeManager().getAllRecipes());
        for (int i = 0; i < recipes.size() && i < RECIPE_SLOTS.length; i++) {
            UpgradeRecipe r = recipes.get(i);
            Material iconMat = Material.matchMaterial(r.getRequiredMaterialOrName());
            if (iconMat == null) iconMat = Material.NETHER_STAR;

            int playerHas = countMaterial(player, r.getRequiredMaterialOrName());
            boolean canAfford = playerHas >= r.getRequiredAmount();

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&#94A3B8Required Material: &#CBD5E1" + r.getRequiredAmount() + "x " + r.getRequiredMaterialOrName()));
            lore.add(ColorUtil.color("&#94A3B8You Have: " + (canAfford ? "&#34D399" : "&#FB7185") + playerHas + "x"));
            lore.add("");
            lore.add(ColorUtil.color("&#FDE047Upgrade Benefit:"));
            if (r.getTargetStat() != null) {
                lore.add(ColorUtil.color("  &#34D399+" + r.getStatBonus() + " " + r.getTargetStat().getDisplayName()));
                if (cItem != null) {
                    double curr = cItem.getStat(r.getTargetStat());
                    lore.add(ColorUtil.color("  &#64748B(Current: " + curr + " ➔ &#34D399" + (curr + r.getStatBonus()) + "&#64748B)"));
                }
            }
            if (r.getAttachCommand() != null) {
                lore.add(ColorUtil.color("  &#C084FC⚡ Attaches Command: &#94A3B8/" + r.getAttachCommand()));
            }
            lore.add("");
            if (canAfford) {
                lore.add(ColorUtil.color("&#34D399» Left-Click: UPGRADE THIS ITEM!"));
            } else {
                lore.add(ColorUtil.color("&#FB7185✖ Not enough materials!"));
            }
            lore.add(ColorUtil.color("&#38BDF8» Shift+Left-Click: EDIT recipe (Cost / Bonus)"));
            lore.add(ColorUtil.color("&#F87171» Right-Click: Delete recipe"));

            String title = (canAfford ? "&#34D399§l" : "&#64748B") + r.getId().replace("_", " ");
            inventory.setItem(RECIPE_SLOTS[i], createItem(iconMat, title, lore));
        }

        // Close Menu at Slot 49
        inventory.setItem(49, createItem(Material.OAK_DOOR, "&#FB7185✖ Close Station", Arrays.asList("&#94A3B8Close upgrade station")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (selectingItem) {
            handleItemSelectionClick(event, slot);
        } else {
            handleUpgradeClick(event, slot);
        }
    }

    private void handleItemSelectionClick(InventoryClickEvent event, int slot) {
        // Back / Close
        if (slot == 49) {
            if (targetInventorySlot != -1) {
                selectingItem = false;
                render();
            } else {
                player.closeInventory();
            }
            return;
        }

        // Find which inventory item was clicked
        for (int gridIdx = 0; gridIdx < GRID_SLOTS.length; gridIdx++) {
            if (slot == GRID_SLOTS[gridIdx]) {
                // Find matching item in inventory
                int currentGrid = 0;
                for (int invSlot = 0; invSlot < 36; invSlot++) {
                    ItemStack stack = player.getInventory().getItem(invSlot);
                    if (stack != null && !stack.getType().isAir()) {
                        if (currentGrid == gridIdx) {
                            // Selected this item!
                            this.targetInventorySlot = invSlot;
                            this.selectingItem = false;
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#34D399✔ Selected item from slot #" + invSlot + " for upgrade!"));
                            render();
                            return;
                        }
                        currentGrid++;
                    }
                }
            }
        }
    }

    private void handleUpgradeClick(InventoryClickEvent event, int slot) {
        // Change Target Item (Slot 4)
        if (slot == 4) {
            this.selectingItem = true;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            render();
            return;
        }

        // Reset Defaults (Slot 6)
        if (slot == 6) {
            plugin.getUpgradeManager().resetToDefaults();
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.0f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Reset upgrade recipes to default."));
            render();
            return;
        }

        // Close (Slot 49)
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Create Custom Recipe Wizard (Slot 2)
        if (slot == 2) {
            openRecipeCreationWizard();
            return;
        }

        // Recipe Grid
        ItemStack targetItem = (targetInventorySlot >= 0 && targetInventorySlot < player.getInventory().getSize())
                ? player.getInventory().getItem(targetInventorySlot)
                : null;

        if (targetItem == null || targetItem.getType().isAir()) {
            player.sendMessage(ColorUtil.color("&#FB7185Target item is no longer in inventory!"));
            this.selectingItem = true;
            render();
            return;
        }

        CustomItem cItem = plugin.getItemManager().getFromItemStack(targetItem);
        if (cItem == null) {
            cItem = new CustomItem("ITEM_" + System.currentTimeMillis() % 100000, targetItem.getType(),
                    targetItem.hasItemMeta() && targetItem.getItemMeta().hasDisplayName() ? targetItem.getItemMeta().getDisplayName() : "&#F472B6✦ " + targetItem.getType().name(),
                    com.merycraft.meryitem.item.ItemRarity.COMMON, com.merycraft.meryitem.item.ItemType.SWORD);
            cItem.setShowRarity(false);
        }

        List<UpgradeRecipe> recipes = new ArrayList<>(plugin.getUpgradeManager().getAllRecipes());
        for (int i = 0; i < recipes.size() && i < RECIPE_SLOTS.length; i++) {
            if (slot == RECIPE_SLOTS[i]) {
                UpgradeRecipe r = recipes.get(i);

                // Right-Click: Delete recipe
                if (event.getClick().isRightClick()) {
                    plugin.getUpgradeManager().removeRecipe(r.getId());
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                    player.sendMessage(ColorUtil.color("&#FB7185Deleted upgrade recipe: " + r.getId()));
                    render();
                    return;
                }

                // Shift-Click: Edit recipe
                if (event.isShiftClick()) {
                    final CustomItem finalCItem = cItem;
                    ChatInputListener.startPrompt(player, "Type new required cost amount for " + r.getId() + " (Current: " + r.getRequiredAmount() + "):", costInput -> {
                        int cost = r.getRequiredAmount();
                        try { cost = Integer.parseInt(costInput.trim()); } catch (NumberFormatException ignored) {}
                        final int finalCost = Math.max(1, cost);
                        ChatInputListener.startPrompt(player, "Type new stat bonus amount (Current: " + r.getStatBonus() + "):", bonusInput -> {
                            double bonus = r.getStatBonus();
                            try { bonus = Double.parseDouble(bonusInput.trim()); } catch (NumberFormatException ignored) {}
                            UpgradeRecipe updated = new UpgradeRecipe(r.getId(), r.getRequiredMaterialOrName(), finalCost, r.getTargetStat(), bonus, r.getAttachCommand(), "+" + bonus + " " + (r.getTargetStat() != null ? r.getTargetStat().getDisplayName() : "Bonus"));
                            plugin.getUpgradeManager().addRecipe(updated);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#34D399✔ Updated recipe: " + r.getId()));
                            new UpgradeStationGUI(plugin, player, targetInventorySlot).open();
                        });
                    });
                    return;
                }

                // Normal Click: Apply upgrade!
                int playerHas = countMaterial(player, r.getRequiredMaterialOrName());
                if (playerHas < r.getRequiredAmount()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
                    player.sendMessage(ColorUtil.color("&#FB7185You need &#F43F5E" + r.getRequiredAmount() + "x " + r.getRequiredMaterialOrName() + " &#FB7185to apply this upgrade."));
                    return;
                }

                // Deduct material
                removeMaterial(player, r.getRequiredMaterialOrName(), r.getRequiredAmount());

                // Apply stat bonus
                if (r.getTargetStat() != null) {
                    double curr = cItem.getStat(r.getTargetStat());
                    cItem.setStat(r.getTargetStat(), curr + r.getStatBonus());
                }

                // Attach command if configured
                if (r.getAttachCommand() != null) {
                    cItem.addClickCommand(r.getAttachCommand());
                    cItem.addAbility(ItemAbility.RUN_COMMAND);
                }

                // Update item in player's target slot
                ItemStack upgradedStack = cItem.buildItemStack(targetItem.getAmount());
                player.getInventory().setItem(targetInventorySlot, upgradedStack);

                // Auto-sync with catalog if saved item
                if (plugin.getItemManager().getItem(cItem.getId()) != null) {
                    plugin.getItemManager().saveCustomItem(cItem);
                }

                // FX
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.9f, 1.3f);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.4f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage(ColorUtil.color("&#34D399✨ Successfully upgraded item! " + r.getDescription()));
                render();
                return;
            }
        }
    }

    public void openRecipeCreationWizard() {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String defaultMat = (offHand != null && !offHand.getType().isAir()) ?
                (offHand.hasItemMeta() && offHand.getItemMeta().hasDisplayName() ? offHand.getItemMeta().getDisplayName() : offHand.getType().name())
                : "DIAMOND";

        player.sendMessage(ColorUtil.color("&#38BDF8[💡 TIP] Put any custom material/item in your OFF-HAND to auto-detect its name!"));
        ChatInputListener.startPrompt(player, "Type new Upgrade ID (e.g. 'FIRE_ORB_BOOST'):", idInput -> {
            String id = idInput.trim().toUpperCase().replace(" ", "_");
            ChatInputListener.startPrompt(player, "Type required material name (or press Enter for: " + defaultMat + "):", matInput -> {
                String mat = matInput.trim().isEmpty() ? defaultMat : matInput.trim();
                ChatInputListener.startPrompt(player, "Type required amount (e.g. '4'):", costInput -> {
                    int cost = 1;
                    try { cost = Integer.parseInt(costInput.trim()); } catch (NumberFormatException ignored) {}
                    final int finalCost = Math.max(1, cost);
                    ChatInputListener.startPrompt(player, "Type stat to upgrade (e.g. 'ATTACK_DAMAGE', 'DEFENSE', 'CRITICAL_DAMAGE' or 'CMD' for command):", statInput -> {
                        if (statInput.equalsIgnoreCase("CMD")) {
                            ChatInputListener.startPrompt(player, "Type command to attach (e.g. 'heal %player%'):", cmdInput -> {
                                UpgradeRecipe newRecipe = new UpgradeRecipe(id, mat, finalCost, null, 0, cmdInput.trim(), "Attaches Command: /" + cmdInput.trim());
                                plugin.getUpgradeManager().addRecipe(newRecipe);
                                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.4f);
                                player.sendMessage(ColorUtil.color("&#34D399✔ Successfully created custom upgrade recipe: &#CBD5E1" + id));
                                new UpgradeStationGUI(plugin, player, targetInventorySlot).open();
                            });
                        } else {
                            ItemStat stat = ItemStat.fromString(statInput.trim());
                            if (stat == null) stat = ItemStat.ATTACK_DAMAGE;
                            final ItemStat finalStat = stat;
                            ChatInputListener.startPrompt(player, "Type bonus amount (e.g. '15' or '25.5'):", bonusInput -> {
                                double bonus = 10.0;
                                try { bonus = Double.parseDouble(bonusInput.trim()); } catch (NumberFormatException ignored) {}
                                UpgradeRecipe newRecipe = new UpgradeRecipe(id, mat, finalCost, finalStat, bonus, null, "+" + bonus + " " + finalStat.getDisplayName());
                                plugin.getUpgradeManager().addRecipe(newRecipe);
                                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.4f);
                                player.sendMessage(ColorUtil.color("&#34D399✔ Successfully created custom upgrade recipe: &#CBD5E1" + id));
                                new UpgradeStationGUI(plugin, player, targetInventorySlot).open();
                            });
                        }
                    });
                });
            });
        });
    }

    private int countMaterial(Player p, String matOrName) {
        Material mat = Material.matchMaterial(matOrName);
        int total = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null) {
                if (mat != null && item.getType() == mat) {
                    total += item.getAmount();
                } else if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().toLowerCase().contains(matOrName.toLowerCase())) {
                    total += item.getAmount();
                }
            }
        }
        return total;
    }

    private void removeMaterial(Player p, String matOrName, int amount) {
        Material mat = Material.matchMaterial(matOrName);
        int remaining = amount;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null) {
                boolean match = (mat != null && item.getType() == mat) ||
                        (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().toLowerCase().contains(matOrName.toLowerCase()));
                if (match) {
                    int stackAmt = item.getAmount();
                    if (stackAmt <= remaining) {
                        remaining -= stackAmt;
                        p.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(stackAmt - remaining);
                        remaining = 0;
                    }
                    if (remaining <= 0) break;
                }
            }
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
