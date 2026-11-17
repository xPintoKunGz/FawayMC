package com.bedwars.commands;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaManager;
import com.bedwars.arena.ArenaState;
import com.bedwars.arena.Team;
import com.bedwars.gui.ArenaAdminGUI;
import com.bedwars.gui.ArenaSelectorGUI;
import com.bedwars.gui.SetupWizardGUI;
import com.bedwars.util.LocationUtil;
import com.bedwars.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BedwarsCommand implements CommandExecutor, TabCompleter {

    private final BedwarsPlugin plugin;

    public BedwarsCommand(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        ArenaManager am = plugin.getArenaManager();

        if (args.length == 0 || args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("gui")) {
            ArenaSelectorGUI.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "shop" -> {
                com.bedwars.gui.ShopGUI.open(player, com.bedwars.gui.ShopGUI.ShopCategory.QUICK_BUY);
                return true;
            }
            case "upgrade", "upgrades" -> {
                com.bedwars.gui.UpgradesGUI.open(player);
                return true;
            }
            case "join" -> {
                if (args.length < 2) {
                    ArenaSelectorGUI.open(player);
                    return true;
                }
                Arena arena = am.getArena(args[1]);
                if (arena == null) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.arena-not-found"));
                    return true;
                }
                arena.join(player);
                return true;
            }
            case "leave" -> {
                Arena current = am.getArena(player);
                if (current == null) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.arena-not-in"));
                    return true;
                }
                current.leave(player);
                return true;
            }
            case "quick", "autojoin" -> {
                Arena best = am.findBestAvailableArena();
                if (best != null) {
                    best.join(player);
                } else {
                    MessageUtil.sendMessage(player, "§cNo available arenas to join right now!");
                }
                return true;
            }
            case "rejoin" -> {
                Arena target = null;
                for (Arena a : am.getArenas()) {
                    if (a.getState() == ArenaState.IN_GAME && a.getDisconnectedPlayers().containsKey(player.getUniqueId())) {
                        target = a;
                        break;
                    }
                }
                if (target == null) {
                    MessageUtil.sendMessage(player, "§c✖ You do not have an active disconnected game to rejoin!");
                    return true;
                }
                target.handleRejoin(player);
                return true;
            }
            case "replay", "replays" -> {
                if (args.length >= 3 && args[1].equalsIgnoreCase("delete")) {
                    String targetId = args[2];
                    boolean del = plugin.getReplayManager().deleteReplay(targetId);
                    if (del) {
                        MessageUtil.sendMessage(player, "§a✔ Deleted replay: §e" + targetId);
                    } else {
                        MessageUtil.sendMessage(player, "§c✖ Replay not found: §e" + targetId);
                    }
                    return true;
                }
                Arena currentArena = am.getArena(player);
                if (currentArena != null && currentArena.getState() == ArenaState.IN_GAME) {
                    MessageUtil.sendMessage(player, "§c✖ You cannot watch replays while playing an active match!");
                    return true;
                }
                if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
                    plugin.getReplayManager().stopPlayback(player);
                }
                com.bedwars.gui.ReplayGUI.open(player);
                return true;
            }
            case "practice", "bot", "bots" -> {
                Arena targetArena = null;
                if (args.length >= 2) {
                    targetArena = am.getArena(args[1]);
                }
                if (targetArena == null) {
                    targetArena = am.getArena(player);
                }

                if (targetArena == null) {
                    MessageUtil.sendMessage(player, "§6✦ Right-click any map to open Bot Practice Setup:");
                    com.bedwars.gui.ArenaSelectorGUI.open(player);
                    return true;
                }

                // If number of bot teams is directly specified: e.g. /bw practice highland 2
                if (args.length >= 3) {
                    try {
                        int botTeamsCount = Integer.parseInt(args[2]);
                        List<Team> allEnemyTeams = new ArrayList<>();
                        Team playerTeam = targetArena.getPlayerTeam(player.getUniqueId());
                        for (Team t : targetArena.getTeams().values()) {
                            if (!t.equals(playerTeam)) allEnemyTeams.add(t);
                        }
                        if (allEnemyTeams.isEmpty()) {
                            MessageUtil.sendMessage(player, "§c✖ This arena does not have enough teams configured!");
                            return true;
                        }
                        int take = Math.max(1, Math.min(botTeamsCount, allEnemyTeams.size()));
                        List<Team> selected = allEnemyTeams.subList(0, take);
                        targetArena.startPracticeMatch(player, selected);
                        return true;
                    } catch (NumberFormatException ignored) {}
                }

                com.bedwars.gui.PracticeGUI.open(player, targetArena);
                return true;
            }
            case "admin", "maps", "manage" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                ArenaAdminGUI.open(player);
                return true;
            }
            case "edit" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                Arena arena = (args.length >= 2) ? am.getArena(args[1]) : am.getArena(player);
                if (arena == null) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw edit <arena> - Toggles builder edit mode for that map.");
                    return true;
                }
                boolean newMode = !arena.isEditMode();
                arena.setEditMode(newMode);
                if (newMode) {
                    MessageUtil.sendMessage(player, "§a✔ Map Edit Mode for §e" + arena.getName() + " §aENABLED! You can now place and break blocks in this map.");
                } else {
                    MessageUtil.sendMessage(player, "§c✖ Map Edit Mode for §e" + arena.getName() + " §cDISABLED! Map is now protected.");
                }
                return true;
            }
            case "create", "setup" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    ArenaAdminGUI.open(player);
                    return true;
                }
                String arenaName = args[1];
                Arena arena = am.getArena(arenaName);
                if (arena == null) {
                    arena = new Arena(arenaName);
                    am.addArena(arena);
                    MessageUtil.sendMessage(player, "§aCreated arena §e" + arenaName + "§a! Opening setup menu...");
                }
                SetupWizardGUI.open(player, arena);
                return true;
            }
            case "rename" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw rename <arena> <new display name>");
                    return true;
                }
                Arena arena = am.getArena(args[1]);
                if (arena == null) {
                    MessageUtil.sendMessage(player, "§cArena not found!");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
                }
                arena.setDisplayName(sb.toString());
                am.saveArenas();
                MessageUtil.sendMessage(player, "§a✔ Changed display name of arena §e" + arena.getName() + " §ato " + MessageUtil.color(sb.toString()) + "§a!");
                return true;
            }
            case "tp" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw tp <arena>");
                    return true;
                }
                Arena arena = am.getArena(args[1]);
                if (arena != null && arena.getLobbyLocation() != null) {
                    player.teleport(arena.getLobbyLocation());
                    MessageUtil.sendMessage(player, "§aTeleported to arena §e" + arena.getName() + "§a!");
                } else {
                    MessageUtil.sendMessage(player, "§cArena not found or waiting lobby spawn is not set!");
                }
                return true;
            }
            case "delete", "remove" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw delete <arena>");
                    return true;
                }
                am.removeArena(args[1]);
                MessageUtil.sendMessage(player, "§a✔ Deleted arena §e" + args[1] + " §asuccessfully!");
                return true;
            }
            case "setspawn", "setlobby", "setmainlobby" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                Location loc = player.getLocation();
                plugin.getConfig().set("server.spawn-location", LocationUtil.serialize(loc));
                plugin.getConfig().set("server.lobby-location", LocationUtil.serialize(loc));
                plugin.saveConfig();
                plugin.setMainLobby(loc);
                MessageUtil.sendMessage(player, "§a✔ Main spawn location set to your current position!");
                MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                return true;
            }
            case "list" -> {
                MessageUtil.sendMessage(player, "§6§l✦ BEDWARS ARENAS LIST ✦");
                for (Arena a : am.getArenas()) {
                    MessageUtil.sendMessage(player, "§e● " + a.getName() + " §7(" + MessageUtil.color(a.getDisplayName()) + "§7) - " + a.getState().getDisplayStatus() + " §8[" + a.getPlayers().size() + "/" + a.getMaxPlayers() + "]");
                }
                return true;
            }
            case "forcestart" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                Arena arena = am.getArena(player);
                if (arena != null) {
                    arena.startGame();
                    MessageUtil.sendMessage(player, "§aForce started game in arena " + arena.getName() + "!");
                } else {
                    MessageUtil.sendMessage(player, "§cYou are not in a game!");
                }
                return true;
            }
            case "time" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw time <arena> <day|noon|sunset|night>");
                    return true;
                }
                Arena arena = am.getArena(args[1]);
                if (arena == null) {
                    MessageUtil.sendMessage(player, "§cArena not found!");
                    return true;
                }
                String time = args[2].toLowerCase();
                if (!List.of("day", "noon", "sunset", "night").contains(time)) {
                    MessageUtil.sendMessage(player, "§cTime must be one of: day, noon, sunset, night!");
                    return true;
                }
                arena.setTimeOfDay(time);
                am.saveArenas();
                arena.applyTimeOfDay();
                MessageUtil.sendMessage(player, "§a✔ Changed time of arena §e" + arena.getName() + " §ato §b" + time + "§a!");
                return true;
            }
            case "pos1", "setpos1" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                Arena arena = null;
                if (args.length >= 2) {
                    arena = am.getArena(args[1]);
                } else {
                    arena = am.getArena(player);
                }
                if (arena == null) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw pos1 <arena>");
                    return true;
                }
                arena.setPos1(player.getLocation().getBlock().getLocation());
                am.saveArenas();
                MessageUtil.playSound(player, org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                MessageUtil.sendMessage(player, "§a✔ Saved Map Boundary Pos 1 at: §e" + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ() + " §afor arena §e" + arena.getName() + "§a!");
                return true;
            }
            case "pos2", "setpos2" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                Arena arena = null;
                if (args.length >= 2) {
                    arena = am.getArena(args[1]);
                } else {
                    arena = am.getArena(player);
                }
                if (arena == null) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw pos2 <arena>");
                    return true;
                }
                arena.setPos2(player.getLocation().getBlock().getLocation());
                am.saveArenas();
                MessageUtil.playSound(player, org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                MessageUtil.sendMessage(player, "§a✔ Saved Map Boundary Pos 2 at: §e" + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ() + " §afor arena §e" + arena.getName() + "§a!");
                return true;
            }
            case "suddendeath", "sd", "skip" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                Arena arena = (args.length >= 2) ? am.getArena(args[1]) : am.getArena(player);
                if (arena == null) {
                    MessageUtil.sendMessage(player, "§cUsage: /bw suddendeath <arena>");
                    return true;
                }
                if (arena.getState() != com.bedwars.arena.ArenaState.IN_GAME) {
                    MessageUtil.sendMessage(player, "§cArena §e" + arena.getName() + " §cis not in an active game!");
                    return true;
                }
                arena.triggerSuddenDeath();
                MessageUtil.sendMessage(player, "§a✔ Fast-forwarded arena §e" + arena.getName() + " §ato Sudden Death! Beds destroyed & dragons summoned with island smoke destruction!");
                return true;
            }
            case "reload" -> {
                if (!player.hasPermission("bedwars.admin")) {
                    MessageUtil.sendMessage(player, plugin.getMessage("messages.no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.loadCustomConfigs();
                am.loadArenas();
                com.bedwars.gui.ShopGUI.reloadCatalog();
                MessageUtil.sendMessage(player, "§a✔ Reloaded Bedwars configurations, shop prices, and arenas!");
                return true;
            }
            default -> {
                sendHelp(player);
                return true;
            }
        }
    }

    private void sendHelp(Player player) {
        MessageUtil.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        MessageUtil.sendMessage(player, "§6§l✦ BEDWARS NEXT-GEN COMMANDS ✦");
        MessageUtil.sendMessage(player, "§e/bw §7or §e/bw menu §8- §fOpen arena selector GUI");
        MessageUtil.sendMessage(player, "§e/bw join <arena> §8- §fJoin a specific arena");
        MessageUtil.sendMessage(player, "§e/bw leave §8- §fLeave your current game");
        MessageUtil.sendMessage(player, "§e/bw quick §8- §fQuick join an available arena");
        MessageUtil.sendMessage(player, "§e/bw rejoin §8- §fRejoin active match after disconnect");
        MessageUtil.sendMessage(player, "§e/bw replay §8- §fBrowse and watch past match replays (7 days)");
        MessageUtil.sendMessage(player, "§e/bw practice [arena] §8- §fPractice match with AI bots (Choose team count)");
        if (player.hasPermission("bedwars.admin")) {
            MessageUtil.sendMessage(player, "§6/bw maps §8- §fOpen Arena Manager GUI");
            MessageUtil.sendMessage(player, "§6/bw setup <arena> §8- §fInteractive setup wizard");
            MessageUtil.sendMessage(player, "§6/bw edit <arena> §8- §fToggle builder edit mode for a map");
            MessageUtil.sendMessage(player, "§6/bw rename <arena> <name> §8- §fRename arena display name");
            MessageUtil.sendMessage(player, "§6/bw tp <arena> §8- §fTeleport to arena lobby");
            MessageUtil.sendMessage(player, "§6/bw delete <arena> §8- §fDelete an arena");
            MessageUtil.sendMessage(player, "§6/bw setspawn §8- §fSet global server spawn point (return after game ends)");
            MessageUtil.sendMessage(player, "§6/bw pos1 <arena> §8- §fSet map boundary corner 1");
            MessageUtil.sendMessage(player, "§6/bw pos2 <arena> §8- §fSet map boundary corner 2");
            MessageUtil.sendMessage(player, "§6/bw time <arena> <time> §8- §fSet game time of day");
            MessageUtil.sendMessage(player, "§6/bw forcestart §8- §fForce start game immediately");
            MessageUtil.sendMessage(player, "§6/bw suddendeath <arena> §8- §fFast-forward to Sudden Death & summon dragons");
            MessageUtil.sendMessage(player, "§6/bw reload §8- §fReload configurations");
        }
        MessageUtil.sendMessage(player, "§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("menu", "join", "leave", "quick", "rejoin", "replay", "practice", "bot"));
            if (sender.hasPermission("bedwars.admin")) {
                subs.addAll(Arrays.asList("maps", "admin", "setup", "create", "edit", "rename", "tp", "delete", "time", "pos1", "pos2", "setspawn", "setmainlobby", "forcestart", "suddendeath", "list", "reload"));
            }
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) list.add(s);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("practice") || args[0].equalsIgnoreCase("bot") || args[0].equalsIgnoreCase("setup") || args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("rename") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2") || args[0].equalsIgnoreCase("suddendeath") || args[0].equalsIgnoreCase("sd"))) {
            for (Arena a : plugin.getArenaManager().getArenas()) {
                if (a.getName().toLowerCase().startsWith(args[1].toLowerCase())) list.add(a.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("time")) {
            for (String t : List.of("day", "noon", "sunset", "night")) {
                if (t.startsWith(args[2].toLowerCase())) list.add(t);
            }
        }
        return list;
    }
}
