package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.effect.ItemEffect;
import com.merycraft.meryitem.effect.ParticleMotion;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemRarity;
import com.merycraft.meryitem.item.ItemType;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MMOItems-grade Particle Studio GUI (Choose Particle Style + Motion Type).
 */
public class EffectSelectorGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int[] PARTICLE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31
    };

    public EffectSelectorGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&#F472B6§lMeryITEM &#334155» &#CBD5E1MMOItems Particle Studio"));
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.3f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);
        ItemEffect currentEffect = (cItem != null) ? cItem.getEffect() : ItemEffect.NONE;
        ParticleMotion currentMotion = (cItem != null) ? cItem.getParticleMotion() : ParticleMotion.AURA;

        ItemEffect[] allEffects = ItemEffect.values();
        for (int i = 0; i < allEffects.length && i < PARTICLE_SLOTS.length; i++) {
            ItemEffect eff = allEffects[i];
            if (eff == ItemEffect.NONE) continue;

            boolean isCurrent = (eff == currentEffect);

            Material mat;
            switch (eff) {
                case SAKURA: mat = Material.CHERRY_SAPLING; break;
                case FLAME: mat = Material.BLAZE_POWDER; break;
                case SOUL_FLAME: mat = Material.SOUL_TORCH; break;
                case STARLIGHT: mat = Material.END_ROD; break;
                case VOID: mat = Material.ENDER_EYE; break;
                case DIVINE: mat = Material.TOTEM_OF_UNDYING; break;
                case CRIT: mat = Material.GLOWSTONE_DUST; break;
                case HEART: mat = Material.POPPY; break;
                case DRAGON_BREATH: mat = Material.DRAGON_BREATH; break;
                case ENCHANT: mat = Material.BOOK; break;
                case SMOKE: mat = Material.GUNPOWDER; break;
                case ELECTRIC: mat = Material.LIGHTNING_ROD; break;
                case GLOW: mat = Material.GLOW_INK_SAC; break;
                case NAUTILUS: mat = Material.NAUTILUS_SHELL; break;
                case SCULK_SOUL: mat = Material.ECHO_SHARD; break;
                case SONIC_BOOM: mat = Material.DISC_FRAGMENT_5; break;
                case FROST: mat = Material.SNOWBALL; break;
                case FIREWORK: mat = Material.FIREWORK_ROCKET; break;
                default: mat = Material.NETHER_STAR; break;
            }

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&#94A3B8Particle type from MMOItems"));
            lore.add("");
            if (isCurrent) {
                lore.add(ColorUtil.color("&#34D399✔ Currently Selected Particle"));
            } else {
                lore.add(ColorUtil.color(eff.getHexColor() + "» Click to equip this particle"));
            }

            String title = (isCurrent ? "&#34D399§l✔ " : eff.getHexColor() + "✦ ") + eff.getDisplayName();
            inventory.setItem(PARTICLE_SLOTS[i], createItem(mat, title, lore));
        }

        // Row 4: Motion Type Selector (Slots 37 - 43)
        ParticleMotion[] motions = ParticleMotion.values();
        int[] motionSlots = {37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < motions.length && i < motionSlots.length; i++) {
            ParticleMotion pm = motions[i];
            boolean isSelected = (pm == currentMotion);

            Material mMat;
            switch (pm) {
                case HELIX: mMat = Material.STRING; break;
                case VORTEX: mMat = Material.FEATHER; break;
                case DOUBLE_RINGS: mMat = Material.COMPASS; break;
                case GALAXY: mMat = Material.NETHER_STAR; break;
                case FIREFLIES: mMat = Material.GLOWSTONE_DUST; break;
                case OFFSET: mMat = Material.IRON_SWORD; break;
                default: mMat = Material.CLOCK; break;
            }

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&#94A3B8Pattern: &#CBD5E1" + pm.getDescription()));
            lore.add("");
            if (isSelected) {
                lore.add(ColorUtil.color("&#34D399✔ Active Motion Pattern"));
            } else {
                lore.add(ColorUtil.color("&#38BDF8» Click to set pattern to " + pm.getDisplayName()));
            }

            String title = (isSelected ? "&#34D399§l[ " : "&#38BDF8") + pm.getDisplayName() + (isSelected ? " ]" : "");
            inventory.setItem(motionSlots[i], createItem(mMat, title, lore));
        }

        // Slot 47: Remove/Clear Particle
        inventory.setItem(47, createItem(Material.LAVA_BUCKET, "&#FB7185✖ Disable Particle",
                Arrays.asList("&#94A3B8Remove all particle auras from this weapon", "", "&#FB7185» Click to disable")));

        // Slot 49: Back to Editor
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

        // Disable Particle (Slot 47)
        if (slot == 47) {
            cItem.setEffect(ItemEffect.NONE);
            applyAndSync(held, cItem);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.0f);
            player.sendMessage(ColorUtil.color("&#FB7185Particle aura disabled."));
            render();
            return;
        }

        // Particle Slots
        ItemEffect[] allEffects = ItemEffect.values();
        for (int i = 0; i < allEffects.length && i < PARTICLE_SLOTS.length; i++) {
            if (slot == PARTICLE_SLOTS[i]) {
                ItemEffect eff = allEffects[i];
                cItem.setEffect(eff);
                applyAndSync(held, cItem);
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#34D399✔ Equipped particle effect: " + eff.getHexColor() + eff.getDisplayName()));
                render();
                return;
            }
        }

        // Motion Slots (37 - 43)
        ParticleMotion[] motions = ParticleMotion.values();
        int[] motionSlots = {37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < motions.length && i < motionSlots.length; i++) {
            if (slot == motionSlots[i]) {
                ParticleMotion pm = motions[i];
                cItem.setParticleMotion(pm);
                applyAndSync(held, cItem);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#34D399✔ Set particle motion pattern: &#38BDF8" + pm.getDisplayName()));
                render();
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
