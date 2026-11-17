package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessagingCommands implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;
    private final Map<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();

    public MessagingCommands(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        switch (name) {
            case "msg":
            case "tell":
            case "w":
            case "whisper":
            case "m":
                return handleMsg(sender, args);
            case "reply":
            case "r":
                return handleReply(sender, args);
            case "socialspy":
            case "spy":
                return handleSocialSpy(sender);
            case "broadcast":
            case "bc":
            case "alert":
                return handleBroadcast(sender, args);
            case "invsee":
                return handleInvsee(sender, args);
            default:
                return false;
        }
    }

    private boolean handleMsg(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.msg")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/msg <player> <message>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
            return true;
        }

        if (sender instanceof Player && sender.equals(target)) {
            MessageUtil.sendConfigMessage(sender, "msg-self");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendPrivateMessage(sender, target, message);
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.msg")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/r <message>");
            return true;
        }

        Player player = (Player) sender;
        UUID replyTo = lastMessaged.get(player.getUniqueId());
        if (replyTo == null) {
            MessageUtil.sendConfigMessage(player, "msg-no-reply");
            return true;
        }

        Player target = Bukkit.getPlayer(replyTo);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendConfigMessage(player, "msg-no-reply");
            return true;
        }

        String message = String.join(" ", args);
        sendPrivateMessage(player, target, message);
        return true;
    }

    private void sendPrivateMessage(CommandSender sender, Player target, String message) {
        if (sender instanceof Player && sender.hasPermission("bloomeryx.chat.colors")) {
            message = ColorUtil.colorize(message);
        }

        MessageUtil.sendConfigMessage(sender, "msg-format-send", "{target}", target.getName(), "{message}", message);
        MessageUtil.sendConfigMessage(target, "msg-format-receive", "{sender}", sender.getName(), "{message}", message);

        if (sender instanceof Player) {
            lastMessaged.put(((Player) sender).getUniqueId(), target.getUniqueId());
            lastMessaged.put(target.getUniqueId(), ((Player) sender).getUniqueId());
        }

        // Notify SocialSpy
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender) || online.equals(target)) continue;
            if (plugin.getPlayerDataManager().isSocialSpy(online.getUniqueId())) {
                MessageUtil.sendConfigMessage(online, "msg-spy", "{sender}", sender.getName(), "{target}", target.getName(), "{message}", message);
            }
        }
    }

    private boolean handleSocialSpy(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.socialspy")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        boolean current = plugin.getPlayerDataManager().isSocialSpy(player.getUniqueId());
        plugin.getPlayerDataManager().setSocialSpy(player.getUniqueId(), !current);

        MessageUtil.sendMessage(player, (!current ? "&#70E000✔ &aSocialSpy ENABLED &8(Monitoring private messages)" : "&#FF758C✕ &cSocialSpy DISABLED"));
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean handleBroadcast(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.broadcast")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/broadcast <message>");
            return true;
        }

        String msg = String.join(" ", args);
        if (sender.hasPermission("bloomeryx.chat.colors")) {
            msg = ColorUtil.colorize(msg);
        }

        String format = plugin.getConfig().getString("messages.broadcast-format", "&#FF7EB3[Broadcast] &8: &f{message}");
        String formatted = ColorUtil.colorize(format.replace("{message}", msg));

        Bukkit.broadcastMessage(formatted);
        for (Player p : Bukkit.getOnlinePlayers()) {
            MessageUtil.playSuccess(p);
        }
        return true;
    }

    private boolean handleInvsee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.invsee")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(player, "&#FF4B4B✕ &cUsage: &f/invsee <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendConfigMessage(player, "player-not-found", "{target}", args[0]);
            return true;
        }

        player.openInventory(target.getInventory());
        MessageUtil.playSuccess(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        String name = command.getName().toLowerCase();
        if ((name.equals("msg") || name.equals("invsee")) && args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
        }
        return list;
    }
}

