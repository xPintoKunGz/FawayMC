package com.holomery.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI soft-dependency integration.
 */
public class PapiHook {

    private static boolean hooked = false;

    public static void init() {
        hooked = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static boolean isHooked() {
        return hooked;
    }

    public static String parse(Player player, String text) {
        if (!hooked || text == null) return text;
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}

