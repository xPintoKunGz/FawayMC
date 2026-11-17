package com.mery.npc.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    /**
     * Translates standard Minecraft color codes (&a, &b, etc.)
     * and modern hex colors (&#RRGGBB or <#RRGGBB>).
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Replace <#RRGGBB>
        Matcher matcher2 = HEX_PATTERN_2.matcher(text);
        StringBuffer buffer2 = new StringBuffer();
        while (matcher2.find()) {
            String hex = matcher2.group(1);
            matcher2.appendReplacement(buffer2, ChatColor.of("#" + hex).toString());
        }
        matcher2.appendTail(buffer2);
        text = buffer2.toString();

        // Replace &#RRGGBB
        Matcher matcher1 = HEX_PATTERN_1.matcher(text);
        StringBuffer buffer1 = new StringBuffer();
        while (matcher1.find()) {
            String hex = matcher1.group(1);
            matcher1.appendReplacement(buffer1, ChatColor.of("#" + hex).toString());
        }
        matcher1.appendTail(buffer1);
        text = buffer1.toString();

        // Standard alternate color codes
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorize(List<String> lines) {
        if (lines == null) return new ArrayList<>();
        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            colored.add(colorize(line));
        }
        return colored;
    }

    /**
     * Strips all colors for clean comparison or console logs.
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }
}

