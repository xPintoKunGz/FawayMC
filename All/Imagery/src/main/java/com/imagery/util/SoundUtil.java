package com.imagery.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Audio feedback helpers for Style 26.3 interactions.
 */
public final class SoundUtil {

    private SoundUtil() {}

    public static void playClick(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
        }
    }

    public static void playSuccess(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        }
    }

    public static void playPlace(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.1f);
        }
    }

    public static void playTeleport(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        }
    }

    public static void playHighlight(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.5f);
        }
    }

    public static void playError(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
        }
    }

    public static void playDelete(Player player) {
        if (player != null) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.0f);
        }
    }
}

