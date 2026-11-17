package com.antimery.check;

import com.antimery.AntiMery;
import com.antimery.punishment.PunishmentType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public abstract class Check {

    protected final AntiMery plugin;
    protected final CheckType type;
    protected final String configPath;

    public Check(AntiMery plugin, CheckType type, String configPath) {
        this.plugin = plugin;
        this.type = type;
        this.configPath = configPath;
    }

    public CheckType getType() {
        return type;
    }

    public String getConfigPath() {
        return configPath;
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().isCheckEnabled(configPath);
    }

    public int getMaxVL() {
        return plugin.getConfigManager().getCheckMaxVL(configPath, 5);
    }

    public PunishmentType getPunishmentType() {
        return plugin.getConfigManager().getCheckPunishment(configPath);
    }

    public boolean hasBypassPermission(Player player) {
        if (player.isOp()) return true;
        if (player.hasPermission("antimery.bypass")) return true;
        if (player.hasPermission("antimery.bypass." + configPath)) return true;
        return false;
    }

    public boolean shouldIgnorePlayer(Player player) {
        if (!isEnabled()) return true;
        if (hasBypassPermission(player)) return true;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        return false;
    }

    public boolean flag(Player player, String details) {
        int vl = plugin.getPunishmentManager().addViolation(player, type);
        int maxVl = getMaxVL();

        // Feed directly into Atlas suspicion engine
        if (plugin.getAtlasManager() != null) {
            plugin.getAtlasManager().recordFlag(player, type, 15.0, details);
        }

        plugin.getPunishmentManager().alertStaff(player, type, vl, maxVl, details);

        // เมื่อทำผิดสะสมจนเยอะแล้ว (>= maxVl) เตะหรือแบนทันที
        if (vl >= maxVl) {
            PunishmentType punishment = getPunishmentType();
            plugin.getPunishmentManager().executePunishment(player, type, punishment, type.getDisplayName() + ": " + details);
            return true;
        }
        return false;
    }
}
