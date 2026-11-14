package org.kcm.bloomery.fawaymc.advancement.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;
import org.kcm.bloomery.fawaymc.advancement.gui.AdvancementCategoryGui;

public class AdvancementCommand implements CommandExecutor, TabCompleter {
   private final Global plugin;

   public AdvancementCommand(Global plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (cmd.getName().equalsIgnoreCase("advancement")) {
         if (sender instanceof Player) {
            Player p = (Player)sender;
            (new AdvancementCategoryGui(this.plugin)).open(p);
            return true;
         } else {
            sender.sendMessage("Only in-game.");
            return true;
         }
      } else if (!sender.hasPermission("Global.admin")) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "No permission.");
         return true;
      } else if (args.length == 0) {
         sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /advadmin <give|revoke|setprogress|reset|list|reload|trigger>");
         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "reload":
               this.plugin.reloadConfig();
               this.plugin.getAdvancementManager().loadFromConfig();
               String var36 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var36 + "Reloaded! " + this.plugin.getAdvancementManager().getAdvancementCount() + " advancements.");
               return true;
            case "list":
               String var34 = String.valueOf(ChatColor.GOLD);
               sender.sendMessage(var34 + "=== Advancements (" + this.plugin.getAdvancementManager().getAdvancementCount() + ") ===");

               for(Advancement a : this.plugin.getAdvancementManager().getAll()) {
                  var34 = String.valueOf(ChatColor.YELLOW);
                  sender.sendMessage(var34 + " • " + a.getId() + String.valueOf(ChatColor.GRAY) + " [" + String.valueOf(a.getTriggerType()) + "] " + String.valueOf(ChatColor.WHITE) + "req=" + a.getRequired());
               }

               return true;
            case "give":
               if (args.length < 3) {
                  this.usage(sender, "give <player> <advId>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found.");
                     return true;
                  } else {
                     if (this.plugin.getAdvancementManager().get(args[2]) == null) {
                        String var33 = String.valueOf(ChatColor.RED);
                        sender.sendMessage(var33 + "Advancement not found: " + args[2]);
                        return true;
                     }

                     this.plugin.getAdvancementManager().unlock(t, args[2]);
                     String var32 = String.valueOf(ChatColor.GREEN);
                     sender.sendMessage(var32 + "Unlocked " + args[2] + " for " + t.getName());
                     return true;
                  }
               }
            case "revoke":
               if (args.length < 3) {
                  this.usage(sender, "revoke <player> <advId>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found.");
                     return true;
                  }

                  this.plugin.getAdvancementManager().revoke(t, args[2]);
                  String var31 = String.valueOf(ChatColor.GREEN);
                  sender.sendMessage(var31 + "Revoked " + args[2] + " from " + t.getName());
                  return true;
               }
            case "setprogress":
               if (args.length < 4) {
                  this.usage(sender, "setprogress <player> <advId> <value>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found.");
                     return true;
                  }

                  try {
                     int v = Integer.parseInt(args[3]);
                     this.plugin.getAdvancementManager().setProgress(t, args[2], v);
                     String var30 = String.valueOf(ChatColor.GREEN);
                     sender.sendMessage(var30 + "Set " + args[2] + " progress to " + v);
                  } catch (NumberFormatException var10) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid number.");
                  }

                  return true;
               }
            case "trigger":
               if (args.length < 3) {
                  this.usage(sender, "trigger <player> <advId> [amount]");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found.");
                     return true;
                  }

                  int amt = args.length >= 4 ? this.safeInt(args[3], 1) : 1;
                  this.plugin.getAdvancementManager().incrementProgress(t, args[2], amt);
                  String var29 = String.valueOf(ChatColor.GREEN);
                  sender.sendMessage(var29 + "Incremented " + args[2] + " by " + amt);
                  return true;
               }
            case "reset":
               if (args.length < 2) {
                  this.usage(sender, "reset <player>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found.");
                     return true;
                  }

                  PlayerProgress prog = this.plugin.getDataManager().get(t.getUniqueId());
                  prog.getProgress().clear();
                  prog.getUnlocked().clear();
                  String var28 = String.valueOf(ChatColor.GREEN);
                  sender.sendMessage(var28 + "Reset all advancements for " + t.getName());
                  return true;
               }
            case "cleanup":
               Set<String> validIds = new HashSet();

               for(Advancement a : this.plugin.getAdvancementManager().getAll()) {
                  validIds.add(a.getId());
               }

               int[] result = this.plugin.getDataManager().cleanupInvalid(validIds);
               String var26 = String.valueOf(ChatColor.GREEN);
               sender.sendMessage(var26 + "✓ Cleaned " + result[1] + " invalid entries from " + result[0] + " players.");
               var26 = String.valueOf(ChatColor.GRAY);
               sender.sendMessage(var26 + "Valid advancement IDs: " + validIds.size());
               return true;
            case "resetall":
               sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "⚠ Resetting ALL players...");
               this.plugin.getDataManager().resetAllPlayers();
               sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Reset ALL advancements for ALL players.");
               return true;
            case "resetplayer":
               if (args.length < 2) {
                  this.usage(sender, "resetplayer <player>");
                  return true;
               } else {
                  Player t = Bukkit.getPlayer(args[1]);
                  if (t == null) {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found.");
                     return true;
                  }

                  this.plugin.getDataManager().resetPlayer(t.getUniqueId());
                  String var10001 = String.valueOf(ChatColor.GREEN);
                  sender.sendMessage(var10001 + "✓ Reset ALL advancements for " + t.getName());
                  return true;
               }
            case "purgeplayerdata":
               this.plugin.getDataManager().resetAllPlayers();

               for(Player p : Bukkit.getOnlinePlayers()) {
                  this.plugin.getDataManager().get(p.getUniqueId());
               }

               sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Purged playerdata for ALL players.");
               return true;
            default:
               sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /advadmin <give|revoke|setprogress|reset|list|reload|trigger|cleanup|resetall|resetplayer>");
               return true;
         }
      }
   }

   private int safeInt(String s, int def) {
      try {
         return Integer.parseInt(s);
      } catch (Exception var4) {
         return def;
      }
   }

   private void usage(CommandSender s, String u) {
      String var10001 = String.valueOf(ChatColor.YELLOW);
      s.sendMessage(var10001 + "Usage: /advadmin " + u);
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (!cmd.getName().equalsIgnoreCase("advancementadmin")) {
         return out;
      } else if (!sender.hasPermission("Global.admin")) {
         return out;
      } else {
         if (args.length == 1) {
            for(String s : Arrays.asList("give", "revoke", "setprogress", "reset", "list", "reload", "trigger", "cleanup", "resetall", "resetplayer", "purgeplayerdata")) {
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
         } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("revoke") || sub.equals("setprogress") || sub.equals("trigger")) {
               for(Advancement a : this.plugin.getAdvancementManager().getAll()) {
                  if (a.getId().startsWith(args[2].toLowerCase())) {
                     out.add(a.getId());
                  }
               }
            }
         }

         return out;
      }
   }
}
