package org.kcm.bloomery.fawaymc.daily.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.daily.Global;
import org.kcm.bloomery.fawaymc.daily.data.PlayerStreak;
import org.kcm.bloomery.fawaymc.daily.gui.DailyMainGui;
import org.kcm.bloomery.fawaymc.daily.manager.DailyManager;

public class DailyCommand implements CommandExecutor, TabCompleter {
   private final Global plugin;
   private final DailyManager manager;

   public DailyCommand(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getDailyManager();
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage("Players only.");
         return true;
      } else if (args.length == 0) {
         (new DailyMainGui(this.plugin)).open(player);

         try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
         } catch (Exception var8) {
         }

         return true;
      } else {
         switch (args[0].toLowerCase()) {
            case "claim":
               this.claim(player);
               return true;
            case "info":
            case "status":
            default:
               this.showInfo(player);
               return true;
         }
      }
   }

   private void claim(Player player) {
      this.manager.claimAsync(player, (result) -> {
         String prefix = this.color(this.plugin.getConfig().getString("messages.prefix", ""));
         switch (result) {
            case SUCCESS:
               player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
               break;
            case ALREADY_CLAIMED:
               player.sendMessage(prefix + this.color(this.plugin.getConfig().getString("messages.already_claimed", "&cAlready claimed!")));
               player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               break;
            case NO_REWARD:
               player.sendMessage(prefix + this.color(this.plugin.getConfig().getString("messages.no_rewards", "&cNo reward configured.")));
               break;
            case ERROR:
               player.sendMessage(prefix + this.color("&cAn error occurred."));
         }

      });
   }

   private void showInfo(Player player) {
      PlayerStreak streak = this.manager.getCache().get(player.getUniqueId());
      String prefix = this.color(this.plugin.getConfig().getString("messages.prefix", ""));
      int nextDay = this.manager.getNextDay(streak);
      boolean ready = this.manager.canClaim(streak);
      player.sendMessage(prefix + this.color("&e&l✦ Daily Rewards ✦"));
      player.sendMessage(this.color(" &8▪ &7Streak: &e" + streak.getStreakDays() + " &7days"));
      player.sendMessage(this.color(" &8▪ &7Total claims: &f" + streak.getTotalClaims()));
      player.sendMessage(this.color(" &8▪ &7Next day: &fDay " + nextDay));
      player.sendMessage("");
      if (ready) {
         player.sendMessage(this.color(" &a▶ &aReady! &7(/daily claim)"));
      } else {
         player.sendMessage(this.color(" &c✘ &cAlready claimed today. Come back tomorrow!"));
      }

   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (args.length == 1) {
         String in = args[0].toLowerCase();

         for(String s : new String[]{"claim", "info"}) {
            if (s.startsWith(in)) {
               out.add(s);
            }
         }
      }

      return out;
   }
}
