package com.imagery.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for formatting messages with Modern Hex Colors and Style 26.3 aesthetics.
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static final String PREFIX = color("&#00f2fe❖ &#4facfe&lImagery &#7f8c8d» &f");
    public static final String ACCENT = "&#00f2fe";
    public static final String PRIMARY = "&#4facfe";
    public static final String SECONDARY = "&#a18cd1";
    public static final String HIGHLIGHT = "&#00f5a0";
    public static final String WARNING = "&#f6d365";
    public static final String DANGER = "&#ff0844";
    public static final String MUTED = "&#95a5a6";

    private ColorUtil() {}

    /**
     * Translates '&' and '&#RRGGBB' color codes to Minecraft formatted text.
     */
    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> color(List<String> lines) {
        List<String> result = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                result.add(color(line));
            }
        }
        return result;
    }

    /**
     * Creates a smooth gradient between two hex colors.
     */
    public static String gradient(String text, String hexStart, String hexEnd) {
        if (text == null || text.isEmpty()) return "";
        Color start = Color.decode(hexStart);
        Color end = Color.decode(hexEnd);
        int length = text.length();

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) Math.max(1, length - 1);
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

            String hex = String.format("#%02x%02x%02x", red, green, blue);
            builder.append(ChatColor.of(hex)).append(text.charAt(i));
        }
        return builder.toString();
    }
}
