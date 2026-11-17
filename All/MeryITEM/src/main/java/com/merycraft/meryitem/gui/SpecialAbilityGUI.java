package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemAbility;
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
 * Visual menu to select special weapon abilities and commands (14 MMOItems-grade skills).
 */
public class SpecialAbilityGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int[] ABILITY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    public SpecialAbilityGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#FBBF24§lMeryITEM &#334155» &#CBD5E1Special Abilities & Skills"));
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.4f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);

        ItemAbility[] abilities = ItemAbility.values();
        for (int i = 0; i < abilities.length && i < ABILITY_SLOTS.length; i++) {
            ItemAbility ab = abilities[i];
            boolean active = (cItem != null && cItem.getAbilities().contains(ab));

            Material mat;
            switch (ab) {
                case CELESTIAL_SLASH: mat = Material.NETHERITE_SWORD; break;
                case LIGHTNING_STRIKE: mat = Material.LIGHTNING_ROD; break;
                case HEAL_BURST: mat = Material.GLISTERING_MELON_SLICE; break;
                case SHADOW_STEP: mat = Material.ENDER_EYE; break;
                case TELEPORT: mat = Material.ENDER_PEARL; break;
                case PULL: mat = Material.FISHING_ROD; break;
                case FIRE_BURST: mat = Material.FIRE_CHARGE; break;
                case FROST_NOVA: mat = Material.PACKED_ICE; break;
                case METEOR_STRIKE: mat = Material.FIREWORK_STAR; break;
                case WIND_PULSE: mat = Material.ECHO_SHARD; break;
                case RUN_COMMAND: mat = Material.COMMAND_BLOCK; break;
                case VAMPIRIC_TOUCH: mat = Material.REDSTONE; break;
                case BERSERK: mat = Material.BLAZE_POWDER; break;
                case THORNS_AURA: mat = Material.SHIELD; break;
                default: mat = Material.NETHER_STAR; break;
            }

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&#94A3B8Trigger: &#CBD5E1" + ab.getTrigger()));
            lore.add(ColorUtil.color("&#94A3B8Cooldown: &#38BDF8" + (ab.getCooldownSeconds() > 0 ? ab.getCooldownSeconds() + "s" : "None")));
            lore.add(ColorUtil.color("&#94A3B8Description: &#CBD5E1" + ab.getDescription()));

            if (ab == ItemAbility.RUN_COMMAND && cItem != null && !cItem.getClickCommands().isEmpty()) {
                lore.add("");
                lore.add(ColorUtil.color("&#C084FCCommands configured (Hidden on weapon):"));
                for (String cmd : cItem.getClickCommands()) {
                    lore.add(ColorUtil.color("  &#94A3B8/ " + cmd));
                }
                lore.add(ColorUtil.color("&#FDE047Trigger Click: &#38BDF8[" + cItem.getCommandClickType() + "-CLICK]"));
                lore.add(ColorUtil.color("&#F472B6» Shift+Left-Click: Toggle [LEFT / RIGHT] Click"));
                lore.add(ColorUtil.color("&#34D399» Left-Click: Add / Set Command"));
                lore.add(ColorUtil.color("&#FB7185» Right-Click: Clear Commands"));
            } else {
                lore.add("");
                if (active) {
                    lore.add(ColorUtil.color("&#34D399✔ Attached to weapon (Working in-game!)"));
                    lore.add(ColorUtil.color("&#FB7185» Click to DETACH"));
                } else {
                    lore.add(ColorUtil.color("&#64748BNot attached"));
                    lore.add(ColorUtil.color("&#34D399» Click to ATTACH to weapon"));
                }
            }

            String title = (active ? "&#34D399§l✔ " : ab.getHexColor() + "✦ ") + ab.getDisplayName();
            inventory.setItem(ABILITY_SLOTS[i], createItem(mat, title, lore));
        }

        // Toggle Ability Lore visibility banner at Slot 31
        boolean showLore = (cItem != null && cItem.isShowAbilityLore());
        String loreState = showLore ? "&#34D399[VISIBLE ON WEAPON]" : "&#64748B[HIDDEN FROM WEAPON]";
        inventory.setItem(31, createItem(Material.BOOK, "&#FDE047📖 Ability Lore & Cooldown: " + loreState,
                Arrays.asList(
                        "&#94A3B8Control whether ability name and cooldown lines",
                        "&#94A3B8appear automatically in weapon description.",
                        "",
                        showLore ? "&#FB7185» Click to HIDE (Custom lore only)" : "&#34D399» Click to SHOW built-in ability lore"
                )));

        // Command Trigger Mode Toggle at Slot 39
        String cmdClick = (cItem != null) ? cItem.getCommandClickType() : "RIGHT";
        inventory.setItem(39, createItem(Material.CLOCK, "&#38BDF8⚡ Command Trigger: &#FDE047[" + cmdClick + "-CLICK]",
                Arrays.asList(
                        "&#94A3B8Toggle whether commands activate on Left or Right click",
                        "",
                        "&#34D399» Click to toggle [LEFT / RIGHT]"
                )));

        // Detach All Abilities at Slot 41
        inventory.setItem(41, createItem(Material.LAVA_BUCKET, "&#FB7185✖ Detach All Abilities",
                Arrays.asList(
                        "&#94A3B8Remove all special skills and commands from this item",
                        "",
                        "&#FB7185» Click to clear all"
                )));

        // Back button at Slot 49
        inventory.setItem(49, createItem(Material.ARROW, "&#F472B6« Back to Editor", Arrays.asList("&#94A3B8Return to item editor menu")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 49) {
            new ItemEditorGUI(plugin, player).open();
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

        // Toggle Ability Lore visibility (Slot 31)
        if (slot == 31) {
            cItem.setShowAbilityLore(!cItem.isShowAbilityLore());
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Ability Lore display: " + (cItem.isShowAbilityLore() ? "&#34D399VISIBLE" : "&#64748BHIDDEN")));
            render();
            return;
        }

        // Toggle Command Trigger Click (Slot 39)
        if (slot == 39) {
            String current = cItem.getCommandClickType();
            String next = current.equalsIgnoreCase("RIGHT") ? "LEFT" : "RIGHT";
            cItem.setCommandClickType(next);
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Command trigger set to &#38BDF8[" + next + "-CLICK]"));
            render();
            return;
        }

        // Detach All Abilities (Slot 41)
        if (slot == 41) {
            for (ItemAbility ab : new ArrayList<>(cItem.getAbilities())) {
                cItem.removeAbility(ab);
            }
            cItem.clearClickCommands();
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.0f);
            player.sendMessage(ColorUtil.color("&#FB7185Detached all abilities and commands."));
            render();
            return;
        }

        ItemAbility[] abilities = ItemAbility.values();
        for (int i = 0; i < abilities.length && i < ABILITY_SLOTS.length; i++) {
            if (slot == ABILITY_SLOTS[i]) {
                ItemAbility ab = abilities[i];

                if (ab == ItemAbility.RUN_COMMAND) {
                    if (event.isShiftClick()) {
                        String current = cItem.getCommandClickType();
                        String next = current.equalsIgnoreCase("RIGHT") ? "LEFT" : "RIGHT";
                        cItem.setCommandClickType(next);
                        applyAndSync(held, cItem);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                        player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Command trigger set to &#38BDF8[" + next + "-CLICK]"));
                        render();
                        return;
                    }

                    if (event.getClick() == ClickType.RIGHT) {
                        cItem.clearClickCommands();
                        cItem.removeAbility(ItemAbility.RUN_COMMAND);
                        applyAndSync(held, cItem);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 1.0f);
                        player.sendMessage(ColorUtil.color("&#FB7185Cleared all commands from item."));
                        render();
                        return;
                    }

                    final CustomItem finalItem = cItem;
                    ChatInputListener.startPrompt(player, "Type command to run (e.g. 'heal %player%' or 'console: say Hello'):", cmdInput -> {
                        finalItem.addClickCommand(cmdInput.trim());
                        finalItem.addAbility(ItemAbility.RUN_COMMAND);
                        applyAndSync(held, finalItem);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                        player.sendMessage(ColorUtil.color("&#34D399✔ Attached command: &#CBD5E1" + cmdInput + " &#64748B(Trigger: " + finalItem.getCommandClickType() + "-CLICK)"));
                        new SpecialAbilityGUI(plugin, player).open();
                    });
                    return;
                }

                if (cItem.getAbilities().contains(ab)) {
                    // Detach
                    cItem.removeAbility(ab);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 0.8f);
                    player.sendMessage(ColorUtil.color("&#FB7185Detached ability: " + ab.getDisplayName()));
                    applyAndSync(held, cItem);
                    render();
                } else {
                    // Attach
                    cItem.addAbility(ab);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#34D399✔ Attached ability: " + ab.getDisplayName()));
                    applyAndSync(held, cItem);
                    render();
                }
                return;
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
