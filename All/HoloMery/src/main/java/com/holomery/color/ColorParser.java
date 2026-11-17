package com.holomery.color;

import com.holomery.manager.ThemeManager;
import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced gradient and color formatting engine for HoloMery.
 * Supports:
 * - Multi-stop gradients: <gradient:#HEX1:#HEX2:...>Text</gradient>
 * - Custom themes: <cyberpunk>Text</cyberpunk>, <sunset>Text</sunset>, etc.
 * - Legacy color codes: &a-&f, &0-&9, &l, &o, &n, &m, &k, &r
 * - Standard Hex codes: &#RRGGBB, {#RRGGBB}, <#RRGGBB>
 */
public class ColorParser {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([#0-9a-fA-F:]+)>(.*?)</gradient>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_GRADIENT_PATTERN = Pattern.compile("<#([0-9a-fA-F]{6})>(.*?)</#([0-9a-fA-F]{6})>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("<#([0-9a-fA-F]{6})>");
    private static final Pattern HEX_HASH_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern HEX_BRACKET_PATTERN = Pattern.compile("\\{([#0-9a-fA-F]{6,7})\\}");
    private static final Pattern THEME_TAG_PATTERN = Pattern.compile("<([a-zA-Z0-9_]+)>(.*?)</\\1>", Pattern.CASE_INSENSITIVE);

    /**
     * Parse all color codes, gradients, custom themes, and formatting.
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 1. Process custom theme tags (<cyberpunk>text</cyberpunk>, <aurora>text</aurora>)
        text = processThemeTags(text);

        // 2. Process multi-stop gradients (<gradient:#HEX1:#HEX2:...>text</gradient>)
        text = processMultiStopGradients(text);

        // 3. Process two-stop short gradients (<#HEX1>text</#HEX2>)
        text = processShortGradients(text);

        // 4. Process standalone hex codes (<#HEX>, &#HEX, {#HEX})
        text = processHexColors(text);

        // 5. Process legacy color codes (&0-9, &a-f, &k-o, &r)
        text = ChatColor.translateAlternateColorCodes('&', text);

        return text;
    }

    private static String processThemeTags(String text) {
        Matcher matcher = THEME_TAG_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String themeName = matcher.group(1).toLowerCase();
            String innerText = matcher.group(2);

            List<Color> palette = ThemeManager.getInstance().getThemeColors(themeName);
            if (palette != null && !palette.isEmpty()) {
                String rendered = applyGradient(innerText, palette);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(rendered));
            } else {
                // If not a known theme, leave unchanged
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String processMultiStopGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hexStopsRaw = matcher.group(1);
            String innerText = matcher.group(2);

            String[] stops = hexStopsRaw.split(":");
            List<Color> colorStops = new ArrayList<>();
            for (String stop : stops) {
                stop = stop.trim();
                if (stop.startsWith("#")) stop = stop.substring(1);
                if (stop.length() == 6) {
                    try {
                        colorStops.add(new Color(Integer.parseInt(stop, 16)));
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (colorStops.size() >= 2) {
                String rendered = applyGradient(innerText, colorStops);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(rendered));
            } else if (colorStops.size() == 1) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(ChatColor.of(colorStops.get(0)) + innerText));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(innerText));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String processShortGradients(String text) {
        Matcher matcher = SHORT_GRADIENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hex1 = matcher.group(1);
            String innerText = matcher.group(2);
            String hex2 = matcher.group(3);

            try {
                Color c1 = new Color(Integer.parseInt(hex1, 16));
                Color c2 = new Color(Integer.parseInt(hex2, 16));
                List<Color> stops = List.of(c1, c2);
                String rendered = applyGradient(innerText, stops);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(rendered));
            } catch (Exception e) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(innerText));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String processHexColors(String text) {
        // <#RRGGBB>
        Matcher m1 = HEX_TAG_PATTERN.matcher(text);
        StringBuffer sb1 = new StringBuffer();
        while (m1.find()) {
            String hex = m1.group(1);
            m1.appendReplacement(sb1, ChatColor.of("#" + hex).toString());
        }
        m1.appendTail(sb1);
        text = sb1.toString();

        // &#RRGGBB
        Matcher m2 = HEX_HASH_PATTERN.matcher(text);
        StringBuffer sb2 = new StringBuffer();
        while (m2.find()) {
            String hex = m2.group(1);
            m2.appendReplacement(sb2, ChatColor.of("#" + hex).toString());
        }
        m2.appendTail(sb2);
        text = sb2.toString();

        // {#RRGGBB}
        Matcher m3 = HEX_BRACKET_PATTERN.matcher(text);
        StringBuffer sb3 = new StringBuffer();
        while (m3.find()) {
            String hex = m3.group(1);
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() == 6) {
                m3.appendReplacement(sb3, ChatColor.of("#" + hex).toString());
            } else {
                m3.appendReplacement(sb3, m3.group(0));
            }
        }
        m3.appendTail(sb3);
        return sb3.toString();
    }

    /**
     * Interpolates multiple color stops across a string, preserving styling codes like &l, &o, etc.
     */
    public static String applyGradient(String text, List<Color> stops) {
        if (text == null || text.isEmpty()) return "";
        if (stops == null || stops.isEmpty()) return text;
        if (stops.size() == 1) return ChatColor.of(stops.get(0)) + text;

        // Strip legacy codes for length calculation while recording active styles
        StringBuilder result = new StringBuilder();
        int charCount = 0;

        // Count printable characters (excluding & codes)
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdefklmnor".indexOf(code) >= 0) {
                    i++; // skip
                    continue;
                }
            }
            charCount++;
        }

        if (charCount == 0) return text;

        StringBuilder activeFormats = new StringBuilder();
        int printableIndex = 0;
        int totalSegments = stops.size() - 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Handle format codes (&l, &o, &m, &n, &k, &r)
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ("klmno".indexOf(code) >= 0) {
                    activeFormats.append("§").append(code);
                    i++;
                    continue;
                } else if (code == 'r') {
                    activeFormats.setLength(0);
                    i++;
                    continue;
                }
            }

            // Calculate gradient position
            float progress = (charCount <= 1) ? 0f : (float) printableIndex / (float) (charCount - 1);
            Color interpolatedColor = interpolateMultiStop(stops, progress);

            result.append(ChatColor.of(interpolatedColor));
            if (activeFormats.length() > 0) {
                result.append(activeFormats);
            }
            result.append(c);

            printableIndex++;
        }

        return result.toString();
    }

    private static Color interpolateMultiStop(List<Color> stops, float progress) {
        if (progress <= 0f) return stops.get(0);
        if (progress >= 1f) return stops.get(stops.size() - 1);

        int totalSegments = stops.size() - 1;
        float segmentLength = 1f / totalSegments;
        int currentSegment = Math.min((int) (progress / segmentLength), totalSegments - 1);

        float segmentProgress = (progress - (currentSegment * segmentLength)) / segmentLength;
        Color c1 = stops.get(currentSegment);
        Color c2 = stops.get(currentSegment + 1);

        return interpolateColor(c1, c2, segmentProgress);
    }

    private static Color interpolateColor(Color c1, Color c2, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int r = (int) (c1.getRed() + ratio * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + ratio * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + ratio * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }

    public static String stripColor(String text) {
        if (text == null) return null;
        return ChatColor.stripColor(colorize(text));
    }
}

