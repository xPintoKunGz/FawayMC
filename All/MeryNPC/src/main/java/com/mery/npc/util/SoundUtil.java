package com.mery.npc.util;

import com.mery.npc.MeryNPC;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    public static void playClick(Player player) {
        if (!MeryNPC.getInstance().getConfigManager().isSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    public static void playSuccess(Player player) {
        if (!MeryNPC.getInstance().getConfigManager().isSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
    }

    public static void playEquip(Player player) {
        if (!MeryNPC.getInstance().getConfigManager().isSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.8f, 1.0f);
    }

    public static void playError(Player player) {
        if (!MeryNPC.getInstance().getConfigManager().isSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
    }

    public static void playTeleport(Player player) {
        if (!MeryNPC.getInstance().getConfigManager().isSoundsEnabled()) return;
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
    }
}

