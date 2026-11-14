package org.kcm.bloomery.fawaymc.bungee.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.kcm.bloomery.fawaymc.bungee.Global;

public class LobbyManager {
   private final Global plugin;
   private final File file;
   private String lobbyServerName = "lobby";
   private boolean autoJoin = true;

   public LobbyManager(Global plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "lobby.properties");
      this.load();
   }

   private void load() {
      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }

         if (!this.file.exists()) {
            this.save();
            return;
         }

         Properties props = new Properties();
         InputStream in = new FileInputStream(this.file);

         try {
            props.load(in);
         } catch (Throwable var6) {
            try {
               in.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         in.close();
         this.lobbyServerName = props.getProperty("lobby_server", "lobby");
         this.autoJoin = Boolean.parseBoolean(props.getProperty("auto_join", "true"));
      } catch (IOException e) {
         this.plugin.getLogger().warning("[Lobby] Could not load lobby.properties: " + e.getMessage());
      }

   }

   private void save() {
      try {
         Properties props = new Properties();
         props.setProperty("lobby_server", this.lobbyServerName);
         props.setProperty("auto_join", String.valueOf(this.autoJoin));
         OutputStream out = new FileOutputStream(this.file);

         try {
            props.store(out, "FawayMC Lobby Configuration");
         } catch (Throwable var6) {
            try {
               out.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         out.close();
      } catch (IOException e) {
         this.plugin.getLogger().warning("[Lobby] Could not save lobby.properties: " + e.getMessage());
      }

   }

   public void setLobbyServer(String name) {
      this.lobbyServerName = name;
      this.save();
   }

   public void setAutoJoin(boolean value) {
      this.autoJoin = value;
      this.save();
   }

   public String getLobbyServerName() {
      return this.lobbyServerName;
   }

   public boolean isAutoJoin() {
      return this.autoJoin;
   }

   public ServerInfo getLobbyServer() {
      return ProxyServer.getInstance().getServerInfo(this.lobbyServerName);
   }

   public boolean isInLobby(ProxiedPlayer player) {
      return player != null && player.getServer() != null ? player.getServer().getInfo().getName().equalsIgnoreCase(this.lobbyServerName) : false;
   }

   public boolean sendToLobby(ProxiedPlayer player) {
      if (player == null) {
         return false;
      } else if (this.isInLobby(player)) {
         return false;
      } else {
         ServerInfo lobby = this.getLobbyServer();
         if (lobby == null) {
            return false;
         } else {
            player.connect(lobby);
            return true;
         }
      }
   }
}
