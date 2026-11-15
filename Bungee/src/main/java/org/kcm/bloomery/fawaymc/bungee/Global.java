package org.kcm.bloomery.fawaymc.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import org.kcm.bloomery.fawaymc.bungee.command.LobbyCommand;
import org.kcm.bloomery.fawaymc.bungee.command.SetLobbyCommand;
import org.kcm.bloomery.fawaymc.bungee.listener.JoinListener;
import org.kcm.bloomery.fawaymc.bungee.listener.PingListener;
import org.kcm.bloomery.fawaymc.bungee.listener.SessionListener;
import org.kcm.bloomery.fawaymc.bungee.manager.LobbyManager;
import org.kcm.bloomery.fawaymc.bungee.manager.MotdManager;
import org.kcm.bloomery.fawaymc.bungee.manager.PlayerSessionManager;

public final class Global extends Plugin {
   private static Global instance;
   private MotdManager motdManager;
   private LobbyManager lobbyManager;
   private PlayerSessionManager sessionManager;

   public void onEnable() {
      instance = this;
      this.motdManager = new MotdManager(this);
      this.lobbyManager = new LobbyManager(this);
      this.sessionManager = new PlayerSessionManager(this);

      // Listeners
      this.getProxy().getPluginManager().registerListener(this, new PingListener(this));
      this.getProxy().getPluginManager().registerListener(this, new JoinListener(this));
      this.getProxy().getPluginManager().registerListener(this, new SessionListener(this, this.sessionManager));

      // Commands
      this.getProxy().getPluginManager().registerCommand(this, new SetLobbyCommand(this));
      this.getProxy().getPluginManager().registerCommand(this, new LobbyCommand(this));

      // Auto-save sessions.yml ทุก 60 วินาที
      this.sessionManager.startAutoSave();

      this.getLogger().info("§a[FawayMC-Bungeecord] enabled!");
      this.getLogger().info("§7Lobby server: §e" + this.lobbyManager.getLobbyServerName());
   }

   public void onDisable() {
      if (this.sessionManager != null) {
         this.sessionManager.stop();
         this.sessionManager = null;
      }
      this.getLogger().info("§c[FawayMC-Bungeecord] disabled!");
   }

   public static Global getInstance() {
      return instance;
   }

   public MotdManager getMotdManager() {
      return this.motdManager;
   }

   public LobbyManager getLobbyManager() {
      return this.lobbyManager;
   }

   public PlayerSessionManager getSessionManager() {
      return this.sessionManager;
   }

   /**
    * true ถ้าเซิร์ฟเวอร์นี้ถือเป็น lobby (อิงจาก LobbyManager.getLobbyServerName())
    */
   public boolean isLobbyServer(String serverName) {
      if (serverName == null || serverName.isEmpty()) return false;
      String lobby = this.lobbyManager != null ? this.lobbyManager.getLobbyServerName() : null;
      return lobby != null && lobby.equalsIgnoreCase(serverName);
   }

   /**
    * เปิด/ปิดฟีเจอร์ stay-on-server
    * ตอนนี้ hard-code = true; ถ้าต้องการทำ config เพิ่มทีหลังสามารถแก้เมธอดนี้ได้
    */
   public boolean isStayServerEnabled() {
      return true;
   }
}