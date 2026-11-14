package org.kcm.bloomery.fawaymc.leaderboard.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.leaderboard.Global;
import org.kcm.bloomery.fawaymc.leaderboard.hologram.HologramConfig;
import org.kcm.bloomery.fawaymc.leaderboard.hologram.LeaderboardHologram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HologramCommand implements CommandExecutor, TabCompleter {

   private final Global plugin;

   public HologramCommand(Global plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!sender.hasPermission("Global.admin")) {
         sender.sendMessage(ChatColor.RED + "No permission.");
         return true;
      }

      if (args.length == 0) {
         sender.sendMessage(ChatColor.YELLOW + "Usage: /lbholo <create|delete|edit|list|tp|move|refresh>");
         return true;
      }

      String sub = args[0].toLowerCase();
      switch (sub) {
         // ★ FIX: ใส่ปีกกา { } ครอบเพื่อจำกัด Scope ของตัวแปร
         case "create": {
            if (!(sender instanceof Player p)) {
               sender.sendMessage(ChatColor.RED + "Only in-game.");
               return true;
            }

            if (args.length < 3) {
               sender.sendMessage(ChatColor.YELLOW + "Usage: /lbholo create <id> <type>");
               sender.sendMessage(ChatColor.GRAY + "Types: " + plugin.getLeaderboardManager().getRegisteredTypes());
               return true;
            }

            String id = args[1].toLowerCase();
            String type = args[2].toLowerCase();

            if (plugin.getLeaderboardManager().getProvider(type) == null) {
               sender.sendMessage(ChatColor.RED + "Unknown type: " + type);
               sender.sendMessage(ChatColor.GRAY + "Available: " + plugin.getLeaderboardManager().getRegisteredTypes());
               return true;
            }

            if (plugin.getHologramManager().get(id) != null) {
               sender.sendMessage(ChatColor.RED + "Hologram already exists: " + id);
               return true;
            }

            boolean ok = plugin.getHologramManager().create(id, type, p.getLocation());
            sender.sendMessage(ok
                    ? ChatColor.GREEN + "✓ Created hologram §e" + id + " §7(" + type + ")"
                    : ChatColor.RED + "Failed to create.");
            return true;
         }

         case "delete": {
            if (args.length < 2) {
               sender.sendMessage(ChatColor.YELLOW + "Usage: /lbholo delete <id>");
               return true;
            }

            String id = args[1].toLowerCase();
            boolean ok = plugin.getHologramManager().delete(id);
            sender.sendMessage(ok
                    ? ChatColor.GREEN + "✓ Deleted " + id
                    : ChatColor.RED + "Hologram not found: " + id);
            return true;
         }

         case "edit": {
            if (!(sender instanceof Player p)) {
               sender.sendMessage(ChatColor.RED + "Only in-game.");
               return true;
            }

            if (args.length < 2) {
               sender.sendMessage(ChatColor.YELLOW + "Usage: /lbholo edit <id>");
               return true;
            }

            String id = args[1].toLowerCase();
            LeaderboardHologram h = plugin.getHologramManager().get(id);
            if (h == null) {
               sender.sendMessage(ChatColor.RED + "Hologram not found: " + id);
               return true;
            }

            plugin.getHologramEditGui().open(p, h.getConfig());
            return true;
         }

         case "list": {
            List<String> ids = new ArrayList<>(plugin.getHologramManager().getIds());
            if (ids.isEmpty()) {
               sender.sendMessage(ChatColor.YELLOW + "No holograms yet.");
               return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== Holograms (" + ids.size() + ") ===");
            for (String id : ids) {
               LeaderboardHologram h = plugin.getHologramManager().get(id);
               if (h != null) {
                  HologramConfig c = h.getConfig();
                  String locStr = c.location != null
                          ? c.location.getWorld().getName()
                            + " @ " + c.location.getBlockX()
                            + "," + c.location.getBlockY()
                            + "," + c.location.getBlockZ()
                          : "?";
                  sender.sendMessage(ChatColor.YELLOW + " • " + ChatColor.WHITE + id
                          + ChatColor.GRAY + " [" + c.type + "] "
                          + ChatColor.DARK_GRAY + locStr);
               }
            }
            return true;
         }

         case "tp": {
            if (!(sender instanceof Player p)) {
               sender.sendMessage(ChatColor.RED + "Only in-game.");
               return true;
            }

            if (args.length < 2) {
               sender.sendMessage(ChatColor.YELLOW + "Usage: /lbholo tp <id>");
               return true;
            }

            LeaderboardHologram h = plugin.getHologramManager().get(args[1].toLowerCase());
            if (h == null || h.getConfig().location == null) {
               sender.sendMessage(ChatColor.RED + "Not found.");
               return true;
            }

            p.teleport(h.getConfig().location.clone().add(0, 1, 0));
            sender.sendMessage(ChatColor.GREEN + "✓ Teleported.");
            return true;
         }

         case "move": {
            if (!(sender instanceof Player p)) {
               sender.sendMessage(ChatColor.RED + "Only in-game.");
               return true;
            }

            if (args.length < 2) {
               sender.sendMessage(ChatColor.YELLOW + "Usage: /lbholo move <id>");
               return true;
            }

            LeaderboardHologram h = plugin.getHologramManager().get(args[1].toLowerCase());
            if (h == null) {
               sender.sendMessage(ChatColor.RED + "Not found.");
               return true;
            }

            h.getConfig().location = p.getLocation().clone();
            plugin.getHologramManager().save(h.getConfig());
            plugin.getHologramManager().render(h.getConfig().id);
            sender.sendMessage(ChatColor.GREEN + "✓ Moved to your location.");
            return true;
         }

         case "refresh": {
            plugin.getHologramManager().renderAll();
            sender.sendMessage(ChatColor.GREEN + "✓ Re-rendered all holograms.");
            return true;
         }

         default: {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /lbholo <create|delete|edit|list|tp|move|refresh>");
            return true;
         }
      }
   }

   @Override
   public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
      List<String> out = new ArrayList<>();
      if (!s.hasPermission("Global.admin")) {
         return out;
      }

      if (args.length == 1) {
         for (String x : Arrays.asList("create", "delete", "edit", "list", "tp", "move", "refresh")) {
            if (x.startsWith(args[0].toLowerCase())) {
               out.add(x);
            }
         }
      } else if (args.length == 2) {
         if (!args[0].equalsIgnoreCase("create")) {
            for (String id : plugin.getHologramManager().getIds()) {
               if (id.startsWith(args[1].toLowerCase())) {
                  out.add(id);
               }
            }
         }
      } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
         for (String t : plugin.getLeaderboardManager().getRegisteredTypes()) {
            if (t.startsWith(args[2].toLowerCase())) {
               out.add(t);
            }
         }
      }

      return out;
   }
}