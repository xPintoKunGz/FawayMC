package com.bedwars.scoreboard;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import com.bedwars.arena.Team;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ScoreboardManager {

    private final BedwarsPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerScoreboard(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Update once every second
    }

    public void updatePlayerScoreboard(Player player) {
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            removeScoreboard(player);
            return;
        }

        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), k -> {
            org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
            Scoreboard newBoard = sm.getNewScoreboard();
            player.setScoreboard(newBoard);
            return newBoard;
        });

        Objective obj = board.getObjective("bedwars");
        String title = plugin.getConfig().getString("scoreboard.title", "§6§l✦ §e§lBEDWARS §6§l✦");
        if (obj == null) {
            obj = board.registerNewObjective("bedwars", Criteria.DUMMY, MessageUtil.color(title));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.setDisplayName(MessageUtil.color(title));
        }

        List<String> lines = new ArrayList<>();
        String footer = plugin.getConfig().getString("scoreboard.server-footer", "§eplay.yourserver.net");

        if (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING) {
            lines.add("§8§m-------------------");
            lines.add("§6» §fMap: §e" + arena.getDisplayName());
            lines.add("§6» §fPlayers: §a" + arena.getPlayers().size() + "§7/§a" + arena.getMaxPlayers());
            lines.add("§6» §fMode: §b" + arena.getMode());
            lines.add(" ");
            if (arena.getState() == ArenaState.STARTING) {
                lines.add("§fStatus: §eStarting in §a" + arena.getCountdown() + "s");
            } else {
                lines.add("§fStatus: §7Waiting for players...");
            }
            lines.add("  ");
            lines.add("§8§m-------------------");
            lines.add(footer);
        } else {
            // IN_GAME - Distinct, Modern RPG / Cyberpunk style (completely different from Hypixel)
            int sec = arena.getGameSeconds();
            boolean compact = arena.getTeams().size() > 4;

            if (!compact) {
                lines.add("§8§m-------------------");
            }
            lines.add("§6» §fTime: §e" + formatTime(sec) + " §8| §b" + getNextEventLine(sec));
            if (!compact) {
                lines.add(" ");
            }

            Team playerTeam = arena.getPlayerTeam(player.getUniqueId());
            for (Team t : arena.getTeams().values()) {
                StringBuilder sb = new StringBuilder();
                sb.append(t.getColor().getChatColor()).append("◆ §l").append(t.getColor().name()).append(" §8| ");
                int alive = t.getAlivePlayerCount(arena);
                if (t.getPlayers().isEmpty()) {
                    sb.append("§8✖ Empty");
                } else if (t.isBedAlive()) {
                    sb.append("§a✔ Bed");
                } else if (alive > 0) {
                    sb.append("§e⚔ ").append(alive).append(" Left");
                } else {
                    sb.append("§c✖ Dead");
                }

                if (playerTeam == t) {
                    sb.append(" §7★");
                }
                lines.add(sb.toString());
            }

            lines.add("  ");
            int kills = arena.getKills(player.getUniqueId());
            int finals = arena.getFinalKills(player.getUniqueId());
            int beds = arena.getBedsBroken(player.getUniqueId());
            lines.add("§7▪ §fKills: §a" + kills + " §7(§c" + finals + "F§7)");
            lines.add("§7▪ §fBeds Smashed: §e" + beds);
            lines.add("§8§m-------------------");
            lines.add(footer);
        }

        // Synchronize Teams on scoreboard for colored nametags and tab list
        for (Team t : arena.getTeams().values()) {
            String teamId = "bw_" + t.getColor().name().toLowerCase();
            org.bukkit.scoreboard.Team sbTeam = board.getTeam(teamId);
            if (sbTeam == null) {
                sbTeam = board.registerNewTeam(teamId);
            }
            sbTeam.setColor(t.getColor().getChatColor());
            sbTeam.setPrefix(t.getColor().getPrefix() + " ");
            for (UUID u : t.getPlayers()) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    if (!sbTeam.hasEntry(p.getName())) {
                        sbTeam.addEntry(p.getName());
                    }
                    p.setPlayerListName(t.getColor().getChatColor() + p.getName());
                } else if (arena.getActiveBots().containsKey(u)) {
                    String bName = arena.getActiveBots().get(u).getPlayerName();
                    if (!sbTeam.hasEntry(bName)) {
                        sbTeam.addEntry(bName);
                    }
                }
            }
        }

        renderLines(player, board, obj, lines);
    }

    private String getNextEventLine(int sec) {
        int d2 = plugin.getConfig().getInt("game.diamond-2-time", 120);
        int e2 = plugin.getConfig().getInt("game.emerald-2-time", 240);
        int d3 = plugin.getConfig().getInt("game.diamond-3-time", 360);
        int e3 = plugin.getConfig().getInt("game.emerald-3-time", 480);
        int bedDestroy = plugin.getConfig().getInt("game.bed-destroy-time", 600);
        int gameDuration = plugin.getConfig().getInt("game.game-duration", 1200);

        if (sec < d2) {
            return "Diamond II in §e" + formatTime(d2 - sec);
        } else if (sec < e2) {
            return "Emerald II in §e" + formatTime(e2 - sec);
        } else if (sec < d3) {
            return "Diamond III in §e" + formatTime(d3 - sec);
        } else if (sec < e3) {
            return "Emerald III in §e" + formatTime(e3 - sec);
        } else if (sec < bedDestroy) {
            return "Bed Destruction in §e" + formatTime(bedDestroy - sec);
        } else if (sec < gameDuration) {
            return "Game Ends in §e" + formatTime(gameDuration - sec);
        } else {
            return "§4Sudden Death!";
        }
    }

    private String formatTime(int sec) {
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    private void renderLines(Player player, Scoreboard board, Objective obj, List<String> lines) {
        // Clear old entries
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        int score = lines.size();
        Set<String> used = new HashSet<>();
        for (String line : lines) {
            String colored = MessageUtil.parse(player, line);
            // Ensure unique lines
            while (used.contains(colored)) {
                colored = colored + " ";
            }
            used.add(colored);
            obj.getScore(colored).setScore(score--);
        }
    }

    public void removeScoreboard(Player player) {
        Scoreboard board = boards.remove(player.getUniqueId());
        if (board != null) {
            org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
            if (sm != null) {
                player.setScoreboard(sm.getMainScoreboard());
            }
        }
        player.setPlayerListName(player.getName());
    }
}

