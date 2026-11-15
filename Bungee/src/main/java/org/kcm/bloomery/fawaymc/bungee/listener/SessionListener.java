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
 * - บันทึกเซิร์ฟเวอร์ล่าสุดตอนสลับเซิร์ฟเวอร์ (ServerSwitchEvent)
 * - เปลี่ยน target ตอน rejoin ให้กลับไปเซิร์ฟเวอร์เดิม (ServerConnectEvent)
 */
public class SessionListener implements Listener {

    private final Global plugin;
    private final PlayerSessionManager sessions;

    public SessionListener(Global plugin, PlayerSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    /**
     * บันทึกเซิร์ฟเวอร์ล่าสุดเมื่อสลับสำเร็จ
     * ข้าม lobby และข้ามกรณี getFrom() == null (เข้า session ครั้งแรก)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onServerSwitch(ServerSwitchEvent event) {
        if (!plugin.isStayServerEnabled()) return;

        ProxiedPlayer player = event.getPlayer();
        if (player == null || player.getServer() == null) return;

        String newServer = player.getServer().getInfo().getName();
        if (plugin.isLobbyServer(newServer)) return;

        // getFrom() == null → เพิ่งเข้า session ใหม่ ไม่ใช่การสลับจริง
        if (event.getFrom() == null) return;

        sessions.setLastServer(player.getUniqueId(), newServer);
        sessions.saveAsync();
        plugin.getLogger().info("[Session] " + player.getName() + " → " + newServer + " (saved)");
    }

    /**
     * เปลี่ยน target ตอนผู้เล่นเชื่อมต่อ proxy (initial join)
     * Reason.JOIN_PROXY = การเชื่อมต่อครั้งแรกจาก client → proxy
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event) {
        if (!plugin.isStayServerEnabled()) return;
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) return;

        ProxiedPlayer player = event.getPlayer();
        if (player == null) return;

        String lastServer = sessions.getLastServer(player.getUniqueId());
        if (lastServer == null || lastServer.isEmpty()) return;

        // ถ้า last เป็น lobby → ไป lobby ตาม default อยู่แล้ว
        if (plugin.isLobbyServer(lastServer)) return;

        ServerInfo target = plugin.getProxy().getServerInfo(lastServer);
        if (target == null) {
            plugin.getLogger().warning("[Session] เซิร์ฟเวอร์ '" + lastServer + "' ของ " + player.getName() + " ไม่มีแล้ว → ลบที่บันทึกไว้");
            sessions.removeLastServer(player.getUniqueId());
            sessions.saveAsync();
            return;
        }

        // target เดิมเป็นเซิร์ฟเวอร์นี้อยู่แล้ว ไม่ต้องทำอะไร
        ServerInfo current = event.getTarget();
        if (current != null && current.getName().equalsIgnoreCase(lastServer)) return;

        event.setTarget(target);
        plugin.getLogger().info("[Session] " + player.getName() + " rejoin → redirect ไป " + lastServer);
    }
}