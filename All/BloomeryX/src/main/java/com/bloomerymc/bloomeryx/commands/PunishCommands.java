package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PunishCommands implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public PunishCommands(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        switch (name) {
            case "ban":
                return handleBan(sender, args);
            case "unban":
            case "pardon":
                return handleUnban(sender, args);
            case "ban-ip":
            case "banip":
                return handleBanIp(sender, args);
            case "unban-ip":
            case "unbanip":
            case "pardon-ip":
                return handleUnbanIp(sender, args);
            default:
                return false;
        }
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.ban")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/ban <player> [reason]");
            return true;
        }

        String targetName = args[0];
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Banned by an administrator";

        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, null, sender.getName());

        Player onlineTarget = Bukkit.getPlayer(targetName);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.kickPlayer(ColorUtil.colorize("&#FF4B4B✕ You have been banned from this server!\n&7Reason: &f" + reason + "\n&7By: &f" + sender.getName()));
        }

        Bukkit.broadcastMessage(ColorUtil.colorize("&#FF4B4B[Ban] &f" + targetName + " &cwas banned by &f" + sender.getName() + " &7(&f" + reason + "&7)"));
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.unban")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/unban <player>");
            return true;
        }

        String targetName = args[0];
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        if (banList.isBanned(targetName)) {
            banList.pardon(targetName);
            MessageUtil.sendMessage(sender, "&#70E000✔ &aSuccessfully unbanned &f" + targetName + "&a!");
            if (sender instanceof Player) MessageUtil.playSuccess((Player) sender);
        } else {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cPlayer &f" + targetName + " &cis not banned.");
            if (sender instanceof Player) MessageUtil.playError((Player) sender);
        }
        return true;
    }

    private boolean handleBanIp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.banip")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/ban-ip <player|ip> [reason]");
            return true;
        }

        String targetInput = args[0];
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "IP banned by an administrator";
        String ipToBan = targetInput;

        Player onlineTarget = Bukkit.getPlayer(targetInput);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            InetSocketAddress address = onlineTarget.getAddress();
            if (address != null && address.getAddress() != null) {
                ipToBan = address.getAddress().getHostAddress();
            }
            onlineTarget.kickPlayer(ColorUtil.colorize("&#FF4B4B✕ Your IP has been banned from this server!\n&7Reason: &f" + reason));
        }

        Bukkit.getBanList(BanList.Type.IP).addBan(ipToBan, reason, null, sender.getName());
        Bukkit.broadcastMessage(ColorUtil.colorize("&#FF4B4B[Ban-IP] &cIP &f" + ipToBan + " &cwas banned by &f" + sender.getName() + " &7(&f" + reason + "&7)"));
        return true;
    }

    private boolean handleUnbanIp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.unbanip")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/unban-ip <IP>");
            return true;
        }

        String ip = args[0];
        BanList banList = Bukkit.getBanList(BanList.Type.IP);
        if (banList.isBanned(ip)) {
            banList.pardon(ip);
            MessageUtil.sendMessage(sender, "&#70E000✔ &aSuccessfully unbanned IP &f" + ip + "&a!");
            if (sender instanceof Player) MessageUtil.playSuccess((Player) sender);
        } else {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cIP &f" + ip + " &cis not banned.");
            if (sender instanceof Player) MessageUtil.playError((Player) sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        String name = command.getName().toLowerCase();
        if ((name.equals("ban") || name.equals("ban-ip") || name.equals("banip")) && args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(p.getName());
            }
        }
        return list;
    }
}
