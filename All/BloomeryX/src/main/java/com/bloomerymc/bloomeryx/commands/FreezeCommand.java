package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
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

public class FreezeCommand implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public FreezeCommand(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        // Direct /freezeworld or /tickfreeze
        if (cmd.equals("freezeworld") || cmd.equals("tickfreeze")) {
            if (!sender.hasPermission("bloomeryx.command.freezeworld")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            plugin.getFreezeManager().toggleWorldFreeze(sender);
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/freeze <player|world>");
            return true;
        }

        // World freeze option: /freeze world or /freeze all
        if (args[0].equalsIgnoreCase("world") || args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("server")) {
            if (!sender.hasPermission("bloomeryx.command.freezeworld")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            plugin.getFreezeManager().toggleWorldFreeze(sender);
            return true;
        }

        // Player freeze option: /freeze <player>
        if (!sender.hasPermission("bloomeryx.command.freeze")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
            return true;
        }

        if (cmd.equals("unfreeze")) {
            if (plugin.getFreezeManager().isPlayerFrozen(target.getUniqueId())) {
                plugin.getFreezeManager().unfreezePlayer(target, sender);
            } else {
                MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cPlayer &f" + target.getName() + " &cis not frozen.");
            }
            return true;
        }

        plugin.getFreezeManager().togglePlayerFreeze(target, sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("bloomeryx.command.freezeworld")) {
                if ("world".startsWith(args[0].toLowerCase())) list.add("world");
            }
            if (sender.hasPermission("bloomeryx.command.freeze")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        list.add(p.getName());
                    }
                }
            }
        }
        return list;
    }
}

