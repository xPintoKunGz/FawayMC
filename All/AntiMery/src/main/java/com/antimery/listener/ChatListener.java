package com.antimery.listener;

import com.antimery.AntiMery;
import com.antimery.check.chat.ChatFilterCheck;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final AntiMery plugin;
    private final Map<UUID, Long> lastProcessedTime = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastProcessedMessage = new ConcurrentHashMap<>();

    public ChatListener(AntiMery plugin) {
        this.plugin = plugin;
        registerPaperChatEvent();
    }

    public void registerPaperChatEvent() {
        try {
            Class<?> eventClass = Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            Class<? extends Event> subClass = eventClass.asSubclass(Event.class);
            Bukkit.getPluginManager().registerEvent(
                    subClass,
                    this,
                    EventPriority.LOWEST,
                    (listener, event) -> {
                        if (eventClass.isInstance(event)) {
                            handlePaperChat(event);
                        }
                    },
                    plugin,
                    false
            );
            plugin.getLogger().info("เปิดใช้งานระบบตรวจจับแชทสำหรับ Paper (AsyncChatEvent) เรียบร้อย!");
        } catch (Throwable ignored) {
            // Pure Spigot / CraftBukkit, AsyncPlayerChatEvent handles everything
        }
    }

    private void handlePaperChat(Event event) {
        try {
            Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Player player = (Player) getPlayerMethod.invoke(event);

            Method messageMethod = event.getClass().getMethod("message");
            Object component = messageMethod.invoke(event);

            // Convert Adventure Component to plain text
            Class<?> plainSerializerClass = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
            Method plainTextMethod = plainSerializerClass.getMethod("plainText");
            Object serializer = plainTextMethod.invoke(null);
            Method serializeMethod = plainSerializerClass.getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"));
            String rawText = (String) serializeMethod.invoke(serializer, component);

            // Avoid double processing
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            Long lastTime = lastProcessedTime.get(uuid);
            String lastMsg = lastProcessedMessage.get(uuid);

            if (lastTime != null && now - lastTime < 100 && rawText.equals(lastMsg)) {
                return;
            }

            lastProcessedTime.put(uuid, now);
            lastProcessedMessage.put(uuid, rawText);

            ChatFilterCheck.FilterResult result = plugin.getCheckManager().getChatFilterCheck().processMessage(player, rawText);

            if (result.isBlocked()) {
                if (event instanceof Cancellable) {
                    ((Cancellable) event).setCancelled(true);
                } else {
                    Method setCancelledMethod = event.getClass().getMethod("setCancelled", boolean.class);
                    setCancelledMethod.invoke(event, true);
                }
            } else if (!result.getMessage().equals(rawText)) {
                Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
                Method textMethod = componentClass.getMethod("text", String.class);
                Object newComponent = textMethod.invoke(null, result.getMessage());
                Method setMessageMethod = event.getClass().getMethod("message", componentClass);
                setMessageMethod.invoke(event, newComponent);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("เกิดข้อผิดพลาดในการประมวลผล Paper AsyncChatEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastTime = lastProcessedTime.get(uuid);
        String lastMsg = lastProcessedMessage.get(uuid);

        if (lastTime != null && now - lastTime < 100 && message.equals(lastMsg)) {
            return;
        }

        lastProcessedTime.put(uuid, now);
        lastProcessedMessage.put(uuid, message);

        ChatFilterCheck.FilterResult result = plugin.getCheckManager().getChatFilterCheck().processMessage(player, message);

        if (result.isBlocked()) {
            event.setCancelled(true);
        } else if (!result.getMessage().equals(message)) {
            event.setMessage(result.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        String lower = msg.toLowerCase();

        // Catch bad words in chat-like commands
        if (lower.startsWith("/me ") || lower.startsWith("/msg ") || lower.startsWith("/tell ") || lower.startsWith("/w ") || lower.startsWith("/say ")) {
            int spaceIdx = msg.indexOf(' ');
            if (spaceIdx != -1) {
                String cmdPart = msg.substring(0, spaceIdx);
                String textPart = msg.substring(spaceIdx + 1);

                ChatFilterCheck.FilterResult result = plugin.getCheckManager().getChatFilterCheck().processMessage(event.getPlayer(), textPart);

                if (result.isBlocked()) {
                    event.setCancelled(true);
                } else if (!result.getMessage().equals(textPart)) {
                    event.setMessage(cmdPart + " " + result.getMessage());
                }
            }
        }
    }
}
