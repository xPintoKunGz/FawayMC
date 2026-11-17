package me.editmery;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(&#|\\{#|<#)([A-Fa-f0-9]{6})([\\}>]?)");

    /**
     * Translates hex color codes (&#RRGGBB, {#RRGGBB}, <#RRGGBB>) and standard & color codes
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(2);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(Character.toLowerCase(c));
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    /**
     * Sends colorized chat message to player
     */
    public static void sendMessage(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(colorize(message));
        }
    }

    /**
     * Plays sound safely with fallback if sound name varies
     */
    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            // Sound name might not exist on all versions
        }
    }

    /**
     * Sends action bar message
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) return;
        try {
            player.sendActionBar(colorize(message));
        } catch (Throwable t) {
            // Fallback to chat if sendActionBar is unavailable
            player.sendMessage(colorize(message));
        }
    }

    /**
     * Sends interactive clickable message with hover text
     */
    public static void sendClickableToggleMessage(Player player, String prefixText, String buttonText, String clickCommand, String hoverText, String suffixText) {
        if (player == null) return;
        try {
            TextComponent message = new TextComponent();
            if (prefixText != null && !prefixText.isEmpty()) {
                message.addExtra(new TextComponent(colorize(prefixText)));
            }

            TextComponent button = new TextComponent(colorize(buttonText));
            button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand));
            button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(colorize(hoverText))));
            message.addExtra(button);

            if (suffixText != null && !suffixText.isEmpty()) {
                message.addExtra(new TextComponent(colorize(suffixText)));
            }

            player.spigot().sendMessage(message);
        } catch (Throwable t) {
            // Fallback to regular chat message
            player.sendMessage(colorize(prefixText + buttonText + suffixText));
        }
    }

    /**
     * Sends message with click event to suggest command into chat
     */
    public static void sendSuggestCommandMessage(Player player, String messageText, String suggestCmd, String hoverText) {
        if (player == null) return;
        try {
            TextComponent message = new TextComponent(colorize(messageText));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCmd));
            if (hoverText != null && !hoverText.isEmpty()) {
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(colorize(hoverText))));
            }
            player.spigot().sendMessage(message);
        } catch (Throwable t) {
            player.sendMessage(colorize(messageText));
        }
    }
}
