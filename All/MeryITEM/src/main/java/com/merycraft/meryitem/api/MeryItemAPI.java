package com.merycraft.meryitem.api;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemStat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

/**
 * Public developer API for interacting with MeryITEM from other plugins.
 */
public final class MeryItemAPI {

    private MeryItemAPI() {}

    public static CustomItem getItem(String id) {
        return MeryItemPlugin.getInstance().getItemManager().getItem(id);
    }

    public static ItemStack buildItem(String id, int amount) {
        CustomItem item = getItem(id);
        if (item == null) return null;
        return item.buildItemStack(amount);
    }

    public static boolean isMeryItem(ItemStack stack) {
        return MeryItemPlugin.getInstance().getItemManager().isMeryItem(stack);
    }

    public static String getItemId(ItemStack stack) {
        return MeryItemPlugin.getInstance().getItemManager().getMeryItemId(stack);
    }

    public static Collection<CustomItem> getAllItems() {
        return MeryItemPlugin.getInstance().getItemManager().getAllItems();
    }

    public static Map<ItemStat, Double> getPlayerStats(Player player) {
        return MeryItemPlugin.getInstance().getItemManager().getPlayerTotalStats(player);
    }

    public static double getPlayerStat(Player player, ItemStat stat) {
        return MeryItemPlugin.getInstance().getItemManager().getPlayerStat(player, stat);
    }
}

