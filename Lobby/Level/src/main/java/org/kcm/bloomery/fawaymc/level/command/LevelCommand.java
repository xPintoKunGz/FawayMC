package org.kcm.bloomery.fawaymc.level.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.level.Global;
import org.kcm.bloomery.fawaymc.level.data.PlayerLevel;

public class LevelCommand implements CommandExecutor, TabCompleter {
   private final Global plugin;

   public LevelCommand(Global plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("level")) {
         if (sender instanceof Player) {
            Player p = (Player)sender;
            this.showLevel(p, p);
            return true;
         } else {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Only in-game.");
            return true;
         }
      } else if (!sender.hasPermission("fawaylevel.admin")) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lvladmin <addexp|setlevel|setexp|get|reload>");
         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "reload":
               this.plugin.reloadConfig();
               sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Config reloaded.");
               return true;
            case "get":
               if (args.length < 2) {
                  this.usage(sender, "get <player>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     this.notFound(sender);
                     return true;
                  }

                  this.showLevel(t, sender);
                  return true;
               }
            case "addexp":
               if (args.length < 3) {
                  this.usage(sender, "addexp <player> <amount>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     this.notFound(sender);
                     return true;
                  }

                  try {
                     long amt = Long.parseLong(args[2]);
                     int gained = this.plugin.getLevelManager().addExp(t, amt);
                     String var22 = String.valueOf(ChatColor.GREEN);
                     sender.sendMessage(var22 + "Added " + amt + " EXP to " + t.getName() + " (+" + gained + " levels)");
                  } catch (NumberFormatException var14) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid amount.");
                  }

                  return true;
               }
            case "setlevel":
               if (args.length < 3) {
                  this.usage(sender, "setlevel <player> <level>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     this.notFound(sender);
                     return true;
                  }

                  try {
                     int lvl = Integer.parseInt(args[2]);
                     this.plugin.getLevelManager().setLevel(t, lvl);
                     String var21 = String.valueOf(ChatColor.GREEN);
                     sender.sendMessage(var21 + "Set " + t.getName() + "'s level to " + lvl);
                  } catch (NumberFormatException var13) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid level.");
                  }

                  return true;
               }
            case "setexp":
               if (args.length < 3) {
                  this.usage(sender, "setexp <player> <exp>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     this.notFound(sender);
                     return true;
                  }

                  try {
                     long exp = Long.parseLong(args[2]);
                     this.plugin.getLevelManager().setExp(t, exp);
                     String var10001 = String.valueOf(ChatColor.GREEN);
                     sender.sendMessage(var10001 + "Set " + t.getName() + "'s exp to " + exp);
                  } catch (NumberFormatException var12) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid exp.");
                  }

                  return true;
               }
            default:
               sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lvladmin <addexp|setlevel|setexp|get|reload>");
               return true;
         }
      }
   }

   private void showLevel(Player target, CommandSender viewer) {
      PlayerLevel pl = this.plugin.getDataManager().get(target.getUniqueId());
      long required = this.plugin.getLevelManager().getRequiredExp(pl.getLevel());
      viewer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l✦ &e" + target.getName() + "'s Level &6&l✦"));
      viewer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Level: &e" + pl.getLevel()));
      long var10002 = pl.getExp();
      viewer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7EXP: &e" + var10002 + "&7/&f" + required));
      viewer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Total EXP: &e" + pl.getTotalExp()));
   }

   private void usage(CommandSender s, String usage) {
      String var10001 = String.valueOf(ChatColor.YELLOW);
      s.sendMessage(var10001 + "Usage: /lvladmin " + usage);
   }

   private void notFound(CommandSender s) {
      s.sendMessage(String.valueOf(ChatColor.RED) + "Player not found.");
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (!command.getName().equalsIgnoreCase("leveladmin")) {
         return out;
      } else if (!sender.hasPermission("fawaylevel.admin")) {
         return out;
      } else {
         if (args.length == 1) {
            for(String s : Arrays.asList("addexp", "setlevel", "setexp", "get", "reload")) {
               if (s.startsWith(args[0].toLowerCase())) {
                  out.add(s);
               }
            }
         } else if (args.length == 2) {
            for(Player p : Bukkit.getOnlinePlayers()) {
               if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                  out.add(p.getName());
               }
            }
         }

         return out;
      }
   }
}
