package com.bloomerymc.bloomeryx.util;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    /**
     * Translates standard & color codes and Hex colors (&#RRGGBB / #RRGGBB) to Minecraft format.
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Match &#RRGGBB
        Matcher matcher1 = HEX_PATTERN_1.matcher(message);
        StringBuffer buffer1 = new StringBuffer();
        while (matcher1.find()) {
            String hex = matcher1.group(1);
            matcher1.appendReplacement(buffer1, ChatColor.of("#" + hex).toString());
        }
        matcher1.appendTail(buffer1);
        String step1 = buffer1.toString();

        // Match #RRGGBB where preceded by & or start
        Matcher matcher2 = HEX_PATTERN_2.matcher(step1);
        StringBuffer buffer2 = new StringBuffer();
        while (matcher2.find()) {
            // Check if it's already translated or standalone hex
            try {
                String hex = matcher2.group(1);
                matcher2.appendReplacement(buffer2, ChatColor.of("#" + hex).toString());
            } catch (Exception ignored) {
                matcher2.appendReplacement(buffer2, matcher2.group(0));
            }
        }
        matcher2.appendTail(buffer2);

        // Standard alternate color codes
        return ChatColor.translateAlternateColorCodes('&', buffer2.toString());
    }

    /**
     * Strips all colors from a string.
     */
    public static String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(colorize(message));
    }
}

