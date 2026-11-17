package com.merycraft.meryitem.hook;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemStat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * PlaceholderAPI expansion for MeryITEM stats and gear.
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final MeryItemPlugin plugin;

    public PlaceholderAPIHook(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "meryitem";
    }

    @Override
    public String getAuthor() {
        return "MeryCraft";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        String lower = params.toLowerCase();

        // %meryitem_stat_<stat>%
        if (lower.startsWith("stat_")) {
            String statName = lower.substring(5);
            ItemStat stat = ItemStat.fromString(statName);
            if (stat != null) {
                double val = plugin.getItemManager().getPlayerStat(player, stat);
                return (val == (long) val) ? String.valueOf((long) val) : String.format("%.1f", val);
            }
        }

        // Shortcut placeholders
        switch (lower) {
            case "damage":
            case "attack_damage": {
                double val = plugin.getItemManager().getPlayerStat(player, ItemStat.ATTACK_DAMAGE);
                return String.format("%.1f", val);
            }
            case "defense": {
                double val = plugin.getItemManager().getPlayerStat(player, ItemStat.DEFENSE);
                return String.format("%.1f", val);
            }
            case "crit_chance": {
                double val = plugin.getItemManager().getPlayerStat(player, ItemStat.CRIT_CHANCE);
                return String.format("%.1f", val);
            }
            case "speed": {
                double val = plugin.getItemManager().getPlayerStat(player, ItemStat.MOVEMENT_SPEED);
                return String.format("%.1f", val);
            }
            case "mainhand_id": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                String id = plugin.getItemManager().getMeryItemId(hand);
                return id != null ? id : "NONE";
            }
            case "mainhand_rarity": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                CustomItem c = plugin.getItemManager().getFromItemStack(hand);
                return c != null ? c.getRarity().getDisplayName() : "NONE";
            }
            case "mainhand_name": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                CustomItem c = plugin.getItemManager().getFromItemStack(hand);
                return c != null ? c.getDisplayName() : (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName() ? hand.getItemMeta().getDisplayName() : hand.getType().name());
            }
        }

        return null;
    }
}
