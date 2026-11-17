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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NickCommand implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public NickCommand(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/nick [player] <nickname|off>");
            return true;
        }

        Player target;
        String rawNick;

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            rawNick = args[0];
            if (!target.hasPermission("bloomeryx.command.nick")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
        } else {
            if (!sender.hasPermission("bloomeryx.command.nick.others")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
            rawNick = args[1];
        }

        // Reset nickname
        if (rawNick.equalsIgnoreCase("off") || rawNick.equalsIgnoreCase("reset") || rawNick.equalsIgnoreCase("none")) {
            plugin.getPlayerDataManager().setNickname(target.getUniqueId(), null);
            plugin.getLuckPermsHook().updatePlayerNames(target);

            if (target.equals(sender)) {
                MessageUtil.sendConfigMessage(target, "nick-reset");
            } else {
                MessageUtil.sendConfigMessage(target, "nick-reset");
                MessageUtil.sendConfigMessage(sender, "nick-reset-other", "{target}", target.getName());
            }
            MessageUtil.playSuccess(target);
            return true;
        }

        // Validate stripped length
        String stripped = ColorUtil.stripColor(rawNick);
        int min = plugin.getConfig().getInt("nickname.min-length", 3);
        int max = plugin.getConfig().getInt("nickname.max-length", 16);

        if (stripped.length() < min || stripped.length() > max) {
            MessageUtil.sendConfigMessage(sender, "nick-invalid", "{min}", String.valueOf(min), "{max}", String.valueOf(max));
            MessageUtil.playError(target);
            return true;
        }

        // Format nickname
        String nickPrefix = plugin.getConfig().getString("nickname.prefix", "");
        String formattedNick = nickPrefix + rawNick;

        // Apply colors if permitted
        if (sender.hasPermission("bloomeryx.command.nick.colors")) {
            formattedNick = ColorUtil.colorize(formattedNick);
        } else {
            formattedNick = ColorUtil.stripColor(formattedNick);
        }

        plugin.getPlayerDataManager().setNickname(target.getUniqueId(), formattedNick);
        plugin.getLuckPermsHook().updatePlayerNames(target);

        if (target.equals(sender)) {
            MessageUtil.sendConfigMessage(target, "nick-set", "{nick}", formattedNick);
        } else {
            MessageUtil.sendConfigMessage(target, "nick-set", "{nick}", formattedNick);
            MessageUtil.sendConfigMessage(sender, "nick-set-other", "{target}", target.getName(), "{nick}", formattedNick);
        }
        MessageUtil.playSuccess(target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("off");
            if (sender.hasPermission("bloomeryx.command.nick.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(p.getName());
                }
            }
        } else if (args.length == 2 && sender.hasPermission("bloomeryx.command.nick.others")) {
            list.add("off");
        }
        return list;
    }
}

