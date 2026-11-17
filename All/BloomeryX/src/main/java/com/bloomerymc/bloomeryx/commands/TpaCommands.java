package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.manager.TeleportManager;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TpaCommands implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public TpaCommands(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        String name = command.getName().toLowerCase();

        switch (name) {
            case "tpa":
                return handleTpa(player, args, TeleportManager.TpaType.TPA);
            case "tpahere":
                return handleTpa(player, args, TeleportManager.TpaType.TPA_HERE);
            case "tpaccept":
            case "tpyes":
                return handleTpAccept(player);
            case "tpdeny":
            case "tpno":
                return handleTpDeny(player);
            case "tpcancel":
                return handleTpCancel(player);
            default:
                return false;
        }
    }

    private boolean handleTpa(Player sender, String[] args, TeleportManager.TpaType type) {
        if (!sender.hasPermission("bloomeryx.command.tpa")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/" + (type == TeleportManager.TpaType.TPA ? "tpa" : "tpahere") + " <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
            return true;
        }

        if (target.equals(sender)) {
            MessageUtil.sendConfigMessage(sender, "tpa-self");
            MessageUtil.playError(sender);
            return true;
        }

        plugin.getTeleportManager().sendRequest(sender, target, type);
        int timeout = plugin.getConfig().getInt("teleport.tpa-timeout", 60);

        if (type == TeleportManager.TpaType.TPA) {
            MessageUtil.sendConfigMessage(sender, "tpa-sent", "{target}", target.getName(), "{timeout}", String.valueOf(timeout));
            MessageUtil.sendConfigMessage(target, "tpa-received", "{sender}", sender.getName());
        } else {
            MessageUtil.sendConfigMessage(sender, "tpahere-sent", "{target}", target.getName(), "{timeout}", String.valueOf(timeout));
            MessageUtil.sendConfigMessage(target, "tpahere-received", "{sender}", sender.getName());
        }

        MessageUtil.playSuccess(sender);
        MessageUtil.playSuccess(target);
        return true;
    }

    private boolean handleTpAccept(Player receiver) {
        TeleportManager.TpaRequest req = plugin.getTeleportManager().getRequest(receiver.getUniqueId());
        if (req == null) {
            MessageUtil.sendConfigMessage(receiver, "tpa-no-request");
            MessageUtil.playError(receiver);
            return true;
        }

        Player sender = Bukkit.getPlayer(req.getSender());
        if (sender == null || !sender.isOnline()) {
            MessageUtil.sendConfigMessage(receiver, "player-not-found", "{target}", "Sender");
            plugin.getTeleportManager().removeRequest(receiver.getUniqueId());
            return true;
        }

        plugin.getTeleportManager().removeRequest(receiver.getUniqueId());
        MessageUtil.sendConfigMessage(receiver, "tpa-accepted-receiver", "{sender}", sender.getName());
        MessageUtil.sendConfigMessage(sender, "tpa-accepted-sender", "{target}", receiver.getName());

        if (req.getType() == TeleportManager.TpaType.TPA) {
            plugin.getTeleportManager().teleportPlayer(sender, receiver.getLocation(), null);
        } else {
            plugin.getTeleportManager().teleportPlayer(receiver, sender.getLocation(), null);
        }
        return true;
    }

    private boolean handleTpDeny(Player receiver) {
        TeleportManager.TpaRequest req = plugin.getTeleportManager().getRequest(receiver.getUniqueId());
        if (req == null) {
            MessageUtil.sendConfigMessage(receiver, "tpa-no-request");
            MessageUtil.playError(receiver);
            return true;
        }

        Player sender = Bukkit.getPlayer(req.getSender());
        plugin.getTeleportManager().removeRequest(receiver.getUniqueId());

        MessageUtil.sendConfigMessage(receiver, "tpa-denied-receiver", "{sender}", sender != null ? sender.getName() : "Unknown");
        if (sender != null && sender.isOnline()) {
            MessageUtil.sendConfigMessage(sender, "tpa-denied-sender", "{target}", receiver.getName());
            MessageUtil.playError(sender);
        }
        return true;
    }

    private boolean handleTpCancel(Player sender) {
        boolean cancelled = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            TeleportManager.TpaRequest req = plugin.getTeleportManager().getRequest(online.getUniqueId());
            if (req != null && req.getSender().equals(sender.getUniqueId())) {
                plugin.getTeleportManager().removeRequest(online.getUniqueId());
                cancelled = true;
            }
        }

        if (cancelled) {
            MessageUtil.sendConfigMessage(sender, "tpa-cancelled");
            MessageUtil.playSuccess(sender);
        } else {
            MessageUtil.sendConfigMessage(sender, "tpa-no-request");
            MessageUtil.playError(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        String name = command.getName().toLowerCase();
        if ((name.equals("tpa") || name.equals("tpahere")) && args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
        }
        return list;
    }
}

