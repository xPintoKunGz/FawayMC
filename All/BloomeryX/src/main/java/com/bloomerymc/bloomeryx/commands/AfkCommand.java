package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AfkCommand implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public AfkCommand(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("delafk") || cmd.equals("remafk")) {
            return handleDeleteAfkPoint(sender);
        }

        if (cmd.equals("setafk")) {
            return handleSetAfkPoint(sender);
        }

        if (cmd.equals("afk")) {
            if (args.length > 0 && (args[0].equalsIgnoreCase("delpoint") || args[0].equalsIgnoreCase("deletepoint"))) {
                return handleDeleteAfkPoint(sender);
            }
            if (args.length > 0 && (args[0].equalsIgnoreCase("setpoint") || args[0].equalsIgnoreCase("set"))) {
                return handleSetAfkPoint(sender);
            }

            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("bloomeryx.command.afk")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }

            String reason = args.length > 0 ? String.join(" ", args) : null;
            plugin.getAfkManager().toggleAfk(player, reason);
            return true;
        }

        return false;
    }

    private boolean handleSetAfkPoint(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.setafk") && !player.hasPermission("bloomeryx.admin")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        plugin.getAfkManager().setAfkPoint(player.getLocation());
        MessageUtil.sendConfigMessage(player, "afk-point-set");
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean handleDeleteAfkPoint(CommandSender sender) {
        if (!sender.hasPermission("bloomeryx.command.setafk") && !sender.hasPermission("bloomeryx.admin")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        if (plugin.getAfkManager().deleteAfkPoint()) {
            MessageUtil.sendConfigMessage(sender, "afk-point-deleted");
            if (sender instanceof Player) MessageUtil.playSuccess((Player) sender);
        } else {
            MessageUtil.sendConfigMessage(sender, "afk-point-not-set");
            if (sender instanceof Player) MessageUtil.playError((Player) sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("afk") && args.length == 1) {
            if (sender.hasPermission("bloomeryx.command.setafk")) {
                for (String sub : Arrays.asList("setpoint", "delpoint")) {
                    if (sub.startsWith(args[0].toLowerCase())) list.add(sub);
                }
            }
        }
        return list;
    }
}
