package org.kcm.bloomery.fawaymc.daily.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.daily.Global;
import org.kcm.bloomery.fawaymc.daily.data.PlayerStreak;
import org.kcm.bloomery.fawaymc.daily.manager.DailyManager;

public class DailyAdminCommand implements CommandExecutor, TabCompleter {
   private final Global plugin;
   private final DailyManager manager;

   public DailyAdminCommand(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getDailyManager();
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!sender.hasPermission("daily.admin")) {
         sender.sendMessage(this.color("&cNo permission!"));
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(this.color("&eUsage: /dailyadmin <reset|info|reload>"));
         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "reload":
               this.plugin.reloadConfig();
               sender.sendMessage(this.color("&a✓ Config reloaded!"));
               return true;
            case "info":
               if (args.length < 2) {
                  sender.sendMessage(this.color("&e/dailyadmin info <player>"));
                  return true;
               } else {
                  Player t = Bukkit.getPlayerExact(args[1]);
                  if (t == null) {
                     sender.sendMessage(this.color("&cPlayer not found!"));
                     return true;
                  }

                  PlayerStreak s = this.manager.getCache().get(t.getUniqueId());
                  sender.sendMessage(this.color("&e&l✦ " + t.getName() + " ✦"));
                  sender.sendMessage(this.color(" &8▪ &7Streak: &e" + s.getStreakDays()));
                  sender.sendMessage(this.color(" &8▪ &7Total: &f" + s.getTotalClaims()));
                  sender.sendMessage(this.color(" &8▪ &7Last claim: &f" + s.getLastClaimDate()));
                  return true;
               }
            case "reset":
               if (args.length < 2) {
                  sender.sendMessage(this.color("&e/dailyadmin reset <player>"));
                  return true;
               } else {
                  Player r = Bukkit.getPlayerExact(args[1]);
                  if (r == null) {
                     sender.sendMessage(this.color("&cPlayer not found!"));
                     return true;
                  }

                  PlayerStreak rs = new PlayerStreak(r.getUniqueId());
                  this.plugin.getDataManager().saveStreak(rs);
                  this.plugin.getCache().set(r.getUniqueId(), rs);
                  sender.sendMessage(this.color("&a✓ Reset daily streak for " + r.getName()));
                  r.sendMessage(this.color("&cYour daily streak has been reset by admin."));
                  return true;
               }
            default:
               sender.sendMessage(this.color("&eUsage: /dailyadmin <reset|info|reload>"));
               return true;
         }
      }
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (!sender.hasPermission("daily.admin")) {
         return out;
      } else {
         if (args.length == 1) {
            String in = args[0].toLowerCase();

            for(String s : new String[]{"reset", "info", "reload"}) {
               if (s.startsWith(in)) {
                  out.add(s);
               }
            }
         } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reset") || sub.equals("info")) {
               String in = args[1].toLowerCase();

               for(Player p : Bukkit.getOnlinePlayers()) {
                  if (p.getName().toLowerCase().startsWith(in)) {
                     out.add(p.getName());
                  }
               }
            }
         }

         return out;
      }
   }
}
