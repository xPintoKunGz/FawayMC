package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GamemodeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // Direct shortcuts: /gms, /gmc, /gma, /gmsp
        if (cmdName.equals("gms")) {
            return changeMode(sender, GameMode.SURVIVAL, args.length > 0 ? args[0] : null);
        } else if (cmdName.equals("gmc")) {
            return changeMode(sender, GameMode.CREATIVE, args.length > 0 ? args[0] : null);
        } else if (cmdName.equals("gma")) {
            return changeMode(sender, GameMode.ADVENTURE, args.length > 0 ? args[0] : null);
        } else if (cmdName.equals("gmsp")) {
            return changeMode(sender, GameMode.SPECTATOR, args.length > 0 ? args[0] : null);
        }

        // Generic /gm or /gamemode
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/" + label + " <0|1|2|3|s|c|a|sp> [player]");
            return true;
        }

        GameMode mode = parseGameMode(args[0]);
        if (mode == null) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cInvalid mode: &f" + args[0] + " &c(Use 0, 1, 2, 3 or s, c, a, sp)");
            return true;
        }

        String targetName = args.length > 1 ? args[1] : null;
        return changeMode(sender, mode, targetName);
    }

    private boolean changeMode(CommandSender sender, GameMode mode, String targetName) {
        if (targetName == null) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("bloomeryx.command.gamemode")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }

            player.setGameMode(mode);
            MessageUtil.sendConfigMessage(player, "gamemode-self", "{mode}", formatModeName(mode));
            MessageUtil.playSuccess(player);
            return true;
        }

        // Changing other player's gamemode
        if (!sender.hasPermission("bloomeryx.command.gamemode.others")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", targetName);
            return true;
        }

        target.setGameMode(mode);
        MessageUtil.sendConfigMessage(target, "gamemode-by-other", "{mode}", formatModeName(mode), "{sender}", sender.getName());
        MessageUtil.sendConfigMessage(sender, "gamemode-other", "{target}", target.getName(), "{mode}", formatModeName(mode));
        if (sender instanceof Player) {
            MessageUtil.playSuccess((Player) sender);
        }
        MessageUtil.playSuccess(target);
        return true;
    }

    public static GameMode parseGameMode(String input) {
        switch (input.toLowerCase()) {
            case "0":
            case "s":
            case "survival":
                return GameMode.SURVIVAL;
            case "1":
            case "c":
            case "creative":
                return GameMode.CREATIVE;
            case "2":
            case "a":
            case "adventure":
                return GameMode.ADVENTURE;
            case "3":
            case "sp":
            case "spectator":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }

    private String formatModeName(GameMode mode) {
        switch (mode) {
            case SURVIVAL: return "Survival";
            case CREATIVE: return "Creative";
            case ADVENTURE: return "Adventure";
            case SPECTATOR: return "Spectator";
            default: return mode.name();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("gamemode") || command.getName().equalsIgnoreCase("gm")) {
            if (args.length == 1) {
                for (String m : Arrays.asList("0", "1", "2", "3", "survival", "creative", "adventure", "spectator")) {
                    if (m.startsWith(args[0].toLowerCase())) list.add(m);
                }
            } else if (args.length == 2 && sender.hasPermission("bloomeryx.command.gamemode.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) list.add(p.getName());
                }
            }
        } else {
            // gms, gmc, gma, gmsp
            if (args.length == 1 && sender.hasPermission("bloomeryx.command.gamemode.others")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(p.getName());
                }
            }
        }
        return list;
    }
}

