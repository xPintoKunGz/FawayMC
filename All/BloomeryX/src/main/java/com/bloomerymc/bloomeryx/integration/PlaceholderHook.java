package com.bloomerymc.bloomeryx.integration;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderHook {

    private final boolean papiAvailable;

    public PlaceholderHook() {
        boolean hasPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.papiAvailable = hasPlugin;
        if (hasPlugin) {
            BloomeryX.getInstance().getLogger().info("Successfully hooked into PlaceholderAPI!");
        } else {
            BloomeryX.getInstance().getLogger().info("PlaceholderAPI not found. Using internal placeholders.");
        }
    }

    public boolean isPapiAvailable() {
        return papiAvailable;
    }

    /**
     * Parses all placeholders (internal LuckPerms + PlaceholderAPI) and formats colors.
     */
    public String parse(Player player, String text) {
        if (text == null || text.isEmpty()) return "";

        String prefix = player != null ? BloomeryX.getInstance().getLuckPermsHook().getPrefix(player) : "";
        String suffix = player != null ? BloomeryX.getInstance().getLuckPermsHook().getSuffix(player) : "";
        String group = player != null ? BloomeryX.getInstance().getLuckPermsHook().getPrimaryGroup(player) : "default";
        String displayName = player != null ? player.getDisplayName() : "";
        String name = player != null ? player.getName() : "";

        // Fallback to PAPI if direct LuckPerms returned empty
        if (prefix.isEmpty() && papiAvailable && player != null) {
            String p = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
            if (!p.equals("%luckperms_prefix%") && !p.isEmpty()) {
                prefix = p;
            } else {
                p = PlaceholderAPI.setPlaceholders(player, "%vault_prefix%");
                if (!p.equals("%vault_prefix%") && !p.isEmpty()) {
                    prefix = p;
                }
            }
        }

        if (suffix.isEmpty() && papiAvailable && player != null) {
            String s = PlaceholderAPI.setPlaceholders(player, "%luckperms_suffix%");
            if (!s.equals("%luckperms_suffix%") && !s.isEmpty()) {
                suffix = s;
            } else {
                s = PlaceholderAPI.setPlaceholders(player, "%vault_suffix%");
                if (!s.equals("%vault_suffix%") && !s.isEmpty()) {
                    suffix = s;
                }
            }
        }

        String result = text
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{group}", group)
                .replace("{displayname}", displayName)
                .replace("{name}", name);

        if (papiAvailable && player != null) {
            try {
                result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception ignored) {}
        }

        return ColorUtil.colorize(result);
    }
}

