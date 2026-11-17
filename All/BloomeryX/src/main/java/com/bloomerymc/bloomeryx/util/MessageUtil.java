package com.bloomerymc.bloomeryx.util;

import com.bloomerymc.bloomeryx.BloomeryX;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private MessageUtil() {}

    public static void sendMessage(CommandSender sender, String rawMessage) {
        if (sender == null || rawMessage == null || rawMessage.isEmpty()) return;
        String prefix = BloomeryX.getInstance().getConfig().getString("settings.prefix", "");
        String formatted = ColorUtil.colorize(rawMessage.replace("{prefix}", prefix));
        sender.sendMessage(formatted);
    }

    public static void sendConfigMessage(CommandSender sender, String configKey, String... replacements) {
        String msg = BloomeryX.getInstance().getConfig().getString("messages." + configKey, "");
        if (msg == null || msg.isEmpty()) return;

        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }

        String prefix = BloomeryX.getInstance().getConfig().getString("settings.prefix", "");
        msg = msg.replace("{prefix}", prefix);
        sender.sendMessage(ColorUtil.colorize(msg));
    }

    public static void playSound(Player player, String soundName) {
        if (player == null || !BloomeryX.getInstance().getConfig().getBoolean("settings.enable-sounds", true)) return;
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    public static void playSuccess(Player player) {
        String sound = BloomeryX.getInstance().getConfig().getString("settings.success-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        playSound(player, sound);
    }

    public static void playError(Player player) {
        String sound = BloomeryX.getInstance().getConfig().getString("settings.error-sound", "ENTITY_VILLAGER_NO");
        playSound(player, sound);
    }

    public static void playTeleport(Player player) {
        String sound = BloomeryX.getInstance().getConfig().getString("settings.teleport-sound", "ENTITY_ENDERMAN_TELEPORT");
        playSound(player, sound);
    }
}

