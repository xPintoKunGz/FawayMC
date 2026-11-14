package org.kcm.bloomery.fawaymc.bungee.command;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.kcm.bloomery.fawaymc.bungee.Global;

public class SetLobbyCommand extends Command implements TabExecutor {
   private final Global plugin;

   public SetLobbyCommand(Global plugin) {
      super("setlobby", (String)null, new String[]{"sethub"});
      this.plugin = plugin;
   }

   public void execute(CommandSender sender, String[] args) {
      if (args.length < 1) {
         sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Lobby Setup ===");
         sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /setlobby <server_name>");
         String var6 = String.valueOf(ChatColor.GRAY);
         sender.sendMessage(var6 + "Current: " + String.valueOf(ChatColor.WHITE) + this.plugin.getLobbyManager().getLobbyServerName());
      } else {
         String serverName = args[0];
         ServerInfo server = ProxyServer.getInstance().getServerInfo(serverName);
         if (server == null) {
            String var5 = String.valueOf(ChatColor.RED);
            sender.sendMessage(var5 + "✗ Server not found: " + serverName);
         } else {
            this.plugin.getLobbyManager().setLobbyServer(serverName);
            String var10001 = String.valueOf(ChatColor.GREEN);
            sender.sendMessage(var10001 + "✓ Lobby server set to: " + String.valueOf(ChatColor.YELLOW) + serverName);
         }
      }
   }

   public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
      if (args.length == 1) {
         List<String> servers = new ArrayList();

         for(String s : ProxyServer.getInstance().getServers().keySet()) {
            if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
               servers.add(s);
            }
         }

         return servers;
      } else {
         return List.of();
      }
   }
}
