package org.kcm.bloomery.fawaymc.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.kcm.bloomery.fawaymc.bungee.Global;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SendCommand extends Command implements TabExecutor {
    private final Global plugin;

    public SendCommand(Global plugin) {
        super("send", plugin.getConfig().getString("permissions.send", "fawaymc.admin.send"), "gsend");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /send <player> <server>");
            return;
        }

        String targetName = args[0];
        String serverName = args[1];

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer §e" + targetName + " §cis not online on the network!");
            return;
        }

        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(serverName);
        if (serverInfo == null) {
            sender.sendMessage("§cServer §e" + serverName + " §cdoes not exist!");
            return;
        }

        target.connect(serverInfo);

        sender.sendMessage("§aSuccessfully sent §e" + target.getName() + " §ato §e" + serverInfo.getName() + "§a!");
        target.sendMessage("§eYou have been sent to §a" + serverInfo.getName() + " §eby an admin.");
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        // ถ้าพิมพ์แค่ /send <กำลังหาชื่อผู้เล่น>
        if (args.length == 1) {
            return ProxyServer.getInstance().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        // ถ้าพิมพ์ /send <ชื่อผู้เล่น> <กำลังหาชื่อเซิร์ฟเวอร์>
        else if (args.length == 2) {
            return ProxyServer.getInstance().getServers().keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}