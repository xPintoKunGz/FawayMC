package org.kcm.bloomery.fawaymc.bungee.listener;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectEvent.Reason;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.kcm.bloomery.fawaymc.bungee.Global;

public class JoinListener implements Listener {
   private final Global plugin;

   public JoinListener(Global plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onServerConnect(ServerConnectEvent event) {
      if (event.getReason() == Reason.JOIN_PROXY) {
         if (this.plugin.getLobbyManager().isAutoJoin()) {
            ServerInfo lobby = this.plugin.getLobbyManager().getLobbyServer();
            if (lobby == null) {
               this.plugin.getLogger().warning("[Lobby] Lobby server '" + this.plugin.getLobbyManager().getLobbyServerName() + "' is not online!");
            } else {
               event.setTarget(lobby);
            }
         }
      }
   }
}
