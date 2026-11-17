package com.merycraft.meryitem.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for modern Hex (#RRGGBB) and legacy Minecraft color codes.
 * Optimized for Minecraft 1.20+ and modern Spigot/Paper versions (including 26.x).
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN_3 = Pattern.compile("#([A-Fa-f0-9]{6})");

    private ColorUtil() {}

    /**
     * Colorize a string with hex and legacy ampersand '&' codes.
     */
    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Format &#RRGGBB
        Matcher matcher1 = HEX_PATTERN_1.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher1.find()) {
            String hex = matcher1.group(1);
            matcher1.appendReplacement(buffer, toChatHex(hex));
        }
        matcher1.appendTail(buffer);
        message = buffer.toString();

        // Format <#RRGGBB>
        Matcher matcher2 = HEX_PATTERN_2.matcher(message);
        buffer = new StringBuffer();
        while (matcher2.find()) {
            String hex = matcher2.group(1);
            matcher2.appendReplacement(buffer, toChatHex(hex));
        }
        matcher2.appendTail(buffer);
        message = buffer.toString();

        // Translate standard color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Colorize a list of strings (for Lore).
     */
    public static List<String> color(List<String> lines) {
        if (lines == null) return new ArrayList<>();
        List<String> colored = new ArrayList<>(lines.size());
        for (String line : lines) {
            colored.add(color(line));
        }
        return colored;
    }

    /**
     * Strips all colors from a string.
     */
    public static String strip(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(color(message));
    }

    private static String toChatHex(String hex) {
        StringBuilder builder = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            builder.append('§').append(c);
        }
        return builder.toString();
    }
}

