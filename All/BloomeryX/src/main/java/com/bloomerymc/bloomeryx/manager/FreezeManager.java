package com.bloomerymc.bloomeryx.manager;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezeManager {

    private final BloomeryX plugin;
    private final Map<UUID, Location> frozenPlayers = new ConcurrentHashMap<>();
    private boolean worldFrozen = false;
    private BukkitTask freezeReminderTask;

    public FreezeManager(BloomeryX plugin) {
        this.plugin = plugin;
        startFreezeReminder();
    }

    public boolean isPlayerFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }

    public Location getFrozenLocation(UUID uuid) {
        return frozenPlayers.get(uuid);
    }

    public void freezePlayer(Player target, CommandSender sender) {
        frozenPlayers.put(target.getUniqueId(), target.getLocation().clone());
        MessageUtil.sendMessage(sender, "&#70E000✔ &aYou froze &f" + target.getName() + " &a(Unable to move or turn camera).");
        MessageUtil.sendMessage(target, "&#FF4B4B❄ You have been frozen by &f" + sender.getName() + " &c(You cannot move or look around!)");
        target.sendTitle(ColorUtil.colorize("&#FF4B4B❄ FROZEN ❄"), ColorUtil.colorize("&7Frozen by " + sender.getName()), 10, 60, 20);
        MessageUtil.playError(target);
    }

    public void unfreezePlayer(Player target, CommandSender sender) {
        frozenPlayers.remove(target.getUniqueId());
        MessageUtil.sendMessage(sender, "&#70E000✔ &aYou unfroze &f" + target.getName() + "&a!");
        MessageUtil.sendMessage(target, "&#70E000☀ You have been unfrozen! You may now move freely.");
        target.sendTitle(ColorUtil.colorize("&#70E000☀ UNFROZEN ☀"), ColorUtil.colorize("&aYou are free to move"), 10, 40, 15);
        MessageUtil.playSuccess(target);
    }

    public void togglePlayerFreeze(Player target, CommandSender sender) {
        if (isPlayerFrozen(target.getUniqueId())) {
            unfreezePlayer(target, sender);
        } else {
            freezePlayer(target, sender);
        }
    }

    public boolean isWorldFrozen() {
        return worldFrozen;
    }

    public void setWorldFreeze(boolean freeze, CommandSender sender) {
        this.worldFrozen = freeze;

        // Try native tick freeze in Minecraft 1.20+
        try {
            if (freeze) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick freeze");
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick unfreeze");
            }
        } catch (Exception ignored) {}

        if (freeze) {
            Bukkit.broadcastMessage(ColorUtil.colorize("&#B2EBF2❄ &l[World Freeze] &#B2EBF2The world has been frozen by &f" + sender.getName() + "&8."));
        } else {
            Bukkit.broadcastMessage(ColorUtil.colorize("&#70E000☀ &l[World Freeze] &#70E000World time has resumed by &f" + sender.getName() + "!"));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (freeze) {
                p.sendTitle(ColorUtil.colorize("&#B2EBF2❄ WORLD FROZEN ❄"), ColorUtil.colorize("&7Everything in the world has paused"), 10, 60, 20);
            } else {
                p.sendTitle(ColorUtil.colorize("&#70E000☀ TIME RESUMED ☀"), ColorUtil.colorize("&aWorld ticks resumed"), 10, 40, 15);
            }
        }
    }

    public void toggleWorldFreeze(CommandSender sender) {
        setWorldFreeze(!worldFrozen, sender);
    }

    private void startFreezeReminder() {
        freezeReminderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : frozenPlayers.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendTitle(ColorUtil.colorize("&#FF4B4B❄ FROZEN ❄"), ColorUtil.colorize("&cYou are still frozen!"), 0, 40, 10);
                }
            }
        }, 40L, 40L);
    }

    public void stop() {
        if (freezeReminderTask != null) {
            freezeReminderTask.cancel();
            freezeReminderTask = null;
        }
        frozenPlayers.clear();
    }
}
