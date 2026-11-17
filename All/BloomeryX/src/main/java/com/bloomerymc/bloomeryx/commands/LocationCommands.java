package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocationCommands implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public LocationCommands(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        switch (name) {
            case "home":
                return handleHome(sender, args);
            case "sethome":
                return handleSetHome(sender, args);
            case "delhome":
            case "deletehome":
            case "rmhome":
                return handleDelHome(sender, args);
            case "homes":
                return handleHomesList(sender);
            case "warp":
                return handleWarp(sender, args);
            case "setwarp":
                return handleSetWarp(sender, args);
            case "delwarp":
            case "deletewarp":
            case "rmwarp":
                return handleDelWarp(sender, args);
            case "warps":
                return handleWarpsList(sender);
            case "back":
            case "return":
                return handleBack(sender);
            case "tp":
            case "teleport":
                return handleTp(sender, args);
            case "tphere":
                return handleTpHere(sender, args);
            case "tpall":
                return handleTpAll(sender);
            default:
                return false;
        }
    }

    private boolean handleHome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.home")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
        Location loc = plugin.getPlayerDataManager().getHome(player.getUniqueId(), homeName);
        if (loc == null) {
            MessageUtil.sendConfigMessage(player, "home-not-found", "{home}", homeName);
            MessageUtil.playError(player);
            return true;
        }

        plugin.getTeleportManager().teleportPlayer(player, loc, () -> {
            MessageUtil.sendConfigMessage(player, "home-teleport", "{home}", homeName);
        });
        return true;
    }

    private boolean handleSetHome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.sethome")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
        int maxHomes = getMaxHomes(player);
        Map<String, Location> currentHomes = plugin.getPlayerDataManager().getHomes(player.getUniqueId());

        if (!currentHomes.containsKey(homeName) && currentHomes.size() >= maxHomes) {
            MessageUtil.sendConfigMessage(player, "home-limit-reached", "{limit}", String.valueOf(maxHomes));
            MessageUtil.playError(player);
            return true;
        }

        plugin.getPlayerDataManager().setHome(player.getUniqueId(), homeName, player.getLocation());
        MessageUtil.sendConfigMessage(player, "home-set", "{home}", homeName);
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean handleDelHome(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.delhome")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        String homeName = args.length > 0 ? args[0].toLowerCase() : "home";
        if (plugin.getPlayerDataManager().deleteHome(player.getUniqueId(), homeName)) {
            MessageUtil.sendConfigMessage(player, "home-deleted", "{home}", homeName);
            MessageUtil.playSuccess(player);
        } else {
            MessageUtil.sendConfigMessage(player, "home-not-found", "{home}", homeName);
            MessageUtil.playError(player);
        }
        return true;
    }

    private boolean handleHomesList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.home")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        Map<String, Location> homes = plugin.getPlayerDataManager().getHomes(player.getUniqueId());
        int limit = getMaxHomes(player);
        String names = homes.isEmpty() ? "&7None" : String.join("&7, &#B2EBF2", homes.keySet());

        MessageUtil.sendConfigMessage(player, "homes-list", "{count}", String.valueOf(homes.size()), "{limit}", String.valueOf(limit), "{homes}", names);
        return true;
    }

    private int getMaxHomes(Player player) {
        if (player.hasPermission("bloomeryx.homes.unlimited") || player.isOp()) return 999;
        int max = plugin.getConfig().getInt("homes.default-max-homes", 3);
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("homes.groups");
        if (groups != null) {
            for (String group : groups.getKeys(false)) {
                if (player.hasPermission("bloomeryx.homes." + group)) {
                    max = Math.max(max, groups.getInt(group));
                }
            }
        }
        return max;
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return handleWarpsList(sender);
        }
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.warp")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        String warpName = args[0].toLowerCase();
        Location loc = plugin.getWarpManager().getWarp(warpName);
        if (loc == null) {
            MessageUtil.sendConfigMessage(player, "warp-not-found", "{warp}", warpName);
            MessageUtil.playError(player);
            return true;
        }

        plugin.getTeleportManager().teleportPlayer(player, loc, () -> {
            MessageUtil.sendConfigMessage(player, "warp-teleport", "{warp}", warpName);
        });
        return true;
    }

    private boolean handleSetWarp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.setwarp")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(player, "&#FF4B4B✕ &cUsage: &f/setwarp <name>");
            return true;
        }

        String warpName = args[0].toLowerCase();
        plugin.getWarpManager().setWarp(warpName, player.getLocation());
        MessageUtil.sendConfigMessage(player, "warp-set", "{warp}", warpName);
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean handleDelWarp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.delwarp")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/delwarp <name>");
            return true;
        }

        String warpName = args[0].toLowerCase();
        if (plugin.getWarpManager().deleteWarp(warpName)) {
            MessageUtil.sendConfigMessage(sender, "warp-deleted", "{warp}", warpName);
            if (sender instanceof Player) MessageUtil.playSuccess((Player) sender);
        } else {
            MessageUtil.sendConfigMessage(sender, "warp-not-found", "{warp}", warpName);
        }
        return true;
    }

    private boolean handleWarpsList(CommandSender sender) {
        Set<String> warps = plugin.getWarpManager().getWarpNames();
        String list = warps.isEmpty() ? "&7None" : String.join("&7, &#B2EBF2", warps);
        MessageUtil.sendConfigMessage(sender, "warps-list", "{warps}", list);
        return true;
    }

    private boolean handleBack(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.back")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        Location backLoc = plugin.getPlayerDataManager().getBackLocation(player.getUniqueId());
        if (backLoc == null) {
            MessageUtil.sendConfigMessage(player, "back-no-location");
            MessageUtil.playError(player);
            return true;
        }

        plugin.getTeleportManager().teleportPlayer(player, backLoc, () -> {
            MessageUtil.sendConfigMessage(player, "back-teleport");
        });
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.tp")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            Player player = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(player, "player-not-found", "{target}", args[0]);
                return true;
            }
            plugin.getTeleportManager().teleportPlayer(player, target.getLocation(), () -> {
                MessageUtil.sendConfigMessage(player, "tp-success", "{target}", target.getName());
            });
            return true;
        } else if (args.length >= 2) {
            Player p1 = Bukkit.getPlayer(args[0]);
            Player p2 = Bukkit.getPlayer(args[1]);
            if (p1 == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
            if (p2 == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[1]);
                return true;
            }
            p1.teleport(p2.getLocation());
            MessageUtil.sendMessage(sender, "&aTeleported &f" + p1.getName() + " &ato &f" + p2.getName() + "&a!");
            return true;
        }

        MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/tp <player> [target]");
        return true;
    }

    private boolean handleTpHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.tphere")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(player, "&#FF4B4B✕ &cUsage: &f/tphere <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendConfigMessage(player, "player-not-found", "{target}", args[0]);
            return true;
        }

        target.teleport(player.getLocation());
        MessageUtil.sendConfigMessage(player, "tphere-success", "{target}", target.getName());
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean handleTpAll(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.tpall")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.teleport(player.getLocation());
            }
        }
        MessageUtil.sendConfigMessage(player, "tpall-success");
        MessageUtil.playSuccess(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        String name = command.getName().toLowerCase();

        if (name.equals("home") || name.equals("delhome") || name.equals("deletehome")) {
            if (sender instanceof Player && args.length == 1) {
                Player player = (Player) sender;
                for (String h : plugin.getPlayerDataManager().getHomes(player.getUniqueId()).keySet()) {
                    if (h.startsWith(args[0].toLowerCase())) list.add(h);
                }
            }
        } else if (name.equals("warp") || name.equals("delwarp") || name.equals("deletewarp")) {
            if (args.length == 1) {
                for (String w : plugin.getWarpManager().getWarpNames()) {
                    if (w.startsWith(args[0].toLowerCase())) list.add(w);
                }
            }
        } else if (name.equals("tp") || name.equals("tphere")) {
            if (args.length == 1 || args.length == 2) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[args.length - 1].toLowerCase())) list.add(p.getName());
                }
            }
        }
        return list;
    }
}
