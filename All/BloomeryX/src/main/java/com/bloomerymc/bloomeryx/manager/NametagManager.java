package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager {

    private final BloomeryX plugin;
    // Cache to prevent any packet spam or blinking/flickering
    private final Map<UUID, String> cachedTabNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> cachedPrefixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> cachedSuffixes = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public NametagManager(BloomeryX plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    public void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        // Periodic sync every 3 seconds (60 ticks)
        // Uses string caching so ZERO packets are sent if nothing has changed!
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 40L, 60L);
    }

    /**
     * Updates Tablist and floating Nametag above head for all online players.
     */
    public void updateAllPlayers() {
        if (!plugin.getConfig().getBoolean("nametag.enabled", true)) return;

        Set<Scoreboard> boards = getActiveScoreboards();
        for (Player target : Bukkit.getOnlinePlayers()) {
            updateTarget(target, boards);
        }
    }

    /**
     * Updates a single target player across all scoreboards and updates their Tablist name.
     */
    public void updatePlayer(Player target) {
        if (target == null || !target.isOnline()) return;
        if (!plugin.getConfig().getBoolean("nametag.enabled", true)) return;

        Set<Scoreboard> boards = getActiveScoreboards();
        updateTarget(target, boards);
    }

    private void updateTarget(Player target, Set<Scoreboard> boards) {
        if (target == null || !target.isOnline()) return;
        UUID uuid = target.getUniqueId();

        // 1. Determine DisplayName (with nickname support)
        String nickname = plugin.getPlayerDataManager().getNickname(uuid);
        String displayName = (nickname != null && !nickname.isEmpty()) ? ColorUtil.colorize(nickname) : target.getName();
        target.setDisplayName(displayName);

        // 2. Tablist Name (Press TAB to see your own and everyone else's name)
        String tabTemplate = plugin.getConfig().getString("nametag.tab-format",
                plugin.getConfig().getString("chat.tab-format", "{prefix}&f{displayname}{suffix}"));
        String formattedTab = plugin.getPlaceholderHook().parse(target, tabTemplate);

        // Only update Tablist if text actually changed (stops all flickering)
        String oldTab = cachedTabNames.get(uuid);
        if (oldTab == null || !oldTab.equals(formattedTab)) {
            cachedTabNames.put(uuid, formattedTab);
            target.setPlayerListName(formattedTab);
        }

        // 3. Floating Nametag above head (Scoreboard Teams)
        boolean headEnabled = plugin.getConfig().getBoolean("nametag.head.enabled",
                plugin.getConfig().getBoolean("nametag.use-scoreboard", true));
        if (!headEnabled) return;

        String rawPrefix = plugin.getConfig().getString("nametag.head.prefix",
                plugin.getConfig().getString("nametag.prefix", "{prefix}"));
        String rawSuffix = plugin.getConfig().getString("nametag.head.suffix",
                plugin.getConfig().getString("nametag.suffix", "{suffix}"));

        String prefix = plugin.getPlaceholderHook().parse(target, rawPrefix);
        String suffix = plugin.getPlaceholderHook().parse(target, rawSuffix);

        // Auto space formatting for clean visual separation
        if (!prefix.isEmpty() && !prefix.endsWith(" ")) {
            prefix = prefix + " ";
        }

        if (plugin.getAfkManager().isAfk(uuid)) {
            String afkTag = ColorUtil.colorize(" &7[AFK]");
            suffix = suffix.isEmpty() ? afkTag.trim() : suffix + afkTag;
        }

        if (!suffix.isEmpty() && !suffix.startsWith(" ")) {
            suffix = " " + suffix;
        }

        String oldPrefix = cachedPrefixes.get(uuid);
        String oldSuffix = cachedSuffixes.get(uuid);

        boolean prefixChanged = oldPrefix == null || !oldPrefix.equals(prefix);
        boolean suffixChanged = oldSuffix == null || !oldSuffix.equals(suffix);

        cachedPrefixes.put(uuid, prefix);
        cachedSuffixes.put(uuid, suffix);

        String teamName = getTeamName(target);

        // Apply team to all active scoreboards so EVERY viewer sees the target's nametag
        for (Scoreboard board : boards) {
            try {
                Team team = board.getTeam(teamName);
                if (team == null) {
                    team = board.registerNewTeam(teamName);
                }

                if (!team.hasEntry(target.getName())) {
                    team.addEntry(target.getName());
                }

                if (prefixChanged || !team.getPrefix().equals(prefix)) {
                    team.setPrefix(prefix);
                }

                if (suffixChanged || !team.getSuffix().equals(suffix)) {
                    team.setSuffix(suffix);
                }
            } catch (Exception ignored) {
                // Safe ignore if another plugin temporarily interferes
            }
        }
    }

    private Set<Scoreboard> getActiveScoreboards() {
        Set<Scoreboard> boards = Collections.newSetFromMap(new IdentityHashMap<>());
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm != null && sm.getMainScoreboard() != null) {
            boards.add(sm.getMainScoreboard());
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                Scoreboard sb = p.getScoreboard();
                if (sb != null) {
                    boards.add(sb);
                }
            } catch (Exception ignored) {}
        }
        return boards;
    }

    private String getTeamName(Player player) {
        String name = player.getName();
        if (name.length() <= 13) {
            return "bx_" + name;
        }
        return "b_" + name.substring(0, 10) + Integer.toHexString(player.getUniqueId().hashCode()).substring(0, 4);
    }

    public void removePlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        cachedTabNames.remove(uuid);
        cachedPrefixes.remove(uuid);
        cachedSuffixes.remove(uuid);

        String teamName = getTeamName(player);
        for (Scoreboard board : getActiveScoreboards()) {
            try {
                Team team = board.getTeam(teamName);
                if (team != null) {
                    team.unregister();
                }
            } catch (Exception ignored) {}
        }
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        cachedTabNames.clear();
        cachedPrefixes.clear();
        cachedSuffixes.clear();
    }
}
