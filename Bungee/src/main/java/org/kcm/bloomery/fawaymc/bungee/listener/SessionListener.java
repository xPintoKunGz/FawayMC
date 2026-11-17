package org.kcm.bloomery.fawaymc.bungee.listener;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import org.kcm.bloomery.fawaymc.bungee.Global;
import org.kcm.bloomery.fawaymc.bungee.manager.PlayerSessionManager;

/**
 * ง่าย ๆ:
 * - ทุกครั้งที่ผู้เล่นสลับเซิร์ฟเวอร์ (หรือเข้าเซิร์ฟเวอร์ครั้งแรก) → จำชื่อเซิร์ฟเวอร์ล่าสุด
 * - ตอน rejoin (JOIN_PROXY) → setTarget ไปเซิร์ฟเวอร์ล่าสุดที่จำไว้
 */
public class SessionListener implements Listener {

    private final Global plugin;
    private final PlayerSessionManager sessions;

    public SessionListener(Global plugin, PlayerSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    /**
     * บันทึกเซิร์ฟเวอร์ที่ผู้เล่นอยู่ล่าสุด
     * ทำงานทุกครั้งที่ ServerSwitchEvent เกิด (รวมถึง entry แรกของ session)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player == null || player.getServer() == null) return;

        String newServer = player.getServer().getInfo().getName();
        sessions.setLastServer(player.getUniqueId(), newServer);
        sessions.saveAsync();
    }

    /**
     * ตอนผู้เล่นเชื่อมต่อ proxy (rejoin) → เปลี่ยน target ไปเซิร์ฟเวอร์ล่าสุด
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event) {
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) return;

        ProxiedPlayer player = event.getPlayer();
        if (player == null) return;

        String lastServer = sessions.getLastServer(player.getUniqueId());
        if (lastServer == null || lastServer.isEmpty()) return;

        ServerInfo target = plugin.getProxy().getServerInfo(lastServer);
        if (target == null) {
            sessions.removeLastServer(player.getUniqueId());
            sessions.saveAsync();
            return;
        }

        event.setTarget(target);
        plugin.getLogger().info("[Session] " + player.getName() + " rejoin → " + lastServer);
    }
}