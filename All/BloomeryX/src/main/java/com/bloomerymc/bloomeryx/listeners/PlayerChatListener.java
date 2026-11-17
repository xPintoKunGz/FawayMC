package com.bloomerymc.bloomeryx.listeners;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerChatListener implements Listener {

    private final BloomeryX plugin;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public PlayerChatListener(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        // Update AFK activity on chat
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getAfkManager().updateActivity(sender));

        if (!plugin.getConfig().getBoolean("chat.enabled", true)) {
            return;
        }

        String rawMessage = event.getMessage();
        String message = rawMessage;

        // Check if sender can use colors in chat
        if (sender.hasPermission("bloomeryx.chat.colors")) {
            message = ColorUtil.colorize(message);
        }

        // Mention system (@PlayerName)
        if (plugin.getConfig().getBoolean("mention.enabled", true) && sender.hasPermission("bloomeryx.chat.mention")) {
            Matcher matcher = MENTION_PATTERN.matcher(message);
            Set<Player> mentionedPlayers = new HashSet<>();

            while (matcher.find()) {
                String targetName = matcher.group(1);
                Player target = Bukkit.getPlayer(targetName);
                if (target != null && target.isOnline() && !target.equals(sender)) {
                    mentionedPlayers.add(target);
                }
            }

            // Highlight mentions in message
            for (Player target : mentionedPlayers) {
                String highlightColor = plugin.getConfig().getString("mention.highlight-color", "&#FF7EB3");
                message = message.replaceAll("(?i)@" + Pattern.quote(target.getName()), ColorUtil.colorize(highlightColor + "@" + target.getName() + "&r"));
            }

            // Send notification to mentioned players
            if (!mentionedPlayers.isEmpty()) {
                final String finalRawMessage = rawMessage;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player target : mentionedPlayers) {
                        sendMentionNotification(sender, target, finalRawMessage);
                    }
                });
            }
        }

        String formatTemplate = plugin.getConfig().getString("chat.format", "{prefix}&f{displayname}{suffix}&8: &7{message}");
        String formatted = plugin.getPlaceholderHook().parse(sender, formatTemplate);

        // Preserve % in messages by setting format with %2$s
        event.setFormat(formatted.replace("{message}", "%2$s"));
        event.setMessage(message);
    }

    private void sendMentionNotification(Player sender, Player target, String fullMessage) {
        // Chat notification
        String chatFormat = plugin.getConfig().getString("mention.format-chat", "&#FF7EB3✦ [Mention] &#B2EBF2{sender} &7called you: &f\"{message}\"");
        chatFormat = chatFormat
                .replace("{sender}", sender.getName())
                .replace("{displayname}", sender.getDisplayName())
                .replace("{message}", fullMessage);
        target.sendMessage(ColorUtil.colorize(chatFormat));

        // Title notification (if enabled)
        if (plugin.getConfig().getBoolean("mention.show-title", true)) {
            String title = plugin.getConfig().getString("mention.title", "&#FF7EB3{sender} mentioned you!");
            String subtitle = plugin.getConfig().getString("mention.subtitle", "&7{message}");
            title = ColorUtil.colorize(title.replace("{sender}", sender.getName()).replace("{displayname}", sender.getDisplayName()));
            subtitle = ColorUtil.colorize(subtitle.replace("{message}", fullMessage));
            target.sendTitle(title, subtitle, 10, 50, 20);
        }

        // Sound notification
        String soundName = plugin.getConfig().getString("mention.sound", "BLOCK_NOTE_BLOCK_BELL");
        try {
            Sound sound = Sound.valueOf(soundName);
            target.playSound(target.getLocation(), sound, 1.0f, 1.2f);
        } catch (Exception ignored) {
            MessageUtil.playSuccess(target);
        }
    }
}
