package com.holomery.util;

import com.holomery.color.ColorParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Modern chat message utility featuring stylish futuristic gradients and interactive components.
 */
public class MessageUtil {

    public static final String PREFIX = ColorParser.colorize("<gradient:#00F2FE:#4FACFE:#FF0080>&l✦ HoloMery ✦</gradient> &8» &r");

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ColorParser.colorize(message));
    }

    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ColorParser.colorize("<#00FF87>&l[SUCCESS] <#60EFFF>" + message));
    }

    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ColorParser.colorize("<#FF0844>&l[ERROR] <#FFB199>" + message));
    }

    public static void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ColorParser.colorize("<#4FACFE>&l[INFO] &f" + message));
    }

    public static void sendRaw(CommandSender sender, String rawMessage) {
        sender.sendMessage(ColorParser.colorize(rawMessage));
    }

    /**
     * Send an interactive clickable entry (e.g. in /hm list)
     */
    public static void sendHoloListEntry(Player player, String id, int lineCount, String world, double x, double y, double z) {
        TextComponent base = new TextComponent(ColorParser.colorize("&8 ▪ &b" + id + " &7(&f" + lineCount + " lines&7) "));

        // [TP] button
        TextComponent tpBtn = new TextComponent(ColorParser.colorize("&a&l[TP] "));
        tpBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hm tp " + id));
        tpBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ColorParser.colorize("&aClick to teleport to &e" + id))));

        // [INFO] button
        TextComponent infoBtn = new TextComponent(ColorParser.colorize("&e&l[INFO] "));
        infoBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hm info " + id));
        infoBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ColorParser.colorize("&eClick to view settings for &e" + id))));

        // [DELETE] button
        TextComponent delBtn = new TextComponent(ColorParser.colorize("&c&l[DEL]"));
        delBtn.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/hm delete " + id));
        delBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ColorParser.colorize("&cClick to suggest delete command for &e" + id))));

        base.addExtra(tpBtn);
        base.addExtra(infoBtn);
        base.addExtra(delBtn);

        player.spigot().sendMessage(base);
    }
}

