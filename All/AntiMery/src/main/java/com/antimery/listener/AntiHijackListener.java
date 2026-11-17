package com.antimery.listener;

import com.antimery.AntiMery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class AntiHijackListener implements Listener {

    private final AntiMery plugin;

    public AntiHijackListener(AntiMery plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getConfigManager().isAntiHijackEnabled()) {
            return;
        }

        String incomingName = event.getName();
        UUID incomingUUID = event.getUniqueId();

        // 1. Check if a player with this name is already online (case-insensitive check)
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOnline() && online.getName().equalsIgnoreCase(incomingName)) {
                String kickMsg = plugin.getConfigManager().getAntiHijackKickMessage();
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);

                plugin.getLogger().warning("[AntiMery Anti-Hijack] สกัดกั้นการเข้าแทรกไอดี: มีผู้พยายามเข้าสู่ระบบด้วยชื่อ '"
                        + incomingName + "' ซ้ำกับผู้เล่น " + online.getName() + " (" + online.getAddress() + ") ที่กำลังออนไลน์อยู่!");
                return;
            }

            // 2. Check duplicate UUID collision
            if (online.isOnline() && online.getUniqueId().equals(incomingUUID)) {
                String kickMsg = plugin.getConfigManager().getAntiHijackKickMessage();
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!plugin.getConfigManager().isAntiHijackEnabled()) {
            return;
        }

        Player incoming = event.getPlayer();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOnline() && online != incoming && online.getName().equalsIgnoreCase(incoming.getName())) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getConfigManager().getAntiHijackKickMessage());
                return;
            }
        }
    }
}
