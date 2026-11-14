package org.kcm.bloomery.fawaymc.leaderboard.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.kcm.bloomery.fawaymc.leaderboard.Global;
import org.kcm.bloomery.fawaymc.leaderboard.data.LeaderboardEntry;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {
   private final Global plugin;

   public LeaderboardCommand(Global plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (args.length != 0 && !args[0].equalsIgnoreCase("list")) {
         String sub = args[0].toLowerCase();
         if (sub.equals("refresh")) {
            if (!sender.hasPermission("Global.admin")) {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
               return true;
            } else {
               sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Refreshing leaderboards...");
               this.plugin.getLeaderboardManager().refreshAll();
               sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Refreshed.");
               return true;
            }
         } else if (sub.equals("import")) {
            if (!sender.hasPermission("Global.admin")) {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
               return true;
            } else {
               sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Importing from source plugins...");
               this.plugin.getLeaderboardManager().importFromSources();
               sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Imported.");
               return true;
            }
         } else if (!sub.equals("info")) {
            if (sub.equals("save")) {
               if (!sender.hasPermission("Global.admin")) {
                  sender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
                  return true;
               } else {
                  this.plugin.getLeaderboardManager().saveCache();
                  sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Cache saved.");
                  return true;
               }
            } else {
               sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lb <list|refresh|import|info|save>");
               return true;
            }
         } else if (args.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lb info <type>");
            return true;
         } else {
            String type = args[1].toLowerCase();
            List<LeaderboardEntry> top = this.plugin.getLeaderboardManager().getTop(type);
            if (top.isEmpty()) {
               String var13 = String.valueOf(ChatColor.RED);
               sender.sendMessage(var13 + "No data for: " + type);
               return true;
            } else {
               String var12 = String.valueOf(ChatColor.GOLD);
               sender.sendMessage(var12 + "=== Top " + top.size() + " — " + type + " ===");

               for(int i = 0; i < top.size(); ++i) {
                  sender.sendMessage(this.plugin.getLeaderboardManager().formatLine(type, i + 1));
               }

               return true;
            }
         }
      } else {
         sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Leaderboard Types ===");

         for(String t : this.plugin.getLeaderboardManager().getRegisteredTypes()) {
            int size = this.plugin.getLeaderboardManager().getSize(t);
            String var10001 = String.valueOf(ChatColor.YELLOW);
            sender.sendMessage(var10001 + " • " + t + String.valueOf(ChatColor.GRAY) + " (" + size + " entries)");
         }

         return true;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (args.length == 1) {
         for(String s : Arrays.asList("list", "refresh", "import", "info", "save")) {
            if (s.startsWith(args[0].toLowerCase())) {
               out.add(s);
            }
         }
      } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
         for(String t : this.plugin.getLeaderboardManager().getRegisteredTypes()) {
            if (t.startsWith(args[1].toLowerCase())) {
               out.add(t);
            }
         }
      }

      return out;
   }
}
