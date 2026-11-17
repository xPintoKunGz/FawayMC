package org.kcm.bloomery.fawaymc.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.kcm.bloomery.fawaymc.bungee.Global;

public class FindCommand extends Command {
    private final Global plugin;

    public FindCommand(Global plugin) {
        // คำสั่ง: /find <player>
        // สิทธิ์ที่ใช้: fawaymc.admin
        super("find", "fawaymc.admin", "gfind");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /find <player>");
            return;
        }

        String targetName = args[0];

        // 1. ค้นหาผู้เล่นในระบบ BungeeCord
        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer §e" + targetName + " §cis not online on the network!");
            return;
        }

        // 2. ตรวจสอบว่าผู้เล่นเชื่อมต่อกับเซิร์ฟเวอร์อยู่หรือไม่
        if (target.getServer() == null) {
            sender.sendMessage("§c" + target.getName() + " is not currently connected to any server.");
            return;
        }

        // 3. ดึงชื่อเซิร์ฟเวอร์ที่ผู้เล่นอยู่
        String serverName = target.getServer().getInfo().getName();

        sender.sendMessage("§aPlayer §e" + target.getName() + " §ais currently playing on §e" + serverName + "§a.");
    }
}