package com.holomery.action;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.holomery.HoloMeryPlugin;
import com.holomery.color.ColorParser;
import com.holomery.integration.PapiHook;
import com.holomery.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Handles execution of interactive hologram click actions:
 * - Page navigation: PAGE:2, PAGE:NEXT, PAGE:PREV
 * - Player commands: [player] spawn, PLAYER:warp pvp
 * - Console commands: [console] give %player_name% diamond 1
 * - Chat messages: [message] <cyberpunk>Welcome!</cyberpunk>
 * - Sounds: [sound] ENTITY_EXPERIENCE_ORB_PICKUP
 * - Server jump: [server] survival
 */
public class ActionExecutor {

    public static void execute(Player player, Hologram holo, String rawAction) {
        if (player == null || rawAction == null || rawAction.trim().isEmpty()) return;

        String action = rawAction.trim();
        String upper = action.toUpperCase();

        // 1. PAGE JUMP (e.g. PAGE:2, PAGE:NEXT, PAGE:PREV)
        if (upper.startsWith("PAGE:")) {
            String target = action.substring(5).trim().toLowerCase();
            if ("next".equals(target)) {
                holo.nextPage(player);
            } else if ("prev".equals(target) || "previous".equals(target)) {
                holo.prevPage(player);
            } else {
                try {
                    int pageNum = Integer.parseInt(target);
                    int idx = pageNum - 1;
                    if (idx >= 0 && idx < holo.getPageCount()) {
                        holo.setPage(idx);
                        try {
                            player.playSound(holo.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
                        } catch (Exception ignored) {}
                    }
                } catch (NumberFormatException ignored) {}
            }
            return;
        }

        // Parse placeholders if PlaceholderAPI is enabled
        String parsedAction = action;
        if (PapiHook.isHooked()) {
            parsedAction = PapiHook.parse(player, parsedAction);
        }
        parsedAction = parsedAction.replace("%player_name%", player.getName()).replace("%player%", player.getName());

        // 2. PLAYER COMMAND (e.g. [player] spawn or PLAYER:spawn)
        if (parsedAction.toLowerCase().startsWith("[player]")) {
            String cmd = parsedAction.substring(8).trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            player.performCommand(cmd);
            return;
        } else if (parsedAction.toUpperCase().startsWith("PLAYER:")) {
            String cmd = parsedAction.substring(7).trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            player.performCommand(cmd);
            return;
        }

        // 3. CONSOLE COMMAND (e.g. [console] give %player_name% diamond or CONSOLE:give ...)
        if (parsedAction.toLowerCase().startsWith("[console]")) {
            String cmd = parsedAction.substring(9).trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return;
        } else if (parsedAction.toUpperCase().startsWith("CONSOLE:")) {
            String cmd = parsedAction.substring(8).trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return;
        }

        // 4. CHAT MESSAGE (e.g. [message] <cyberpunk>Hello!</cyberpunk>)
        if (parsedAction.toLowerCase().startsWith("[message]")) {
            String msg = parsedAction.substring(9).trim();
            player.sendMessage(ColorParser.colorize(msg));
            return;
        } else if (parsedAction.toUpperCase().startsWith("MESSAGE:")) {
            String msg = parsedAction.substring(8).trim();
            player.sendMessage(ColorParser.colorize(msg));
            return;
        }

        // 5. SOUND (e.g. [sound] ENTITY_PLAYER_LEVELUP)
        if (parsedAction.toLowerCase().startsWith("[sound]")) {
            String soundName = parsedAction.substring(7).trim().toUpperCase();
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            return;
        } else if (parsedAction.toUpperCase().startsWith("SOUND:")) {
            String soundName = parsedAction.substring(6).trim().toUpperCase();
            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
            return;
        }

        // 6. BUNGEECORD SERVER (e.g. [server] lobby or SERVER:survival)
        if (parsedAction.toLowerCase().startsWith("[server]") || parsedAction.toUpperCase().startsWith("SERVER:")) {
            String srv = parsedAction.substring(parsedAction.indexOf(']') != -1 ? parsedAction.indexOf(']') + 1 : 7).trim();
            sendToServer(player, srv);
            return;
        }

        // Default: If starts with / or command, treat as player command
        if (parsedAction.startsWith("/")) {
            player.performCommand(parsedAction.substring(1));
        }
    }

    private static void sendToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(HoloMeryPlugin.getInstance(), "BungeeCord", out.toByteArray());
        } catch (Exception ignored) {}
    }
}

