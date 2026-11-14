package org.kcm.bloomery.fawaymc.bungee.listener;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.kcm.bloomery.fawaymc.bungee.Global;

public class PingListener implements Listener {
   private final Global plugin;

   public PingListener(Global plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = 64
   )
   public void onProxyPing(ProxyPingEvent event) {
      ServerPing response = event.getResponse();
      if (response != null) {
         String[] motdLines = this.plugin.getMotdManager().getMotd();
         String combinedMotd = motdLines[0] + "\n" + motdLines[1];
         response.setDescription(combinedMotd);
         if (response.getPlayers() != null) {
            response.getPlayers().setMax(this.plugin.getMotdManager().getMaxPlayers());
         }

         if (response.getVersion() != null) {
            response.getVersion().setName(this.plugin.getMotdManager().getVersionName());
         }

         event.setResponse(response);
      }
   }
}
