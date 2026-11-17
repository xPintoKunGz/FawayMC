package com.mery.npc.util;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mery.npc.MeryNPC;
import com.mery.npc.model.CommandType;
import com.mery.npc.model.NPCCommand;
import com.mery.npc.model.NPCData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtil implements PluginMessageListener {

    private static Method setPlaceholdersMethod;
    private static Method setBracketPlaceholdersMethod;
    private static boolean reflectionInitialized = false;

    private static final Map<String, Integer> bungeeCounts = new ConcurrentHashMap<>();
    private static final Pattern BUNGEE_PLACEHOLDER_PATTERN = Pattern.compile("(?i)%(?:bungee|online)_([a-zA-Z0-9_-]+)%");
    private static final Pattern WORLD_PLACEHOLDER_PATTERN = Pattern.compile("(?i)%(?:world_players|players_world)_([a-zA-Z0-9_-]+)%");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            setPlaceholdersMethod = papiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            setBracketPlaceholdersMethod = papiClass.getMethod("setBracketPlaceholders", OfflinePlayer.class, String.class);
        } catch (Throwable ignored) {
            setPlaceholdersMethod = null;
            setBracketPlaceholdersMethod = null;
        }
    }

    public static boolean isPlaceholderAPIEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord") && !channel.equals("bungeecord:main")) return;
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if ("PlayerCount".equals(subchannel)) {
                String server = in.readUTF();
                int count = in.readInt();
                bungeeCounts.put(server.toLowerCase(), count);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Requests real-time player counts from BungeeCord/Velocity for "ALL" and any server referenced in NPCs.
     */
    public static void pollBungeeCounts(MeryNPC plugin) {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        Player carrier = Bukkit.getOnlinePlayers().iterator().next();
        if (carrier == null || !carrier.isOnline()) return;

        Set<String> serversToPoll = new HashSet<>();
        serversToPoll.add("ALL");

        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            collectServersFromText(data.getName(), serversToPoll);
            if (data.getHologramLines() != null) {
                for (String line : data.getHologramLines()) {
                    collectServersFromText(line, serversToPoll);
                }
            }
            if (data.getCommands() != null) {
                for (NPCCommand cmd : data.getCommands()) {
                    if (cmd.getType() == CommandType.SERVER && cmd.getCommand() != null) {
                        String[] parts = cmd.getCommand().trim().split("\\s+");
                        if (parts.length > 0) {
                            serversToPoll.add(parts[parts.length - 1]);
                        }
                    }
                }
            }
        }

        for (String srv : serversToPoll) {
            try {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("PlayerCount");
                out.writeUTF(srv);
                carrier.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            } catch (Throwable ignored) {}
        }
    }

    private static void collectServersFromText(String text, Set<String> out) {
        if (text == null || text.isEmpty()) return;
        Matcher m = BUNGEE_PLACEHOLDER_PATTERN.matcher(text);
        while (m.find()) {
            String srv = m.group(1);
            if ("total".equalsIgnoreCase(srv) || "all".equalsIgnoreCase(srv)) {
                out.add("ALL");
            } else {
                out.add(srv);
            }
        }
    }

    /**
     * Finds the nearest player to the NPC (for player-specific PAPI placeholders in holograms),
     * or falls back to any online player so server/bungee/player expansions always have a valid context.
     */
    public static Player findBestContextPlayer(NPCData data) {
        if (data != null) {
            Location loc = data.getLocation();
            if (loc != null && loc.getWorld() != null) {
                World world = loc.getWorld();
                Player nearest = null;
                double minDistSq = Double.MAX_VALUE;
                for (Player p : world.getPlayers()) {
                    double d = p.getLocation().distanceSquared(loc);
                    if (d < minDistSq) {
                        minDistSq = d;
                        nearest = p;
                    }
                }
                if (nearest != null) return nearest;
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            return p;
        }
        return null;
    }

    /**
     * Parses MeryNPC built-in real-time placeholders, PlaceholderAPI (%...% and {...}), and color codes.
     */
    public static String format(String text, Player contextPlayer, NPCData data) {
        if (text == null || text.isEmpty()) return "";

        Player targetPlayer = contextPlayer != null ? contextPlayer : findBestContextPlayer(data);
        String result = text;

        int onlineCount = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();

        // 1. Built-in Real-Time Server & NPC Placeholders
        result = result
                .replace("%online%", String.valueOf(onlineCount))
                .replace("%server_online%", String.valueOf(onlineCount))
                .replace("%max_players%", String.valueOf(maxPlayers))
                .replace("%server_max_players%", String.valueOf(maxPlayers))
                .replace("%server_time%", LocalTime.now().format(TIME_FORMAT));

        if (data != null) {
            World npcWorld = data.getLocation() != null ? data.getLocation().getWorld() : null;
            int npcWorldPlayers = npcWorld != null ? npcWorld.getPlayers().size() : 0;
            result = result
                    .replace("%npc_id%", String.valueOf(data.getId()))
                    .replace("%npc_skin%", data.getSkinName() != null ? data.getSkinName() : "Steve")
                    .replace("%npc_world%", data.getWorldName() != null ? data.getWorldName() : "world")
                    .replace("%npc_world_online%", String.valueOf(npcWorldPlayers));
        }

        if (targetPlayer != null) {
            result = result
                    .replace("%player%", targetPlayer.getName())
                    .replace("%player_name%", targetPlayer.getName())
                    .replace("%name%", targetPlayer.getName())
                    .replace("<player>", targetPlayer.getName())
                    .replace("%uuid%", targetPlayer.getUniqueId().toString())
                    .replace("%world%", targetPlayer.getWorld().getName())
                    .replace("%player_world%", targetPlayer.getWorld().getName())
                    .replace("%player_ping%", String.valueOf(targetPlayer.getPing()))
                    .replace("%ping%", String.valueOf(targetPlayer.getPing()))
                    .replace("%player_level%", String.valueOf(targetPlayer.getLevel()))
                    .replace("%player_health%", String.valueOf((int) Math.ceil(targetPlayer.getHealth())))
                    .replace("%x%", String.valueOf(targetPlayer.getLocation().getBlockX()))
                    .replace("%y%", String.valueOf(targetPlayer.getLocation().getBlockY()))
                    .replace("%z%", String.valueOf(targetPlayer.getLocation().getBlockZ()));

            if (MeryNPC.getInstance() != null && MeryNPC.getInstance().getEconomy() != null) {
                try {
                    double bal = MeryNPC.getInstance().getEconomy().getBalance(targetPlayer);
                    result = result
                            .replace("%vault_eco_balance%", String.valueOf(bal))
                            .replace("%vault_eco_balance_formatted%", MONEY_FORMAT.format(bal))
                            .replace("%balance%", MONEY_FORMAT.format(bal));
                } catch (Throwable ignored) {}
            }
        }

        // 2. PlaceholderAPI (%...% and {...})
        if (isPlaceholderAPIEnabled()) {
            initReflection();
            if (setPlaceholdersMethod == null) {
                reflectionInitialized = false;
                initReflection();
            }
            if (setPlaceholdersMethod != null) {
                try {
                    result = (String) setPlaceholdersMethod.invoke(null, targetPlayer, result);
                } catch (Throwable ignored) {}
            }
            if (setBracketPlaceholdersMethod != null) {
                try {
                    result = (String) setBracketPlaceholdersMethod.invoke(null, targetPlayer, result);
                } catch (Throwable ignored) {}
            }
        }

        // 3. Built-in Real-Time BungeeCord & World Player Count Fallbacks
        // (Resolves %bungee_<server>%, %bungee_total%, %online_<server>%, %world_players_<world>% even if PAPI expansion is not installed)
        if (result.contains("%")) {
            Matcher bm = BUNGEE_PLACEHOLDER_PATTERN.matcher(result);
            if (bm.find()) {
                StringBuffer sb = new StringBuffer();
                bm.reset();
                while (bm.find()) {
                    String srv = bm.group(1).toLowerCase();
                    if ("total".equals(srv) || "all".equals(srv)) {
                        srv = "all";
                    }
                    Integer count = bungeeCounts.get(srv);
                    String replacement = count != null ? String.valueOf(count) : String.valueOf(onlineCount);
                    bm.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                }
                bm.appendTail(sb);
                result = sb.toString();
            }

            Matcher wm = WORLD_PLACEHOLDER_PATTERN.matcher(result);
            if (wm.find()) {
                StringBuffer sb = new StringBuffer();
                wm.reset();
                while (wm.find()) {
                    String wName = wm.group(1);
                    World w = Bukkit.getWorld(wName);
                    int wCount = w != null ? w.getPlayers().size() : 0;
                    wm.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(wCount)));
                }
                wm.appendTail(sb);
                result = sb.toString();
            }
        }

        // 4. Colorize (& codes and &#HEX / <#HEX> codes, including those returned by PAPI expansions!)
        return ColorUtil.colorize(result);
    }
}
