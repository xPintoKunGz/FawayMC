package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.ItemStat;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Beautiful player stat overview GUI (100% English).
 */
public class StatsGUI implements MenuHolder {

    private final MeryItemPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public StatsGUI(MeryItemPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtil.color("&#38BDF8§lPlayer Profile &#334155» &#CBD5E1Active Stats"));
    }

    public void open() {
        render();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
    }

    private void render() {
        inventory.clear();

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, "&#1E293B ", null);
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, border);
        }

        // Player Head at Slot 13
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ColorUtil.color("&#F472B6§l" + player.getName()));
            skullMeta.setLore(ColorUtil.color(Arrays.asList(
                    "&#94A3B8Level: &#FDE047" + player.getLevel(),
                    "&#94A3B8Current Health: &#F87171" + String.format("%.1f", player.getHealth()) + " / " + String.format("%.1f", player.getMaxHealth()),
                    "",
                    "&#CBD5E1Currently active MeryITEM bonuses."
            )));
            head.setItemMeta(skullMeta);
        }
        inventory.setItem(13, head);

        // Active stats
        Map<ItemStat, Double> stats = plugin.getItemManager().getPlayerTotalStats(player);

        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        ItemStat[] displayStats = {
                ItemStat.ATTACK_DAMAGE,
                ItemStat.DEFENSE,
                ItemStat.CRIT_CHANCE,
                ItemStat.CRIT_DAMAGE,
                ItemStat.LIFE_STEAL,
                ItemStat.MOVEMENT_SPEED,
                ItemStat.MAX_HEALTH
        };

        for (int i = 0; i < displayStats.length; i++) {
            ItemStat stat = displayStats[i];
            double val = stats.getOrDefault(stat, 0.0);

            Material mat;
            switch (stat) {
                case ATTACK_DAMAGE: mat = Material.IRON_SWORD; break;
                case DEFENSE: mat = Material.SHIELD; break;
                case CRIT_CHANCE: mat = Material.GLOWSTONE_DUST; break;
                case CRIT_DAMAGE: mat = Material.BLAZE_POWDER; break;
                case LIFE_STEAL: mat = Material.REDSTONE; break;
                case MOVEMENT_SPEED: mat = Material.SUGAR; break;
                case MAX_HEALTH: mat = Material.GOLDEN_APPLE; break;
                default: mat = Material.NETHER_STAR; break;
            }

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&#94A3B8Active Bonus: " + stat.getHexColor() + stat.formatValue(val)));
            lore.add("");
            lore.add(ColorUtil.color("&#64748BGained from equipped weapons & armor"));

            inventory.setItem(slots[i], createItem(mat, stat.getHexColor() + stat.getIcon() + " " + stat.getDisplayName(), lore));
        }

        // Back button at Slot 31
        inventory.setItem(31, createItem(Material.ARROW, "&#F472B6« Back to Catalog", Arrays.asList("&#94A3B8Return to catalog")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 31) {
            new ItemBrowserGUI(plugin, player).open();
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
