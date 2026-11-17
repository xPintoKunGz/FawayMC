package com.bloomerymc.bloomeryx.integration;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LuckPermsHook {

    private boolean available;
    private LuckPerms luckPerms;

    public LuckPermsHook() {
        boolean hasPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        if (hasPlugin) {
            try {
                this.luckPerms = LuckPermsProvider.get();
                this.available = true;
                BloomeryX.getInstance().getLogger().info("Successfully hooked into LuckPerms API!");

                // Automatically update nametags and tablist whenever LuckPerms rank/meta changes
                try {
                    this.luckPerms.getEventBus().subscribe(BloomeryX.getInstance(), net.luckperms.api.event.user.UserDataRecalculateEvent.class, e -> {
                        Player p = Bukkit.getPlayer(e.getUser().getUniqueId());
                        if (p != null && p.isOnline()) {
                            Bukkit.getScheduler().runTask(BloomeryX.getInstance(), () -> updatePlayerNames(p));
                        }
                    });
                } catch (Throwable ignored) {}
            } catch (IllegalStateException e) {
                BloomeryX.getInstance().getLogger().warning("LuckPerms is present but API is not initialized yet.");
                this.available = false;
            }
        } else {
            this.available = false;
            BloomeryX.getInstance().getLogger().info("LuckPerms not found. Using standalone name formatting.");
        }
    }

    public boolean isAvailable() {
        return available && luckPerms != null;
    }

    public String getPrefix(Player player) {
        if (!isAvailable() || player == null) return "";
        try {
            CachedMetaData meta = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            String prefix = meta.getPrefix();
            return prefix != null ? ColorUtil.colorize(prefix) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String getSuffix(Player player) {
        if (!isAvailable() || player == null) return "";
        try {
            CachedMetaData meta = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            String suffix = meta.getSuffix();
            return suffix != null ? ColorUtil.colorize(suffix) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String getPrimaryGroup(Player player) {
        if (!isAvailable() || player == null) return "default";
        try {
            CachedMetaData meta = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            String group = meta.getPrimaryGroup();
            return group != null ? group : "default";
        } catch (Exception e) {
            return "default";
        }
    }

    /**
     * Updates player's Display Name, Tablist name, and Nametag above head based on LuckPerms meta and nickname.
     */
    public void updatePlayerNames(Player player) {
        if (player == null || !player.isOnline()) return;
        if (BloomeryX.getInstance().getNametagManager() != null) {
            BloomeryX.getInstance().getNametagManager().updatePlayer(player);
        }
    }
}
