package com.merycraft.meryitem.gui;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.util.ColorUtil;
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
 * Captures player chat input for GUI renaming, lore writing, and saving IDs (100% English).
 */
public class ChatInputListener implements Listener {

    private static final Map<UUID, Consumer<String>> PENDING_PROMPTS = new ConcurrentHashMap<>();

    public static void startPrompt(Player player, String promptMessage, Consumer<String> onInput) {
        PENDING_PROMPTS.put(player.getUniqueId(), onInput);
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
        player.sendMessage(ColorUtil.color("&#334155§m                                                  "));
        player.sendMessage(ColorUtil.color("&#F472B6§lMeryITEM &#334155» &#CBD5E1" + promptMessage));
        player.sendMessage(ColorUtil.color("&#94A3B8(Type &#FB7185cancel &#94A3B8to abort)"));
        player.sendMessage(ColorUtil.color("&#334155§m                                                  "));
    }

    public static boolean hasPrompt(Player player) {
        return PENDING_PROMPTS.containsKey(player.getUniqueId());
    }

    public static void cancelPrompt(Player player) {
        PENDING_PROMPTS.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = PENDING_PROMPTS.remove(player.getUniqueId());

        if (callback != null) {
            event.setCancelled(true);
            String message = event.getMessage().trim();

            Bukkit.getScheduler().runTask(MeryItemPlugin.getInstance(), () -> {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ColorUtil.color("&#FB7185Input cancelled."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
                    new ItemEditorGUI(MeryItemPlugin.getInstance(), player).open();
                } else {
                    callback.accept(message);
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PENDING_PROMPTS.remove(event.getPlayer().getUniqueId());
    }
}
