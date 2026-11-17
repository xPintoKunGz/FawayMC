package com.holomery.gui;

import com.holomery.HoloMeryPlugin;
import com.holomery.color.ColorParser;
import com.holomery.manager.HologramManager;
import com.holomery.model.Hologram;
import com.holomery.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Captures player chat input for in-game hologram editing without requiring long commands.
 */
public class ChatInputManager implements Listener {

    private static final Map<UUID, ChatPrompt> activePrompts = new ConcurrentHashMap<>();

    public record ChatPrompt(String hologramId, int pageIndex, int lineIndex, boolean isNew, Consumer<String> callback) {}

    public static void prompt(Player player, String promptTitle, String hologramId, int pageIndex, int lineIndex, boolean isNew, Consumer<String> callback) {
        player.closeInventory();
        activePrompts.put(player.getUniqueId(), new ChatPrompt(hologramId, pageIndex, lineIndex, isNew, callback));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f);
        MessageUtil.sendRaw(player, "\n<gradient:#00F2FE:#4FACFE:#FF0080>&l✦ HoloMery Editor ✦</gradient>");
        MessageUtil.sendRaw(player, "&b" + promptTitle);
        MessageUtil.sendRaw(player, "&7Type your text in chat with any color/gradient tags like &f<cyberpunk>Text</cyberpunk>&7.");
        MessageUtil.sendRaw(player, "&7You can also use &eITEM:DIAMOND_SWORD&7, &eBLOCK:BEACON&7, or &eMOB:ALLAY&7!");
        MessageUtil.sendRaw(player, "&7(Type &c'cancel'&7 to cancel)\n");
    }

    public static boolean isPrompted(Player player) {
        return activePrompts.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = activePrompts.remove(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        Bukkit.getScheduler().runTask(HoloMeryPlugin.getInstance(), () -> {
            if (msg.equalsIgnoreCase("cancel")) {
                MessageUtil.sendInfo(player, "Editing cancelled.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                reopenEditor(player, prompt.hologramId());
                return;
            }

            // Run callback
            prompt.callback().accept(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

            // Re-open GUI
            reopenEditor(player, prompt.hologramId());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activePrompts.remove(event.getPlayer().getUniqueId());
    }

    private static void reopenEditor(Player player, String hologramId) {
        Hologram holo = HologramManager.getInstance().getHologram(hologramId);
        if (holo != null && player.isOnline()) {
            HoloEditorGUI.open(player, holo);
        }
    }
}

