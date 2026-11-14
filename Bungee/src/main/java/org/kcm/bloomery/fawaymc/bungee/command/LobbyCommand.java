package org.kcm.bloomery.fawaymc.bungee.command;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.kcm.bloomery.fawaymc.bungee.Global;

public class LobbyCommand extends Command implements TabExecutor {
   private final Global plugin;

   public LobbyCommand(Global plugin) {
      super("lobby", (String)null, new String[]{"hub"});
      this.plugin = plugin;
   }

   public void execute(CommandSender sender, String[] args) {
      if (args.length > 0 && sender.hasPermission("fawaymc.admin")) {
         ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
         if (target == null) {
            String var6 = String.valueOf(ChatColor.RED);
            sender.sendMessage(var6 + "✗ Player not found: " + args[0]);
         } else if (this.plugin.getLobbyManager().isInLobby(target)) {
            String var5 = String.valueOf(ChatColor.YELLOW);
            sender.sendMessage(var5 + "⚠ " + target.getName() + " is already in the lobby.");
         } else {
            if (this.plugin.getLobbyManager().sendToLobby(target)) {
               String var10001 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var10001 + "✓ Sent " + target.getName() + " to lobby.");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "✗ Failed. Is the lobby server online?");
            }

         }
      } else if (!(sender instanceof ProxiedPlayer)) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "Only in-game players can use this command.");
      } else {
         ProxiedPlayer player = (ProxiedPlayer)sender;
         if (this.plugin.getLobbyManager().isInLobby(player)) {
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "⚠ You are already in the lobby!");
         } else if (!this.plugin.getLobbyManager().sendToLobby(player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "✗ Could not connect to lobby. Server offline?");
         } else {
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Sending you to the lobby...");
         }
      }
   }

   public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
      if (args.length == 1 && sender.hasPermission("fawaymc.admin")) {
         List<String> players = new ArrayList();

         for(ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
               players.add(p.getName());
            }
         }

         return players;
      } else {
         return List.of();
      }
   }
}
