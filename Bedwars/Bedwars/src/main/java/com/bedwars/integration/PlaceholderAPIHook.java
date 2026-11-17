package com.bedwars.integration;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import com.bedwars.arena.TeamColor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final BedwarsPlugin plugin;

    public PlaceholderAPIHook(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "bedwars";
    }

    @Override
    public String getAuthor() {
        return "Antigravity";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        Arena arena = plugin.getArenaManager().getArena(player);

        if (params.equalsIgnoreCase("arena")) {
            return arena != null ? arena.getDisplayName() : "Lobby";
        }
        if (params.equalsIgnoreCase("arena_id")) {
            return arena != null ? arena.getName() : "none";
        }
        if (params.equalsIgnoreCase("team")) {
            if (arena == null) return "None";
            Team t = arena.getPlayerTeam(player.getUniqueId());
            return t != null ? t.getColor().getPrefix() : "None";
        }
        if (params.equalsIgnoreCase("team_color")) {
            if (arena == null) return "";
            Team t = arena.getPlayerTeam(player.getUniqueId());
            return t != null ? t.getColor().getChatColor().toString() : "";
        }
        if (params.equalsIgnoreCase("status") || params.equalsIgnoreCase("state")) {
            return arena != null ? arena.getState().getDisplayStatus() : "Lobby";
        }
        if (params.equalsIgnoreCase("players")) {
            return arena != null ? String.valueOf(arena.getPlayers().size()) : "0";
        }
        if (params.equalsIgnoreCase("max_players")) {
            return arena != null ? String.valueOf(arena.getMaxPlayers()) : "0";
        }
        if (params.equalsIgnoreCase("kills")) {
            return arena != null ? String.valueOf(arena.getKills(player.getUniqueId())) : "0";
        }
        if (params.equalsIgnoreCase("final_kills")) {
            return arena != null ? String.valueOf(arena.getFinalKills(player.getUniqueId())) : "0";
        }
        if (params.equalsIgnoreCase("beds")) {
            return arena != null ? String.valueOf(arena.getBedsBroken(player.getUniqueId())) : "0";
        }
        if (params.equalsIgnoreCase("time")) {
            if (arena == null) return "00:00";
            int sec = arena.getGameSeconds();
            return String.format("%02d:%02d", sec / 60, sec % 60);
        }
        if (params.startsWith("bed_status_")) {
            if (arena == null) return "✖";
            String colorStr = params.substring("bed_status_".length());
            try {
                TeamColor tc = TeamColor.valueOf(colorStr.toUpperCase());
                Team t = arena.getTeam(tc);
                if (t == null) return "✖";
                if (t.isBedAlive()) return "§a✔";
                return t.getAlivePlayerCount() > 0 ? "§e" + t.getAlivePlayerCount() : "§c✖";
            } catch (IllegalArgumentException e) {
                return "✖";
            }
        }
        return null;
    }
}

