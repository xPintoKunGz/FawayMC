package org.kcm.bloomery.fawaymc.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor; // ✅ เพิ่ม import นี้
import org.kcm.bloomery.fawaymc.bungee.Global;

import java.util.Collections;
import java.util.stream.Collectors;

public class FindCommand extends Command implements TabExecutor {
    private final Global plugin;

    public FindCommand(Global plugin) {
        super("find", plugin.getConfig().getString("permissions.find", "fawaymc.admin.find"), "gfind");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /find <player>");
            return;
        }

        String targetName = args[0];

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer §e" + targetName + " §cis not online on the network!");
            return;
        }

        if (target.getServer() == null) {
            sender.sendMessage("§c" + target.getName() + " is not currently connected to any server.");
            return;
        }

        String serverName = target.getServer().getInfo().getName();

        sender.sendMessage("§aPlayer §e" + target.getName() + " §ais currently playing on §e" + serverName + "§a.");
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return ProxyServer.getInstance().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}